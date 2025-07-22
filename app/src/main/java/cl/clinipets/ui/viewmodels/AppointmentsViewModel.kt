// ui/viewmodels/AppointmentsViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.data.model.Pet
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

                // Obtener citas existentes para esa fecha shceduleada o comfirmed
                val existingAppointments =
                    firestore.collection("appointments").whereEqualTo("date", date)
                        .whereEqualTo("status", AppointmentStatus.SCHEDULED.name)
                        .whereEqualTo("status", AppointmentStatus.CONFIRMED.name).get().await()

                val bookedTimes = existingAppointments.documents.map {
                    it.getString("time") ?: ""
                }

                // Generar slots disponibles (cada 30 minutos de 9:00 a 18:00)
                val allSlots = generateTimeSlots()
                val availableSlots = allSlots.map { time ->
                    TimeSlot(
                        time = time, isAvailable = !bookedTimes.contains(time)
                    )
                }

                _appointmentsState.value = _appointmentsState.value.copy(
                    availableTimeSlots = availableSlots, isLoadingSlots = false
                )
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoadingSlots = false, error = "Error al cargar horarios: ${e.message}"
                )
            }
        }
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
    val availableTimeSlots: List<TimeSlot> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingSlots: Boolean = false,
    val isAppointmentCreated: Boolean = false,
    val error: String? = null
)

data class TimeSlot(
    val time: String = "", val isAvailable: Boolean = true
)