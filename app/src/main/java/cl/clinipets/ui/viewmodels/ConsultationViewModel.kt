// ui/viewmodels/ConsultationViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.data.model.Consultation
import cl.clinipets.data.model.MedicationUsed
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.ServiceApplied
import cl.clinipets.data.model.VaccinationRecord
import cl.clinipets.data.model.VaccineApplied
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// package cl.clinipets.ui.viewmodels

@HiltViewModel
class ConsultationViewModel @Inject constructor() : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _consultationState = MutableStateFlow(ConsultationState())
    val consultationState: StateFlow<ConsultationState> = _consultationState

    fun startConsultation(appointmentId: String) {
        val veterinarianId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _consultationState.value = _consultationState.value.copy(isLoading = true)

                // Obtener datos de la cita
                val appointmentDoc = firestore.collection("appointments")
                    .document(appointmentId)
                    .get()
                    .await()

                val appointment =
                    appointmentDoc.toObject<Appointment>()?.copy(id = appointmentDoc.id)

                if (appointment != null) {
                    // Crear consulta
                    val consultation = Consultation(
                        appointmentId = appointmentId,
                        petId = appointment.petId,
                        createdAt = System.currentTimeMillis()
                    )

                    val consultationRef = firestore.collection("consultations")
                        .add(consultation)
                        .await()


                    // Cargar información de la mascota
                    val petDoc = firestore.collection("pets")
                        .document(appointment.petId)
                        .get()
                        .await()

                    val pet = petDoc.toObject<Pet>()?.copy(id = petDoc.id)

                    _consultationState.value = _consultationState.value.copy(
                        isLoading = false,
                        activeConsultation = consultation.copy(id = consultationRef.id),
                        currentPet = pet,
                        currentAppointment = appointment
                    )

                    // Actualizar estado de la cita
                    firestore.collection("appointments")
                        .document(appointmentId)
                        .update(
                            mapOf(
                                "status" to AppointmentStatus.COMPLETED.name,
                                "consultationId" to consultationRef.id
                            )
                        )
                        .await()
                }
            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    isLoading = false,
                    error = "Error al iniciar consulta: ${e.message}"
                )
            }
        }
    }

    fun updateClinicalData(
        weight: Float?,
        temperature: Float?,
        symptoms: String,
        diagnosis: String,
        treatment: String,
        observations: String
    ) {
        val consultationId = _consultationState.value.activeConsultation?.id ?: return

        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "weight" to weight,
                    "temperature" to temperature,
                    "symptoms" to symptoms,
                    "diagnosis" to diagnosis,
                    "treatment" to treatment,
                    "observations" to observations
                )

                firestore.collection("consultations")
                    .document(consultationId)
                    .update(updates)
                    .await()

                val updatedConsultation = _consultationState.value.activeConsultation?.copy(
                    weight = weight,
                    temperature = temperature,
                    symptoms = symptoms,
                    diagnosis = diagnosis,
                    treatment = treatment,
                    observations = observations
                )

                _consultationState.value = _consultationState.value.copy(
                    activeConsultation = updatedConsultation
                )

            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    error = "Error al guardar datos clínicos: ${e.message}"
                )
            }
        }
    }

    // ACTUALIZADO: ahora usa serviceId
    fun addService(serviceId: String, serviceName: String, price: Double) {
        val consultationId = _consultationState.value.activeConsultation?.id ?: return

        viewModelScope.launch {
            try {
                val service = ServiceApplied(
                    serviceId = serviceId,
                    name = serviceName,
                    price = price
                )

                val consultation = _consultationState.value.activeConsultation!!
                val updatedServices = consultation.services + service
                val total =
                    calculateTotal(updatedServices, consultation.medications, consultation.vaccines)

                firestore.collection("consultations")
                    .document(consultationId)
                    .update(
                        mapOf(
                            "services" to updatedServices,
                            "total" to total
                        )
                    )
                    .await()

                _consultationState.value = _consultationState.value.copy(
                    activeConsultation = consultation.copy(
                        services = updatedServices,
                        total = total
                    )
                )
            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    error = "Error al agregar servicio: ${e.message}"
                )
            }
        }
    }

    fun addMedication(medicationId: String, name: String, dose: String, price: Double) {
        val consultationId = _consultationState.value.activeConsultation?.id ?: return

        viewModelScope.launch {
            try {
                val medication = MedicationUsed(
                    medicationId = medicationId,
                    name = name,
                    dose = dose,
                    price = price
                )

                val consultation = _consultationState.value.activeConsultation!!
                val updatedMedications = consultation.medications + medication
                val total =
                    calculateTotal(consultation.services, updatedMedications, consultation.vaccines)

                firestore.collection("consultations")
                    .document(consultationId)
                    .update(
                        mapOf(
                            "medications" to updatedMedications,
                            "total" to total
                        )
                    )
                    .await()

                // Actualizar stock
                updateMedicationStock(medicationId, -1)

                _consultationState.value = _consultationState.value.copy(
                    activeConsultation = consultation.copy(
                        medications = updatedMedications,
                        total = total
                    )
                )
            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    error = "Error al agregar medicamento: ${e.message}"
                )
            }
        }
    }

    fun addVaccine(vaccineId: String, name: String, price: Double, nextDoseDate: Long?) {
        val consultationId = _consultationState.value.activeConsultation?.id ?: return
        val petId = _consultationState.value.activeConsultation?.petId ?: return

        viewModelScope.launch {
            try {
                val vaccine = VaccineApplied(
                    vaccineId = vaccineId,
                    name = name,
                    price = price,
                    nextDoseDate = nextDoseDate
                )

                val consultation = _consultationState.value.activeConsultation!!
                val updatedVaccines = consultation.vaccines + vaccine
                val total =
                    calculateTotal(consultation.services, consultation.medications, updatedVaccines)

                firestore.collection("consultations")
                    .document(consultationId)
                    .update(
                        mapOf(
                            "vaccines" to updatedVaccines,
                            "total" to total
                        )
                    )
                    .await()

                // Crear registro de vacunación
                val vaccinationRecord = VaccinationRecord(
                    petId = petId,
                    vaccineName = name,
                    applicationDate = System.currentTimeMillis(),
                    nextDoseDate = nextDoseDate
                )

                firestore.collection("vaccinations")
                    .add(vaccinationRecord)
                    .await()

                // Actualizar stock
                updateVaccineStock(vaccineId, -1)

                _consultationState.value = _consultationState.value.copy(
                    activeConsultation = consultation.copy(
                        vaccines = updatedVaccines,
                        total = total
                    )
                )
            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    error = "Error al aplicar vacuna: ${e.message}"
                )
            }
        }
    }

    fun finishConsultation(isPaid: Boolean) {
        val consultationId = _consultationState.value.activeConsultation?.id ?: return

        viewModelScope.launch {
            try {
                firestore.collection("consultations")
                    .document(consultationId)
                    .update("paid", isPaid)
                    .await()

                _consultationState.value = _consultationState.value.copy(
                    isConsultationFinished = true
                )
            } catch (e: Exception) {
                _consultationState.value = _consultationState.value.copy(
                    error = "Error al finalizar consulta: ${e.message}"
                )
            }
        }
    }

    private fun calculateTotal(
        services: List<ServiceApplied>,
        medications: List<MedicationUsed>,
        vaccines: List<VaccineApplied>
    ): Double {
        return services.sumOf { it.price } +
                medications.sumOf { it.price } +
                vaccines.sumOf { it.price }
    }

    private suspend fun updateMedicationStock(medicationId: String, change: Int) {
        try {
            val medicationRef = firestore.collection("medications").document(medicationId)
            val doc = medicationRef.get().await()
            val currentStock = doc.getLong("stock")?.toInt() ?: 0
            medicationRef.update("stock", currentStock + change).await()
        } catch (e: Exception) {
            // Log error
        }
    }

    private suspend fun updateVaccineStock(vaccineId: String, change: Int) {
        try {
            val vaccineRef = firestore.collection("vaccines").document(vaccineId)
            val doc = vaccineRef.get().await()
            val currentStock = doc.getLong("stock")?.toInt() ?: 0
            vaccineRef.update("stock", currentStock + change).await()
        } catch (e: Exception) {
            // Log error
        }
    }

    fun clearState() {
        _consultationState.value = ConsultationState()
    }

    fun clearError() {
        _consultationState.value = _consultationState.value.copy(error = null)
    }
}


data class ConsultationState(
    val activeConsultation: Consultation? = null,
    val currentPet: Pet? = null,
    val currentAppointment: Appointment? = null,
    val isLoading: Boolean = false,
    val isConsultationFinished: Boolean = false,
    val error: String? = null
)