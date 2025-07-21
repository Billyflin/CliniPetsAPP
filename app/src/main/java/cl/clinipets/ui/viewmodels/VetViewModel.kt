// ui/viewmodels/VetViewModel.kt (VERSIÓN SIMPLIFICADA)
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.InventoryItemType
import cl.clinipets.data.model.InventoryMovement
import cl.clinipets.data.model.MedicalConsultation
import cl.clinipets.data.model.Medication
import cl.clinipets.data.model.MovementType
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.Vaccine
import cl.clinipets.data.model.VeterinaryService
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class VetViewModel @Inject constructor() : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _vetState = MutableStateFlow(VetState())
    val vetState: StateFlow<VetState> = _vetState

    init {
        checkVetRole()
    }

    private fun checkVetRole() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
                val isVet = userDoc.getBoolean("isVet") ?: false
                if (isVet) {
                    _vetState.value = _vetState.value.copy(
                        isVeterinarian = true,
                        currentVetId = userId
                    )
                    loadVetData()
                } else {
                    _vetState.value = _vetState.value.copy(isVeterinarian = false)
                }
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(isVeterinarian = false)
            }
        }
    }

    private fun loadVetData() {
        loadTodayAppointments()
        loadVetSchedule()
        loadInventorySummary()
        loadLowStockItems()
        loadRecentConsultations()
    }

    fun loadTodayAppointments() {
        val vetId = _vetState.value.currentVetId ?: return

        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val snapshot = firestore.collection("appointments")
                    .whereEqualTo("veterinarianId", vetId)
                    .whereEqualTo("date", today)
                    .orderBy("time")
                    .get()
                    .await()

                val appointments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Appointment>()?.copy(id = doc.id)
                }

                val appointmentsWithPetInfo = appointments.map { appointment ->
                    val petDoc = firestore.collection("pets")
                        .document(appointment.petId)
                        .get()
                        .await()

                    val pet = petDoc.toObject<Pet>()
                    appointment to pet
                }

                _vetState.value = _vetState.value.copy(
                    todayAppointments = appointmentsWithPetInfo
                )
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al cargar citas del día: ${e.message}"
                )
            }
        }
    }

    fun loadVetSchedule() {
        val vetId = _vetState.value.currentVetId ?: return

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users")
                    .document(vetId)
                    .get()
                    .await()

                val schedule = userDoc.get("schedule") as? Map<String, Any> ?: emptyMap()

                val daySchedules = mutableMapOf<Int, DaySchedule>()
                for (day in 1..7) {
                    val dayData = schedule[day.toString()] as? Map<String, Any>
                    daySchedules[day] = if (dayData != null) {
                        DaySchedule(
                            isActive = dayData["isActive"] as? Boolean == true,
                            startTime = dayData["startTime"] as? String ?: "09:00",
                            endTime = dayData["endTime"] as? String ?: "18:00"
                        )
                    } else {
                        DaySchedule()
                    }
                }

                _vetState.value = _vetState.value.copy(
                    vetSchedule = daySchedules
                )
            } catch (e: Exception) {
                // Error handling silencioso
            }
        }
    }

    fun updateVetSchedule(dayNumber: Int, isActive: Boolean, startTime: String, endTime: String) {
        val vetId = _vetState.value.currentVetId ?: return

        viewModelScope.launch {
            try {
                val scheduleUpdate = mapOf(
                    "schedule.$dayNumber" to mapOf(
                        "isActive" to isActive,
                        "startTime" to startTime,
                        "endTime" to endTime
                    )
                )

                firestore.collection("users")
                    .document(vetId)
                    .update(scheduleUpdate)
                    .await()

                loadVetSchedule()
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al actualizar horario: ${e.message}"
                )
            }
        }
    }

    fun loadInventorySummary() {
        viewModelScope.launch {
            try {
                val medicationsSnapshot = firestore.collection("medications")
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()

                val medications = medicationsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<Medication>()?.copy(id = doc.id)
                }

                val vaccinesSnapshot = firestore.collection("vaccines")
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()

                val vaccines = vaccinesSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<Vaccine>()?.copy(id = doc.id)
                }

                val totalItems = medications.size + vaccines.size
                val totalValue = medications.sumOf { it.stock * it.unitPrice } +
                        vaccines.sumOf { it.stock * it.unitPrice }

                _vetState.value = _vetState.value.copy(
                    medications = medications,
                    vaccines = vaccines,
                    inventorySummary = InventorySummary(
                        totalItems = totalItems,
                        totalValue = totalValue,
                        lowStockCount = medications.count { it.stock <= it.minStock } +
                                vaccines.count { it.stock <= it.minStock }
                    )
                )
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al cargar inventario: ${e.message}"
                )
            }
        }
    }

    fun loadLowStockItems() {
        viewModelScope.launch {
            try {
                val lowStockMedications = firestore.collection("medications")
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        doc.toObject<Medication>()?.copy(id = doc.id)
                    }
                    .filter { it.stock <= it.minStock }

                val lowStockVaccines = firestore.collection("vaccines")
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        doc.toObject<Vaccine>()?.copy(id = doc.id)
                    }
                    .filter { it.stock <= it.minStock }

                _vetState.value = _vetState.value.copy(
                    lowStockMedications = lowStockMedications,
                    lowStockVaccines = lowStockVaccines
                )
            } catch (e: Exception) {
                // Error handling silencioso
            }
        }
    }

    fun updateMedicationStock(medicationId: String, newStock: Int, reason: String) {
        viewModelScope.launch {
            try {
                val medicationRef = firestore.collection("medications").document(medicationId)

                firestore.runTransaction { transaction ->
                    val medicationDoc = transaction.get(medicationRef)
                    val currentStock = medicationDoc.getLong("stock")?.toInt() ?: 0
                    val difference = newStock - currentStock

                    transaction.update(medicationRef, mapOf(
                        "stock" to newStock,
                        "lastUpdated" to System.currentTimeMillis()
                    ))

                    val movement = InventoryMovement(
                        itemId = medicationId,
                        itemType = InventoryItemType.MEDICATION,
                        movementType = if (difference > 0) MovementType.IN else MovementType.ADJUSTMENT,
                        quantity = difference,
                        reason = reason,
                        performedBy = auth.currentUser?.uid ?: "",
                        timestamp = System.currentTimeMillis()
                    )

                    firestore.collection("inventory_movements")
                        .add(movement)
                }.await()

                loadInventorySummary()
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al actualizar stock: ${e.message}"
                )
            }
        }
    }

    fun searchMedications(query: String) {
        val filtered = if (query.isBlank()) {
            _vetState.value.medications
        } else {
            _vetState.value.medications.filter { medication ->
                medication.name.contains(query, ignoreCase = true) ||
                        medication.activeIngredient.contains(query, ignoreCase = true) ||
                        medication.laboratory.contains(query, ignoreCase = true)
            }
        }

        _vetState.value = _vetState.value.copy(
            filteredMedications = filtered,
            medicationSearchQuery = query
        )
    }

    fun loadServices() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("services")
                    .whereEqualTo("isActive", true)
                    .orderBy("category")
                    .orderBy("name")
                    .get()
                    .await()

                val services = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<VeterinaryService>()?.copy(id = doc.id)
                }

                _vetState.value = _vetState.value.copy(
                    services = services
                )
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al cargar servicios: ${e.message}"
                )
            }
        }
    }

    fun loadRecentConsultations() {
        val vetId = _vetState.value.currentVetId ?: return

        viewModelScope.launch {
            try {
                val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

                val snapshot = firestore.collection("consultations")
                    .whereEqualTo("veterinarianId", vetId)
                    .whereGreaterThan("createdAt", oneWeekAgo)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .await()

                val consultations = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<MedicalConsultation>()?.copy(id = doc.id)
                }

                _vetState.value = _vetState.value.copy(
                    recentConsultations = consultations
                )
            } catch (e: Exception) {
                // Error handling silencioso
            }
        }
    }

    fun clearState() {
        _vetState.value = _vetState.value.copy(
            isMedicationAdded = false,
            isVaccineAdded = false,
            error = null
        )
    }
}

// Estado simplificado
data class VetState(
    val isVeterinarian: Boolean = false,
    val currentVetId: String? = null,
    val todayAppointments: List<Pair<Appointment, Pet?>> = emptyList(),
    val vetSchedule: Map<Int, DaySchedule> = emptyMap(),
    val medications: List<Medication> = emptyList(),
    val vaccines: List<Vaccine> = emptyList(),
    val filteredMedications: List<Medication> = emptyList(),
    val lowStockMedications: List<Medication> = emptyList(),
    val lowStockVaccines: List<Vaccine> = emptyList(),
    val inventorySummary: InventorySummary? = null,
    val medicationSearchQuery: String = "",
    val services: List<VeterinaryService> = emptyList(),
    val recentConsultations: List<MedicalConsultation> = emptyList(),
    val isLoading: Boolean = false,
    val isMedicationAdded: Boolean = false,
    val isVaccineAdded: Boolean = false,
    val error: String? = null
)

data class DaySchedule(
    val isActive: Boolean = false,
    val startTime: String = "09:00",
    val endTime: String = "18:00"
)

data class InventorySummary(
    val totalItems: Int = 0,
    val totalValue: Double = 0.0,
    val lowStockCount: Int = 0
)