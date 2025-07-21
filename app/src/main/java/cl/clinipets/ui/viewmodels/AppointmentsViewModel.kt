// ui/viewmodels/AppointmentsViewModel.kt
package cl.clinipets.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.data.model.InventoryItemType
import cl.clinipets.data.model.InventoryMovement
import cl.clinipets.data.model.MedicalConsultation
import cl.clinipets.data.model.Medication
import cl.clinipets.data.model.MedicationUsed
import cl.clinipets.data.model.MovementType
import cl.clinipets.data.model.PaymentMethod
import cl.clinipets.data.model.PaymentStatus
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.ServiceCategory
import cl.clinipets.data.model.ServiceProvided
import cl.clinipets.data.model.User
import cl.clinipets.data.model.VaccinationRecord
import cl.clinipets.data.model.Vaccine
import cl.clinipets.data.model.VaccineApplied
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
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AppointmentsViewModel @Inject constructor() : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _appointmentsState = MutableStateFlow(AppointmentsState())
    val appointmentsState: StateFlow<AppointmentsState> = _appointmentsState

    init {
        loadAppointments()
        loadUserPets()
        loadVeterinarians()
        loadServices()
    }

    // ====================== CITAS ======================

    fun loadAppointments() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoading = true)

                val snapshot = firestore.collection("appointments")
                    .whereEqualTo("ownerId", userId)
                    .orderBy("dateTime", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val appointments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Appointment>()?.copy(id = doc.id)
                }

                // Separar citas por estado
                val upcoming = appointments.filter {
                    it.dateTime > System.currentTimeMillis() &&
                            it.status in listOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED)
                }
                val past = appointments.filter {
                    it.dateTime <= System.currentTimeMillis() ||
                            it.status == AppointmentStatus.COMPLETED
                }

                _appointmentsState.value = _appointmentsState.value.copy(
                    appointments = appointments,
                    upcomingAppointments = upcoming,
                    pastAppointments = past,
                    isLoading = false
                )
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoading = false,
                    error = "Error al cargar citas: ${e.message}"
                )
            }
        }
    }

    fun loadUserPets() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("pets")
                    .whereEqualTo("ownerId", userId)
                    .whereEqualTo("active", true)
                    .get()
                    .await()

                val pets = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Pet>()?.copy(id = doc.id)
                }

                _appointmentsState.value = _appointmentsState.value.copy(
                    userPets = pets
                )
            } catch (e: Exception) {
                // Error handling silencioso
            }
        }
    }

    fun loadVeterinarians() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .whereEqualTo("isVet", true)
                    .get()
                    .await()
                Log.d("Veterinarians", "Veterinarians loaded: ${snapshot.documents.size}")
                val vets = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<User>()?.copy(id = doc.id)
                }

                _appointmentsState.value = _appointmentsState.value.copy(
                    veterinarians = vets
                )
            } catch (e: Exception) {
                // Error handling silencioso
            }
        }
    }

    fun loadServices() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("services")
                    .whereEqualTo("isActive", true)
                    .whereEqualTo("requiresAppointment", true)
                    .get()
                    .await()

                val services = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<VeterinaryService>()?.copy(id = doc.id)
                }

                _appointmentsState.value = _appointmentsState.value.copy(
                    availableServices = services
                )
            } catch (e: Exception) {
                // Error handling silencioso
            }
        }
    }

    fun loadVetAvailability(vetId: String, date: String) {
        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoadingSlots = true)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val selectedDate = dateFormat.parse(date) ?: return@launch

                val calendar = Calendar.getInstance()
                calendar.time = selectedDate
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                // Cargar configuración de horario del veterinario
                val vetDoc = firestore.collection("users")
                    .document(vetId)
                    .get()
                    .await()

                val schedule = vetDoc.get("schedule") as? Map<String, Any>
                val daySchedule = schedule?.get(dayOfWeek.toString()) as? Map<String, Any>

                if (daySchedule != null && daySchedule["isActive"] == true) {
                    val startTime = daySchedule["startTime"] as? String ?: "09:00"
                    val endTime = daySchedule["endTime"] as? String ?: "18:00"

                    // Generar slots de 30 minutos
                    val slots = generateTimeSlots(startTime, endTime)

                    // Verificar citas existentes
                    val existingAppointments = firestore.collection("appointments")
                        .whereEqualTo("veterinarianId", vetId)
                        .whereEqualTo("date", date)
                        .whereIn("status", listOf(
                            AppointmentStatus.SCHEDULED.name,
                            AppointmentStatus.CONFIRMED.name,
                            AppointmentStatus.IN_PROGRESS.name
                        ))
                        .get()
                        .await()

                    val bookedTimes = existingAppointments.documents.map {
                        it.getString("time") ?: ""
                    }

                    val availableSlots = slots.map { time ->
                        TimeSlot(
                            time = time,
                            isAvailable = !bookedTimes.contains(time)
                        )
                    }

                    _appointmentsState.value = _appointmentsState.value.copy(
                        availableTimeSlots = availableSlots,
                        isLoadingSlots = false
                    )
                } else {
                    _appointmentsState.value = _appointmentsState.value.copy(
                        availableTimeSlots = emptyList(),
                        isLoadingSlots = false,
                        error = "El veterinario no atiende este día"
                    )
                }
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoadingSlots = false,
                    error = "Error al cargar horarios: ${e.message}"
                )
            }
        }
    }

    fun createAppointment(
        petId: String,
        veterinarianId: String,
        serviceType: ServiceCategory,
        date: String,
        time: String,
        reason: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoading = true)

                val dateTimeStr = "$date $time"
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val dateTime = dateFormat.parse(dateTimeStr)?.time ?: System.currentTimeMillis()

                val appointment = Appointment(
                    petId = petId,
                    ownerId = userId,
                    veterinarianId = veterinarianId,
                    date = date,
                    time = time,
                    dateTime = dateTime,
                    serviceType = serviceType,
                    reason = reason,
                    status = AppointmentStatus.SCHEDULED,
                    createdAt = System.currentTimeMillis()
                )

                val docRef = firestore.collection("appointments")
                    .add(appointment)
                    .await()

                // Enviar notificación al veterinario
                sendAppointmentNotification(veterinarianId, appointment)

                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoading = false,
                    isAppointmentCreated = true
                )

                loadAppointments()
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoading = false,
                    error = "Error al crear cita: ${e.message}"
                )
            }
        }
    }

    fun updateAppointmentStatus(appointmentId: String, newStatus: AppointmentStatus) {
        viewModelScope.launch {
            try {
                firestore.collection("appointments")
                    .document(appointmentId)
                    .update("status", newStatus.name)
                    .await()

                loadAppointments()
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    error = "Error al actualizar estado: ${e.message}"
                )
            }
        }
    }

    fun cancelAppointment(appointmentId: String, cancellationReason: String = "") {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "status" to AppointmentStatus.CANCELLED.name,
                    "cancellationReason" to cancellationReason,
                    "cancelledAt" to System.currentTimeMillis()
                )

                firestore.collection("appointments")
                    .document(appointmentId)
                    .update(updates)
                    .await()

                loadAppointments()
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    error = "Error al cancelar cita: ${e.message}"
                )
            }
        }
    }

    // ====================== CONSULTAS MÉDICAS ======================

    fun loadConsultation(consultationId: String) {
        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoading = true)

                val doc = firestore.collection("consultations")
                    .document(consultationId)
                    .get()
                    .await()

                val consultation = doc.toObject<MedicalConsultation>()?.copy(id = doc.id)

                _appointmentsState.value = _appointmentsState.value.copy(
                    selectedConsultation = consultation,
                    isLoading = false
                )
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoading = false,
                    error = "Error al cargar consulta: ${e.message}"
                )
            }
        }
    }

    fun startConsultation(appointmentId: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoading = true)

                // Obtener datos de la cita
                val appointmentDoc = firestore.collection("appointments")
                    .document(appointmentId)
                    .get()
                    .await()

                val appointment = appointmentDoc.toObject<Appointment>()

                if (appointment != null) {
                    // Crear consulta médica
                    val consultation = MedicalConsultation(
                        appointmentId = appointmentId,
                        petId = appointment.petId,
                        veterinarianId = userId, // El veterinario actual
                        startTime = System.currentTimeMillis()
                    )

                    val consultationRef = firestore.collection("consultations")
                        .add(consultation)
                        .await()

                    // Actualizar estado de la cita
                    firestore.collection("appointments")
                        .document(appointmentId)
                        .update(mapOf(
                            "status" to AppointmentStatus.IN_PROGRESS.name,
                            "consultationId" to consultationRef.id
                        ))
                        .await()

                    _appointmentsState.value = _appointmentsState.value.copy(
                        isLoading = false,
                        activeConsultationId = consultationRef.id
                    )
                }
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoading = false,
                    error = "Error al iniciar consulta: ${e.message}"
                )
            }
        }
    }

    fun updateConsultation(
        consultationId: String,
        weight: Float?,
        temperature: Float?,
        heartRate: Int?,
        respiratoryRate: Int?,
        symptoms: String,
        clinicalExam: String,
        diagnosis: String,
        treatment: String,
        observations: String,
        recommendations: String,
        nextCheckupDays: Int?,
        nextCheckupReason: String?
    ) {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "weight" to weight,
                    "temperature" to temperature,
                    "heartRate" to heartRate,
                    "respiratoryRate" to respiratoryRate,
                    "symptoms" to symptoms,
                    "clinicalExam" to clinicalExam,
                    "diagnosis" to diagnosis,
                    "treatment" to treatment,
                    "observations" to observations,
                    "recommendations" to recommendations,
                    "nextCheckupDays" to nextCheckupDays,
                    "nextCheckupReason" to nextCheckupReason,
                    "lastUpdated" to System.currentTimeMillis()
                )

                firestore.collection("consultations")
                    .document(consultationId)
                    .update(updates.filterValues { it != null })
                    .await()

                loadConsultation(consultationId)
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    error = "Error al actualizar consulta: ${e.message}"
                )
            }
        }
    }

    fun addServiceToConsultation(consultationId: String, serviceId: String, price: Double) {
        viewModelScope.launch {
            try {
                val serviceDoc = firestore.collection("services")
                    .document(serviceId)
                    .get()
                    .await()

                val service = serviceDoc.toObject<VeterinaryService>()

                if (service != null) {
                    val serviceProvided = ServiceProvided(
                        serviceId = serviceId,
                        name = service.name,
                        category = service.category,
                        price = price
                    )

                    // Agregar servicio a la consulta
                    val consultationRef = firestore.collection("consultations")
                        .document(consultationId)

                    firestore.runTransaction { transaction ->
                        val consultationDoc = transaction.get(consultationRef)
                        val consultation = consultationDoc.toObject<MedicalConsultation>()

                        if (consultation != null) {
                            val updatedServices = consultation.services + serviceProvided
                            val subtotal = consultation.subtotal + price
                            val total = subtotal - consultation.discount

                            transaction.update(consultationRef, mapOf(
                                "services" to updatedServices,
                                "subtotal" to subtotal,
                                "total" to total
                            ))
                        }
                    }.await()

                    loadConsultation(consultationId)
                }
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    error = "Error al agregar servicio: ${e.message}"
                )
            }
        }
    }

    fun addMedicationToConsultation(
        consultationId: String,
        medicationId: String,
        dose: String,
        frequency: String,
        duration: String,
        route: String,
        quantity: Int,
        unitPrice: Double
    ) {
        viewModelScope.launch {
            try {
                val medicationDoc = firestore.collection("medications")
                    .document(medicationId)
                    .get()
                    .await()

                val medication = medicationDoc.toObject<Medication>()

                if (medication != null) {
                    val medicationUsed = MedicationUsed(
                        medicationId = medicationId,
                        name = medication.name,
                        dose = dose,
                        frequency = frequency,
                        duration = duration,
                        route = route,
                        quantity = quantity,
                        unitPrice = unitPrice,
                        totalPrice = unitPrice * quantity
                    )

                    val consultationRef = firestore.collection("consultations")
                        .document(consultationId)

                    firestore.runTransaction { transaction ->
                        val consultationDoc = transaction.get(consultationRef)
                        val consultation = consultationDoc.toObject<MedicalConsultation>()

                        if (consultation != null) {
                            val updatedMedications = consultation.medications + medicationUsed
                            val subtotal = consultation.subtotal + (unitPrice * quantity)
                            val total = subtotal - consultation.discount

                            transaction.update(consultationRef, mapOf(
                                "medications" to updatedMedications,
                                "subtotal" to subtotal,
                                "total" to total
                            ))
                        }
                    }.await()

                    // Actualizar inventario
                    updateMedicationStock(medicationId, -quantity, consultationId)

                    loadConsultation(consultationId)
                }
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    error = "Error al agregar medicamento: ${e.message}"
                )
            }
        }
    }

    fun addVaccineToConsultation(
        consultationId: String,
        vaccineId: String,
        batch: String,
        expirationDate: Long,
        nextDoseDate: Long?,
        price: Double
    ) {
        viewModelScope.launch {
            try {
                val vaccineDoc = firestore.collection("vaccines")
                    .document(vaccineId)
                    .get()
                    .await()

                val vaccine = vaccineDoc.toObject<Vaccine>()

                if (vaccine != null) {
                    val vaccineApplied = VaccineApplied(
                        vaccineId = vaccineId,
                        name = vaccine.name,
                        batch = batch,
                        expirationDate = expirationDate,
                        nextDoseDate = nextDoseDate,
                        price = price
                    )

                    val consultationRef = firestore.collection("consultations")
                        .document(consultationId)

                    firestore.runTransaction { transaction ->
                        val consultationDoc = transaction.get(consultationRef)
                        val consultation = consultationDoc.toObject<MedicalConsultation>()

                        if (consultation != null) {
                            val updatedVaccines = consultation.vaccines + vaccineApplied
                            val subtotal = consultation.subtotal + price
                            val total = subtotal - consultation.discount

                            transaction.update(consultationRef, mapOf(
                                "vaccines" to updatedVaccines,
                                "subtotal" to subtotal,
                                "total" to total
                            ))

                            // Crear registro de vacunación
                            val vaccinationRecord = VaccinationRecord(
                                petId = consultation.petId,
                                vaccineId = vaccineId,
                                vaccineName = vaccine.name,
                                consultationId = consultationId,
                                veterinarianId = consultation.veterinarianId,
                                applicationDate = System.currentTimeMillis(),
                                batch = batch,
                                expirationDate = expirationDate,
                                nextDoseDate = nextDoseDate
                            )

                            firestore.collection("vaccinations")
                                .add(vaccinationRecord)
                        }
                    }.await()

                    // Actualizar stock de vacunas
                    updateVaccineStock(vaccineId, -1)

                    loadConsultation(consultationId)
                }
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    error = "Error al agregar vacuna: ${e.message}"
                )
            }
        }
    }

    fun finishConsultation(
        consultationId: String,
        discount: Double = 0.0,
        discountReason: String? = null,
        paymentMethod: PaymentMethod,
        amountPaid: Double
    ) {
        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoading = true)

                val consultationRef = firestore.collection("consultations")
                    .document(consultationId)

                firestore.runTransaction { transaction ->
                    val consultationDoc = transaction.get(consultationRef)
                    val consultation = consultationDoc.toObject<MedicalConsultation>()

                    if (consultation != null) {
                        val total = consultation.subtotal - discount
                        val paymentStatus = when {
                            amountPaid >= total -> PaymentStatus.PAID
                            amountPaid > 0 -> PaymentStatus.PARTIAL
                            else -> PaymentStatus.PENDING
                        }

                        transaction.update(consultationRef, mapOf(
                            "discount" to discount,
                            "discountReason" to discountReason,
                            "total" to total,
                            "amountPaid" to amountPaid,
                            "paymentStatus" to paymentStatus.name,
                            "paymentMethod" to paymentMethod.name,
                            "endTime" to System.currentTimeMillis()
                        ))

                        // Actualizar estado de la cita
                        consultation.appointmentId.let { appointmentId ->
                            val appointmentRef = firestore.collection("appointments")
                                .document(appointmentId)
                            transaction.update(appointmentRef,
                                "status", AppointmentStatus.COMPLETED.name
                            )
                        }
                    }
                }.await()

                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoading = false,
                    isConsultationFinished = true,
                    activeConsultationId = null
                )

                loadAppointments()
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoading = false,
                    error = "Error al finalizar consulta: ${e.message}"
                )
            }
        }
    }

    // ====================== FUNCIONES AUXILIARES ======================

    private fun generateTimeSlots(startTime: String, endTime: String): List<String> {
        val slots = mutableListOf<String>()
        val startParts = startTime.split(":")
        val endParts = endTime.split(":")

        var currentHour = startParts[0].toInt()
        var currentMinute = startParts[1].toInt()
        val endHour = endParts[0].toInt()
        val endMinute = endParts[1].toInt()

        while (currentHour < endHour || (currentHour == endHour && currentMinute < endMinute)) {
            slots.add(String.format("%02d:%02d", currentHour, currentMinute))

            currentMinute += 30
            if (currentMinute >= 60) {
                currentHour++
                currentMinute = 0
            }
        }

        return slots
    }

    private suspend fun sendAppointmentNotification(veterinarianId: String, appointment: Appointment) {
        try {
            val notification = mapOf(
                "recipientId" to veterinarianId,
                "type" to "NEW_APPOINTMENT",
                "title" to "Nueva cita agendada",
                "message" to "Tienes una nueva cita el ${appointment.date} a las ${appointment.time}",
                "data" to mapOf(
                    "appointmentId" to appointment.id,
                    "petId" to appointment.petId
                ),
                "createdAt" to System.currentTimeMillis(),
                "read" to false
            )

            firestore.collection("notifications")
                .add(notification)
                .await()
        } catch (e: Exception) {
            // Error handling silencioso
        }
    }

    private suspend fun updateMedicationStock(medicationId: String, quantityChange: Int, consultationId: String) {
        try {
            val medicationRef = firestore.collection("medications").document(medicationId)

            firestore.runTransaction { transaction ->
                val medicationDoc = transaction.get(medicationRef)
                val currentStock = medicationDoc.getLong("stock")?.toInt() ?: 0
                val newStock = currentStock + quantityChange

                transaction.update(medicationRef, "stock", newStock)

                // Registrar movimiento de inventario
                val movement = InventoryMovement(
                    itemId = medicationId,
                    itemType = InventoryItemType.MEDICATION,
                    movementType = MovementType.OUT,
                    quantity = -quantityChange,
                    reason = "Uso en consulta",
                    consultationId = consultationId,
                    performedBy = auth.currentUser?.uid ?: "",
                    timestamp = System.currentTimeMillis()
                )

                firestore.collection("inventory_movements")
                    .add(movement)
            }.await()
        } catch (e: Exception) {
            // Error handling silencioso
        }
    }

    private suspend fun updateVaccineStock(vaccineId: String, quantityChange: Int) {
        try {
            val vaccineRef = firestore.collection("vaccines").document(vaccineId)

            firestore.runTransaction { transaction ->
                val vaccineDoc = transaction.get(vaccineRef)
                val currentStock = vaccineDoc.getLong("stock")?.toInt() ?: 0
                val newStock = currentStock + quantityChange

                transaction.update(vaccineRef, "stock", newStock)
            }.await()
        } catch (e: Exception) {
            // Error handling silencioso
        }
    }

    fun clearState() {
        _appointmentsState.value = _appointmentsState.value.copy(
            isAppointmentCreated = false,
            isConsultationFinished = false,
            error = null
        )
    }

    // ====================== REPORTES Y ESTADÍSTICAS ======================

    fun loadConsultationsByPet(petId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("consultations")
                    .whereEqualTo("petId", petId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val consultations = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<MedicalConsultation>()?.copy(id = doc.id)
                }

                _appointmentsState.value = _appointmentsState.value.copy(
                    petConsultations = consultations
                )
            } catch (e: Exception) {
                // Error handling silencioso
            }
        }
    }

    fun generateConsultationReport(consultationId: String) {
        // TODO: Implementar generación de reporte PDF
    }
}

// ====================== ESTADO ======================

data class AppointmentsState(
    // Citas
    val appointments: List<Appointment> = emptyList(),
    val upcomingAppointments: List<Appointment> = emptyList(),
    val pastAppointments: List<Appointment> = emptyList(),
    val selectedAppointment: Appointment? = null,

    // Consultas médicas
    val selectedConsultation: MedicalConsultation? = null,
    val activeConsultationId: String? = null,
    val petConsultations: List<MedicalConsultation> = emptyList(),

    // Datos auxiliares
    val userPets: List<Pet> = emptyList(),
    val veterinarians: List<User> = emptyList(),
    val availableServices: List<VeterinaryService> = emptyList(),
    val availableTimeSlots: List<TimeSlot> = emptyList(),

    // Estado UI
    val isLoading: Boolean = false,
    val isLoadingSlots: Boolean = false,
    val isAppointmentCreated: Boolean = false,
    val isConsultationFinished: Boolean = false,
    val error: String? = null
)

data class TimeSlot(
    val time: String = "",
    val isAvailable: Boolean = true
)