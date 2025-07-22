// ui/viewmodels/AppointmentsViewModel.kt (SIMPLIFICADO)
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.ServiceCategory
import cl.clinipets.data.model.User
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

                val upcoming = appointments.filter {
                    it.dateTime > System.currentTimeMillis() &&
                            it.status in listOf(
                        AppointmentStatus.SCHEDULED,
                        AppointmentStatus.CONFIRMED
                    )
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

                firestore.collection("appointments")
                    .add(appointment)
                    .await()

                // Enviar notificación
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

    fun loadVetAvailability(vetId: String, date: String) {
        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoadingSlots = true)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val selectedDate = dateFormat.parse(date) ?: return@launch

                val calendar = Calendar.getInstance()
                calendar.time = selectedDate
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                val adjustedDayOfWeek = when (dayOfWeek) {
                    Calendar.SUNDAY -> 7
                    else -> dayOfWeek - 1
                }

                // Cargar horario del veterinario
                val vetDoc = firestore.collection("users")
                    .document(vetId)
                    .get()
                    .await()

                val schedule = vetDoc.get("schedule") as? Map<String, Any>
                val daySchedule = schedule?.get(adjustedDayOfWeek.toString()) as? Map<String, Any>

                if (daySchedule != null && daySchedule["isActive"] == true) {
                    val startTime = daySchedule["startTime"] as? String ?: "09:00"
                    val endTime = daySchedule["endTime"] as? String ?: "18:00"

                    val slots = generateTimeSlots(startTime, endTime)

                    // Verificar citas existentes
                    val existingAppointments = firestore.collection("appointments")
                        .whereEqualTo("veterinarianId", vetId)
                        .whereEqualTo("date", date)
                        .whereIn(
                            "status", listOf(
                                AppointmentStatus.SCHEDULED.name,
                                AppointmentStatus.CONFIRMED.name,
                                AppointmentStatus.IN_PROGRESS.name
                            )
                        )
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

    private suspend fun sendAppointmentNotification(
        veterinarianId: String,
        appointment: Appointment
    ) {
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

    fun clearState() {
        _appointmentsState.value = _appointmentsState.value.copy(
            isAppointmentCreated = false,
            error = null
        )
    }
}

data class AppointmentsState(
    val appointments: List<Appointment> = emptyList(),
    val upcomingAppointments: List<Appointment> = emptyList(),
    val pastAppointments: List<Appointment> = emptyList(),
    val selectedAppointment: Appointment? = null,
    val userPets: List<Pet> = emptyList(),
    val veterinarians: List<User> = emptyList(),
    val availableServices: List<VeterinaryService> = emptyList(),
    val availableTimeSlots: List<TimeSlot> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingSlots: Boolean = false,
    val isAppointmentCreated: Boolean = false,
    val error: String? = null
)

data class TimeSlot(
    val time: String = "",
    val isAvailable: Boolean = true
)