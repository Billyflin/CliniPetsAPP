// ui/viewmodels/MedicalConsultationViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.MedicationApplication
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MedicalConsultationViewModel @Inject constructor() : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _consultationState = MutableStateFlow(ConsultationState())
    val consultationState: StateFlow<ConsultationState> = _consultationState

    fun loadAppointmentData(appointmentId: String) {
        viewModelScope.launch {
            try {
                val appointment = firestore.collection("appointments")
                    .document(appointmentId)
                    .get()
                    .await()
                    .toObject<VetAppointment>()

                _consultationState.value = _consultationState.value.copy(
                    appointment = appointment
                )

                // Marcar cita como en progreso
                firestore.collection("appointments")
                    .document(appointmentId)
                    .update("status", "IN_PROGRESS")
                    .await()
            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    error = "Error al cargar datos: ${e.message}"
                )
            }
        }
    }

    fun loadMedications() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("medications")
                    .whereEqualTo("isActive", true)
                    .orderBy("name")
                    .get()
                    .await()

                val medications = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<SimpleMedication>()?.copy(id = doc.id)
                }

                _consultationState.value = _consultationState.value.copy(
                    availableMedications = medications
                )
            } catch (e: Exception) {
                // Si no existe la colección, crear algunos medicamentos de ejemplo
                createSampleMedications()
            }
        }
    }

    private fun createSampleMedications() {
        viewModelScope.launch {
            val sampleMedications = listOf(
                mapOf(
                    "name" to "Amoxicilina",
                    "activeIngredient" to "Amoxicilina",
                    "presentation" to "Suspensión 250mg/5ml",
                    "type" to "ANTIBIOTIC",
                    "isActive" to true
                ),
                mapOf(
                    "name" to "Meloxicam",
                    "activeIngredient" to "Meloxicam",
                    "presentation" to "Tabletas 7.5mg",
                    "type" to "ANTI_INFLAMMATORY",
                    "isActive" to true
                ),
                mapOf(
                    "name" to "Tramadol",
                    "activeIngredient" to "Tramadol",
                    "presentation" to "Gotas 100mg/ml",
                    "type" to "ANALGESIC",
                    "isActive" to true
                ),
                mapOf(
                    "name" to "Fenbendazol",
                    "activeIngredient" to "Fenbendazol",
                    "presentation" to "Suspensión 10%",
                    "type" to "ANTIPARASITIC",
                    "isActive" to true
                ),
                mapOf(
                    "name" to "Vacuna Séxtuple",
                    "activeIngredient" to "Virus atenuados",
                    "presentation" to "Liofilizado + diluyente",
                    "type" to "VACCINE",
                    "isActive" to true
                )
            )

            try {
                sampleMedications.forEach { medication ->
                    firestore.collection("medications").add(medication).await()
                }
                loadMedications() // Recargar después de crear
            } catch (e: Exception) {
                // Error al crear medicamentos de ejemplo
            }
        }
    }

    fun addMedication(medication: SimpleMedication) {
        // Por ahora agregamos con valores por defecto
        // En una implementación completa, abrirías otro diálogo para estos detalles
        val medicationApplication = MedicationApplication(
            medicationId = medication.id,
            medicationName = medication.name,
            dose = "Según indicación",
            route = "Oral",
            frequency = "",
            duration = "",
            notes = ""
        )

        val currentMedications = _consultationState.value.appliedMedications.toMutableList()
        currentMedications.add(medicationApplication)

        _consultationState.value = _consultationState.value.copy(
            appliedMedications = currentMedications
        )
    }

    fun removeMedication(medication: MedicationApplication) {
        val currentMedications = _consultationState.value.appliedMedications.toMutableList()
        currentMedications.remove(medication)

        _consultationState.value = _consultationState.value.copy(
            appliedMedications = currentMedications
        )
    }

    fun saveConsultation(
        appointmentId: String,
        symptoms: String,
        diagnosis: String,
        treatment: String,
        observations: String,
        nextCheckup: String?
    ) {
        val vetId = auth.currentUser?.uid ?: return
        val appointment = _consultationState.value.appointment ?: return

        viewModelScope.launch {
            try {
                _consultationState.value = _consultationState.value.copy(isLoading = true)

                val consultation = hashMapOf(
                    "appointmentId" to appointmentId,
                    "pet