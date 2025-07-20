// ui/viewmodels/VetViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.*
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
import java.util.*
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

                val role = UserRole.valueOf(userDoc.getString("role") ?: "CLIENT")
                _vetState.value = _vetState.value.copy(
                    isVeterinarian = role == UserRole.VETERINARIAN,
                    currentVetId = if (role == UserRole.VETERINARIAN) userId else null
                )

                if (role == UserRole.VETERINARIAN) {
                    loadVetData()
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

    // ====================== AGENDA DEL VETERINARIO ======================

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

                // Cargar información adicional de mascotas
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

    // ====================== INVENTARIO ======================

    fun loadInventorySummary() {
        viewModelScope.launch {
            try {
                // Cargar medicamentos
                val medicationsSnapshot = firestore.collection("medications")
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()

                val medications = medicationsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<Medication>()?.copy(id = doc.id)
                }

                // Cargar vacunas
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

    fun addMedication(
        name: String,
        activeIngredient: String,
        presentation: String,
        laboratory: String,
        category: MedicationCategory,
        unitPrice: Double,
        purchasePrice: Double,
        stock: Int,
        minStock: Int,
        expirationDate: Long?,
        batch: String,
        isControlled: Boolean
    ) {
        viewModelScope.launch {
            try {
                _vetState.value = _vetState.value.copy(isLoading = true)

                val medication = Medication(
                    name = name,
                    activeIngredient = activeIngredient,
                    presentation = presentation,
                    laboratory = laboratory,
                    category = category,
                    unitPrice = unitPrice,
                    purchasePrice = purchasePrice,
                    stock = stock,
                    minStock = minStock,
                    expirationDate = expirationDate,
                    batch = batch,
                    isControlled = isControlled,
                    isActive = true,
                    lastUpdated = System.currentTimeMillis()
                )

                val docRef = firestore.collection("medications")
                    .add(medication)
                    .await()

                // Registrar movimiento de inventario
                val movement = InventoryMovement(
                    itemId = docRef.id,
                    itemType = InventoryItemType.MEDICATION,
                    movementType = MovementType.IN,
                    quantity = stock,
                    unitPrice = purchasePrice,
                    totalPrice = purchasePrice * stock,
                    reason = "Ingreso inicial",
                    performedBy = auth.currentUser?.uid ?: "",
                    timestamp = System.currentTimeMillis()
                )

                firestore.collection("inventory_movements")
                    .add(movement)
                    .await()

                _vetState.value = _vetState.value.copy(
                    isLoading = false,
                    isMedicationAdded = true
                )

                loadInventorySummary()
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    isLoading = false,
                    error = "Error al agregar medicamento: ${e.message}"
                )
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

                    // Registrar movimiento
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

    // ====================== SERVICIOS ======================

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

    fun addService(
        name: String,
        category: ServiceCategory,
        description: String,
        basePrice: Double,
        estimatedDuration: Int,
        requiresAppointment: Boolean
    ) {
        viewModelScope.launch {
            try {
                val service = VeterinaryService(
                    name = name,
                    category = category,
                    description = description,
                    basePrice = basePrice,
                    estimatedDuration = estimatedDuration,
                    requiresAppointment = requiresAppointment,
                    isActive = true
                )

                firestore.collection("services")
                    .add(service)
                    .await()

                loadServices()
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al agregar servicio: ${e.message}"
                )
            }
        }
    }

    // ====================== CONSULTAS Y REPORTES ======================

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

    fun generateDailyReport(date: Date) {
        val vetId = _vetState.value.currentVetId ?: return

        viewModelScope.launch {
            try {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)

                // Cargar consultas del día
                val consultationsSnapshot = firestore.collection("consultations")
                    .whereEqualTo("veterinarianId", vetId)
                    .whereGreaterThanOrEqualTo("startTime", getStartOfDay(date))
                    .whereLessThan("startTime", getEndOfDay(date))
                    .get()
                    .await()

                val consultations = consultationsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<MedicalConsultation>()?.copy(id = doc.id)
                }

                // Calcular estadísticas
                val totalConsultations = consultations.size
                val totalRevenue = consultations.sumOf { it.total }
                val totalPaid = consultations.sumOf { it.amountPaid }
                val pendingPayments = totalRevenue - totalPaid

                val servicesSummary = consultations
                    .flatMap { it.services }
                    .groupingBy { it.category }
                    .eachCount()

                val medicationsSummary = consultations
                    .flatMap { it.medications }
                    .groupingBy { it.name }
                    .eachCount()

                val dailyReport = DailyReport(
                    date = date,
                    veterinarianId = vetId,
                    totalConsultations = totalConsultations,
                    totalRevenue = totalRevenue,
                    totalPaid = totalPaid,
                    pendingPayments = pendingPayments,
                    servicesByCategory = servicesSummary,
                    topMedications = medicationsSummary.toList()
                        .sortedByDescending { it.second }
                        .take(10),
                    consultations = consultations
                )

                _vetState.value = _vetState.value.copy(
                    currentDailyReport = dailyReport
                )
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al generar reporte: ${e.message}"
                )
            }
        }
    }

    fun generateInventoryReport() {
        viewModelScope.launch {
            try {
                val expiringMedications = _vetState.value.medications.filter { medication ->
                    medication.expirationDate?.let { expDate ->
                        val daysUntilExpiration = (expDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
                        daysUntilExpiration <= 90
                    } ?: false
                }

                val inventoryValue = _vetState.value.medications.sumOf { it.stock * it.unitPrice } +
                        _vetState.value.vaccines.sumOf { it.stock * it.unitPrice }

                val inventoryReport = InventoryReport(
                    generatedAt = System.currentTimeMillis(),
                    totalMedications = _vetState.value.medications.size,
                    totalVaccines = _vetState.value.vaccines.size,
                    totalValue = inventoryValue,
                    lowStockItems = _vetState.value.lowStockMedications.size + _vetState.value.lowStockVaccines.size,
                    expiringItems = expiringMedications.size,
                    medicationsByCategory = _vetState.value.medications.groupingBy { it.category }.eachCount()
                )

                _vetState.value = _vetState.value.copy(
                    currentInventoryReport = inventoryReport
                )
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al generar reporte de inventario: ${e.message}"
                )
            }
        }
    }

    // ====================== ESTADÍSTICAS ======================

    fun loadVetStatistics(startDate: Date, endDate: Date) {
        val vetId = _vetState.value.currentVetId ?: return

        viewModelScope.launch {
            try {
                // Cargar consultas del período
                val consultationsSnapshot = firestore.collection("consultations")
                    .whereEqualTo("veterinarianId", vetId)
                    .whereGreaterThanOrEqualTo("startTime", startDate.time)
                    .whereLessThanOrEqualTo("startTime", endDate.time)
                    .get()
                    .await()

                val consultations = consultationsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<MedicalConsultation>()?.copy(id = doc.id)
                }

                // Calcular estadísticas
                val totalConsultations = consultations.size
                val totalRevenue = consultations.sumOf { it.total }
                val averageConsultationValue = if (totalConsultations > 0) totalRevenue / totalConsultations else 0.0

                // Consultas por día
                val consultationsByDay = consultations
                    .groupBy { consultation ->
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(Date(consultation.startTime))
                    }
                    .mapValues { it.value.size }

                // Servicios más frecuentes
                val topServices = consultations
                    .flatMap { it.services }
                    .groupingBy { it.name }
                    .eachCount()
                    .toList()
                    .sortedByDescending { it.second }
                    .take(10)

                // Diagnósticos más comunes
                val topDiagnoses = consultations
                    .map { it.diagnosis }
                    .filter { it.isNotBlank() }
                    .groupingBy { it }
                    .eachCount()
                    .toList()
                    .sortedByDescending { it.second }
                    .take(10)

                val statistics = VetStatistics(
                    period = "$startDate - $endDate",
                    totalConsultations = totalConsultations,
                    totalRevenue = totalRevenue,
                    averageConsultationValue = averageConsultationValue,
                    consultationsByDay = consultationsByDay,
                    topServices = topServices,
                    topDiagnoses = topDiagnoses
                )

                _vetState.value = _vetState.value.copy(
                    vetStatistics = statistics
                )
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al cargar estadísticas: ${e.message}"
                )
            }
        }
    }

    // ====================== FUNCIONES AUXILIARES ======================

    private fun getStartOfDay(date: Date): Long {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(date: Date): Long {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun clearState() {
        _vetState.value = _vetState.value.copy(
            isMedicationAdded = false,
            isVaccineAdded = false,
            error = null
        )
    }
}

// ====================== ESTADO Y MODELOS AUXILIARES ======================

data class VetState(
    // Control de acceso
    val isVeterinarian: Boolean = false,
    val currentVetId: String? = null,

    // Agenda
    val todayAppointments: List<Pair<Appointment, Pet?>> = emptyList(),
    val vetSchedule: Map<Int, DaySchedule> = emptyMap(),

    // Inventario
    val medications: List<Medication> = emptyList(),
    val vaccines: List<Vaccine> = emptyList(),
    val filteredMedications: List<Medication> = emptyList(),
    val lowStockMedications: List<Medication> = emptyList(),
    val lowStockVaccines: List<Vaccine> = emptyList(),
    val inventorySummary: InventorySummary? = null,
    val medicationSearchQuery: String = "",

    // Servicios
    val services: List<VeterinaryService> = emptyList(),

    // Consultas
    val recentConsultations: List<MedicalConsultation> = emptyList(),

    // Reportes
    val currentDailyReport: DailyReport? = null,
    val currentInventoryReport: InventoryReport? = null,
    val vetStatistics: VetStatistics? = null,

    // Estado UI
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

data class DailyReport(
    val date: Date,
    val veterinarianId: String,
    val totalConsultations: Int,
    val totalRevenue: Double,
    val totalPaid: Double,
    val pendingPayments: Double,
    val servicesByCategory: Map<ServiceCategory, Int>,
    val topMedications: List<Pair<String, Int>>,
    val consultations: List<MedicalConsultation>
)

data class InventoryReport(
    val generatedAt: Long,
    val totalMedications: Int,
    val totalVaccines: Int,
    val totalValue: Double,
    val lowStockItems: Int,
    val expiringItems: Int,
    val medicationsByCategory: Map<MedicationCategory, Int>
)

data class VetStatistics(
    val period: String,
    val totalConsultations: Int,
    val totalRevenue: Double,
    val averageConsultationValue: Double,
    val consultationsByDay: Map<String, Int>,
    val topServices: List<Pair<String, Int>>,
    val topDiagnoses: List<Pair<String, Int>>
)