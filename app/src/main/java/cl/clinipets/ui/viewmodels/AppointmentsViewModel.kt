// ui/viewmodels/AppointmentsViewModel.kt
package cl.clinipets.ui.viewmodels

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
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AppointmentsViewModel @Inject constructor() : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _appointmentsState = MutableStateFlow(AppointmentsState())
    val appointmentsState: StateFlow<AppointmentsState> = _appointmentsState

    /* ---------- CARGAR Citas ---------- */
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

                val isVet = firestore.collection("users")
                    .document(userId).get().await()
                    .getBoolean("isVet") ?: false

                val query = if (isVet) {
                    firestore.collection("appointments")
                        .orderBy("dateTime", Query.Direction.DESCENDING)
                } else {
                    firestore.collection("appointments")
                        .whereEqualTo("ownerId", userId)
                        .orderBy("dateTime", Query.Direction.DESCENDING)
                }

                val appointments = query.get().await().documents.mapNotNull { d ->
                    d.toObject<Appointment>()?.copy(id = d.id)
                }

                val now = System.currentTimeMillis()
                val upcoming = appointments.filter {
                    it.dateTime > now &&
                            (it.status == AppointmentStatus.SCHEDULED ||
                                    it.status == AppointmentStatus.CONFIRMED)
                }
                val past = appointments.filter {
                    it.dateTime <= now ||
                            it.status == AppointmentStatus.COMPLETED ||
                            it.status == AppointmentStatus.CANCELLED
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

    /* ---------- DÍAS DISPONIBLES ---------- */
    fun loadAvailableDays() {
        viewModelScope.launch {
            try {
                val schedules = firestore.collection("vetSchedules")
                    .whereEqualTo("active", true)
                    .get().await().documents.mapNotNull { it.toObject<VetSchedule>() }

                val availableDays = mutableListOf<AvailableDay>()
                val calendar = Calendar.getInstance()
                val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dayFmt = SimpleDateFormat("EEEE", Locale("es"))
                val viewFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                for (i in 0 until 30) {
                    if (i > 0) calendar.add(Calendar.DAY_OF_MONTH, 1)

                    val wk = calendar.get(Calendar.DAY_OF_WEEK)
                    val ourDay = if (wk == Calendar.SUNDAY) 7 else wk - 1
                    val sch = schedules.find { it.dayOfWeek == ourDay } ?: continue

                    availableDays += AvailableDay(
                        date = dateFmt.format(calendar.time),
                        dayName = dayFmt.format(calendar.time).replaceFirstChar { it.uppercase() },
                        displayDate = viewFmt.format(calendar.time),
                        isAvailable = true,
                        startTime = sch.startTime,
                        endTime = sch.endTime
                    )
                }

                _appointmentsState.value =
                    _appointmentsState.value.copy(availableDays = availableDays)

            } catch (e: Exception) {
                _appointmentsState.value =
                    _appointmentsState.value.copy(error = "Error al cargar días: ${e.message}")
            }
        }
    }

    /* ---------- SLOTS HORARIOS ---------- */
    fun loadAvailableTimeSlots(date: String) {
        viewModelScope.launch {
            try {
                _appointmentsState.value =
                    _appointmentsState.value.copy(isLoadingSlots = true)

                val day = _appointmentsState.value.availableDays.find { it.date == date }
                if (day == null) {
                    _appointmentsState.value =
                        _appointmentsState.value.copy(
                            availableTimeSlots = emptyList(),
                            isLoadingSlots = false
                        )
                    return@launch
                }

                val booked = firestore.collection("appointments")
                    .whereEqualTo("date", date)
                    .whereIn(
                        "status",
                        listOf(
                            AppointmentStatus.SCHEDULED.name,
                            AppointmentStatus.CONFIRMED.name
                        )
                    )
                    .get().await().documents.mapNotNull { it.getString("time") }

                val allSlots = generateTimeSlotsForSchedule(day.startTime, day.endTime)
                val free = allSlots.map { t -> TimeSlot(t, !booked.contains(t)) }

                _appointmentsState.value =
                    _appointmentsState.value.copy(
                        availableTimeSlots = free,
                        isLoadingSlots = false
                    )

            } catch (e: Exception) {
                _appointmentsState.value =
                    _appointmentsState.value.copy(
                        isLoadingSlots = false,
                        error = "Error al cargar horarios: ${e.message}"
                    )
            }
        }
    }

    private fun generateTimeSlotsForSchedule(startTime: String, endTime: String): List<String> {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        return try {
            val start = fmt.parse(startTime) ?: return emptyList()
            val end = fmt.parse(endTime) ?: return emptyList()
            val cal = Calendar.getInstance().apply { time = start }
            buildList {
                while (cal.time.before(end)) {
                    add(fmt.format(cal.time))
                    cal.add(Calendar.MINUTE, 30)
                }
            }
        } catch (_: Exception) {
            generateDefaultSlots()
        }
    }

    private fun generateDefaultSlots(): List<String> =
        (9..17).flatMap { listOf("%02d:00".format(it), "%02d:30".format(it)) }

    /* ---------- CREAR / ACTUALIZAR CITAS ---------- */
    fun createAppointment(petId: String, date: String, time: String, reason: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoading = true)

                val dtStr = "$date $time"
                val dt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .parse(dtStr)?.time ?: System.currentTimeMillis()

                val appt = Appointment(
                    petId = petId,
                    ownerId = userId,
                    date = date,
                    time = time,
                    dateTime = dt,
                    reason = reason,
                    status = AppointmentStatus.SCHEDULED,
                    createdAt = System.currentTimeMillis()
                )

                firestore.collection("appointments").add(appt).await()
                _appointmentsState.value =
                    _appointmentsState.value.copy(isLoading = false, isAppointmentCreated = true)
                loadAppointments()
            } catch (e: Exception) {
                _appointmentsState.value =
                    _appointmentsState.value.copy(
                        isLoading = false,
                        error = "Error al crear cita: ${e.message}"
                    )
            }
        }
    }

    fun cancelAppointment(id: String) = updateStatus(id, AppointmentStatus.CANCELLED)
    fun confirmAppointment(id: String) = updateStatus(id, AppointmentStatus.CONFIRMED)

    private fun updateStatus(id: String, status: AppointmentStatus) {
        viewModelScope.launch {
            try {
                firestore.collection("appointments").document(id)
                    .update("status", status.name).await()
                loadAppointments()
            } catch (e: Exception) {
                _appointmentsState.value =
                    _appointmentsState.value.copy(error = "Error al actualizar cita: ${e.message}")
            }
        }
    }

    /* ---------- MASCOTAS ---------- */
    fun loadUserPets() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val pets = firestore.collection("pets")
                .whereEqualTo("ownerId", userId)
                .whereEqualTo("active", true)
                .get().await().documents.mapNotNull { it.toObject<Pet>()?.copy(id = it.id) }

            _appointmentsState.value = _appointmentsState.value.copy(userPets = pets)
        }
    }

    /* ---------- LIMPIEZA DE ESTADO ---------- */
    fun clearState() {
        _appointmentsState.value =
            _appointmentsState.value.copy(isAppointmentCreated = false, error = null)
    }
}

/* ---------- STATE ---------- */
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
