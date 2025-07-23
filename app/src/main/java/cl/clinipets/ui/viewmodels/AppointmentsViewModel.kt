// Modificar el archivo app/src/main/java/cl/clinipets/ui/viewmodels/AppointmentsViewModel.kt

// ui/viewmodels/AppointmentsViewModel.kt
package cl.clinipets.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.VetSchedule
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
        loadAvailableDays()
    }

    fun loadAppointments() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoading = true)

                // Para usuarios normales, cargar sus citas
                // Para veterinarios, cargar todas las citas
                val userDoc = firestore.collection("users").document(userId).get().await()
                val isVet = userDoc.getBoolean("isVet") ?: false

                val query = if (isVet) {
                    firestore.collection("appointments")
                        .orderBy("dateTime", Query.Direction.DESCENDING)
                } else {
                    firestore.collection("appointments").whereEqualTo("ownerId", userId)
                        .orderBy("dateTime", Query.Direction.DESCENDING)
                }

                val snapshot = query.get().await()

                val appointments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Appointment>()?.copy(id = doc.id)
                }

                val upcoming = appointments.filter {
                    it.dateTime > System.currentTimeMillis() && it.status == AppointmentStatus.SCHEDULED || it.status == AppointmentStatus.CONFIRMED
                }

                val past = appointments.filter {
                    it.dateTime <= System.currentTimeMillis() || it.status == AppointmentStatus.COMPLETED
                }

                _appointmentsState.value = _appointmentsState.value.copy(
                    appointments = appointments,
                    upcomingAppointments = upcoming,
                    pastAppointments = past,
                    isLoading = false
                )
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoading = false, error = "Error al cargar citas: ${e.message}"
                )
            }
        }
    }

    fun loadAvailableDays() {
        viewModelScope.launch {
            try {
                // Obtener todos los horarios de la veterinaria
                val schedulesSnapshot = firestore.collection("vetSchedules")
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()

                val schedules = schedulesSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<VetSchedule>()
                }

                // Generar los próximos 30 días disponibles
                val availableDays = mutableListOf<AvailableDay>()
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dayFormat = SimpleDateFormat("EEEE", Locale("es"))
                val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                for (i in 1..30) { // Próximos 30 días
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    // Convertir a nuestro formato (1 = Lunes, 7 = Domingo)
                    val ourDayOfWeek = if (dayOfWeek == 1) 7 else dayOfWeek - 1

                    // Verificar si hay horario para este día
                    val daySchedule = schedules.find { it.dayOfWeek == ourDayOfWeek }

                    if (daySchedule != null) {
                        availableDays.add(
                            AvailableDay(
                                date = dateFormat.format(calendar.time),
                                dayName = dayFormat.format(calendar.time)
                                    .replaceFirstChar { it.uppercase() },
                                displayDate = displayFormat.format(calendar.time),
                                isAvailable = true,
                                startTime = daySchedule.startTime,
                                endTime = daySchedule.endTime
                            )
                        )
                    }

                    calendar.time = Date() // Reset para el siguiente día
                }

                _appointmentsState.value = _appointmentsState.value.copy(
                    availableDays = availableDays
                )
            } catch (e: Exception) {
                // Error handling silencioso
            }
        }
    }

    fun createAppointment(
        petId: String, date: String, time: String, reason: String
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
                    date = date,
                    time = time,
                    dateTime = dateTime,
                    reason = reason,
                    status = AppointmentStatus.SCHEDULED,
                    createdAt = System.currentTimeMillis()
                )

                firestore.collection("appointments").add(appointment).await()

                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoading = false, isAppointmentCreated = true
                )

                loadAppointments()
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoading = false, error = "Error al crear cita: ${e.message}"
                )
            }
        }
    }

    fun cancelAppointment(appointmentId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("appointments").document(appointmentId)
                    .update("status", AppointmentStatus.CANCELLED.name).await()

                loadAppointments()
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    error = "Error al cancelar cita: ${e.message}"
                )
            }
        }
    }

    fun confirmAppointment(appointmentId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("appointments").document(appointmentId)
                    .update("status", AppointmentStatus.CONFIRMED.name).await()

                loadAppointments()
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    error = "Error al confirmar cita: ${e.message}"
                )
            }
        }
    }

    fun loadAvailableTimeSlots(date: String) {
        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoadingSlots = true)

                // Buscar el día disponible para obtener su horario
                val selectedDay = _appointmentsState.value.availableDays.find { it.date == date }

                if (selectedDay == null) {
                    _appointmentsState.value = _appointmentsState.value.copy(
                        availableTimeSlots = emptyList(),
                        isLoadingSlots = false
                    )
                    return@launch
                }

                // Obtener citas existentes para esa fecha
                val existingAppointments = firestore.collection("appointments")
                    .whereEqualTo("date", date)
                    .whereIn(
                        "status",
                        listOf(AppointmentStatus.SCHEDULED.name, AppointmentStatus.CONFIRMED.name)
                    )
                    .get()
                    .await()

                val bookedTimes = existingAppointments.documents.map {
                    it.getString("time") ?: ""
                }
                Log.d("AvailableTimeSlots", "Booked Times: $bookedTimes")

                // Generar slots disponibles basados en el horario del día
                val allSlots =
                    generateTimeSlotsForSchedule(selectedDay.startTime, selectedDay.endTime)
                val availableSlots = allSlots.map { time ->
                    TimeSlot(
                        time = time,
                        isAvailable = !bookedTimes.contains(time)
                    )
                }

                _appointmentsState.value = _appointmentsState.value.copy(
                    availableTimeSlots = availableSlots,
                    isLoadingSlots = false
                )
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoadingSlots = false,
                    error = "Error al cargar horarios: ${e.message}"
                )
            }
        }
    }

    private fun generateTimeSlotsForSchedule(startTime: String, endTime: String): List<String> {
        val slots = mutableListOf<String>()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        try {
            val start = timeFormat.parse(startTime) ?: return emptyList()
            val end = timeFormat.parse(endTime) ?: return emptyList()

            val calendar = Calendar.getInstance()
            calendar.time = start

            while (calendar.time.before(end) || calendar.time == end) {
                slots.add(timeFormat.format(calendar.time))
                calendar.add(Calendar.MINUTE, 30) // Slots de 30 minutos
            }
        } catch (e: Exception) {
            // Si hay error, devolver slots por defecto
            return generateTimeSlots()
        }

        return slots
    }

    private fun generateTimeSlots(): List<String> {
        val slots = mutableListOf<String>()
        for (hour in 9..17) {
            slots.add(String.format("%02d:00", hour))
            slots.add(String.format("%02d:30", hour))
        }
        return slots
    }

    fun loadUserPets() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("pets").whereEqualTo("ownerId", userId)
                    .whereEqualTo("active", true).get().await()

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

    fun clearState() {
        _appointmentsState.value = _appointmentsState.value.copy(
            isAppointmentCreated = false, error = null
        )
    }
}

data class AppointmentsState(
    val appointments: List<Appointment> = emptyList(),
    val upcomingAppointments: List<Appointment> = emptyList(),
    val pastAppointments: List<Appointment> = emptyList(),
    val userPets: List<Pet> = emptyList(),
    val availableDays: List<AvailableDay> = emptyList(),
    val availableTimeSlots: List<TimeSlot> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingSlots: Boolean = false,
    val isAppointmentCreated: Boolean = false,
    val error: String? = null
)

data class AvailableDay(
    val date: String,
    val dayName: String,
    val displayDate: String,
    val isAvailable: Boolean,
    val startTime: String,
    val endTime: String
)

data class TimeSlot(
    val time: String = "",
    val isAvailable: Boolean = true
)