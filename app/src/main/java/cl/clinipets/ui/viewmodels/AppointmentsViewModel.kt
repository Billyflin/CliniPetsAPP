// ui/viewmodels/AppointmentsViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
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
    private val petsViewModel = PetsViewModel()

    private val _appointmentsState = MutableStateFlow(AppointmentsState())
    val appointmentsState: StateFlow<AppointmentsState> = _appointmentsState

    init {
        loadAppointments()
        loadPetsForAppointment()
        loadVeterinarians()
    }

    fun loadAppointments() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoading = true)

                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("appointments")
                    .get()
                    .await()

                val appointments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Appointment>()?.copy(id = doc.id)
                }.sortedBy { it.dateTime }

                _appointmentsState.value = _appointmentsState.value.copy(
                    appointments = appointments,
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

    private fun loadPetsForAppointment() {
        viewModelScope.launch {
            petsViewModel.loadPets()
            petsViewModel.petsState.collect { petsState ->
                _appointmentsState.value = _appointmentsState.value.copy(
                    availablePets = petsState.pets
                )
            }
        }
    }

    private fun loadVeterinarians() {
        viewModelScope.launch {
            try {
                // Cargar usuarios que son veterinarios
                val snapshot = firestore.collection("users")
                    .whereEqualTo("isVet", true)
                    .get()
                    .await()

                val vets = snapshot.documents.mapNotNull { doc ->
                    SimpleVet(
                        id = doc.id,
                        name = doc.getString("name") ?: "Veterinario",
                        email = doc.getString("email") ?: ""
                    )
                }

                _appointmentsState.value = _appointmentsState.value.copy(
                    availableVets = vets
                )
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    fun loadVetAvailability(vetId: String, date: String) {
        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoadingSlots = true)

                // Parsear fecha
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val selectedDate = dateFormat.parse(date) ?: return@launch

                // Obtener día de la semana (1=Domingo, 2=Lunes, etc)
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

                    // Verificar citas existentes para ese día
                    val dateString = dateFormat.format(selectedDate)
                    val existingAppointments = firestore.collection("appointments")
                        .whereEqualTo("veterinarianId", vetId)
                        .whereEqualTo("date", dateString)
                        .get()
                        .await()

                    val bookedTimes = existingAppointments.documents.map {
                        it.getString("time") ?: ""
                    }

                    // Marcar slots disponibles
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

            // Avanzar 30 minutos
            currentMinute += 30
            if (currentMinute >= 60) {
                currentHour++
                currentMinute = 0
            }
        }

        return slots
    }

    fun addAppointment(
        petId: String,
        vetId: String,
        serviceType: String,
        date: String,
        time: String,
        notes: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        if (petId.isBlank() || vetId.isBlank() || serviceType.isBlank() || date.isBlank() || time.isBlank()) {
            _appointmentsState.value =
                _appointmentsState.value.copy(error = "Complete todos los campos")
            return
        }

        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoading = true)

                // Encontrar la mascota y veterinario seleccionados
                val pet = _appointmentsState.value.availablePets.find { it.id == petId }
                val vet = _appointmentsState.value.availableVets.find { it.id == vetId }

                // Parsear fecha y hora
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val dateTime = dateFormat.parse("$date $time")?.time ?: System.currentTimeMillis()

                val appointment = hashMapOf(
                    "petId" to petId,
                    "petName" to (pet?.name ?: ""),
                    "veterinarianId" to vetId,
                    "veterinarianName" to (vet?.name ?: ""),
                    "serviceType" to serviceType,
                    "date" to date,
                    "time" to time,
                    "dateTime" to dateTime,
                    "status" to "SCHEDULED",
                    "notes" to notes,
                    "createdAt" to System.currentTimeMillis(),
                    "userId" to userId
                )

                // Guardar en colección global de appointments
                firestore.collection("appointments")
                    .add(appointment)
                    .await()

                // También guardar referencia en el usuario
                firestore.collection("users")
                    .document(userId)
                    .collection("appointments")
                    .add(appointment)
                    .await()

                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoading = false,
                    isAppointmentAdded = true
                )

                loadAppointments()
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    isLoading = false,
                    error = "Error al agendar cita: ${e.message}"
                )
            }
        }
    }

    fun cancelAppointment(appointmentId: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("appointments")
                    .document(appointmentId)
                    .update("status", "CANCELLED")
                    .await()

                loadAppointments()
            } catch (e: Exception) {
                _appointmentsState.value = _appointmentsState.value.copy(
                    error = "Error al cancelar cita: ${e.message}"
                )
            }
        }
    }

    fun clearState() {
        _appointmentsState.value = _appointmentsState.value.copy(
            isAppointmentAdded = false,
            error = null
        )
    }
}

data class AppointmentsState(
    val appointments: List<Appointment> = emptyList(),
    val availablePets: List<Pet> = emptyList(),
    val availableVets: List<SimpleVet> = emptyList(),
    val availableTimeSlots: List<TimeSlot> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingSlots: Boolean = false,
    val isAppointmentAdded: Boolean = false,
    val error: String? = null
)

data class SimpleVet(
    val id: String = "",
    val name: String = "",
    val email: String = ""
)

data class TimeSlot(
    val time: String = "",
    val isAvailable: Boolean = true
)

data class Appointment(
    val id: String = "",
    val petId: String = "",
    val petName: String = "",
    val veterinarianId: String = "",
    val veterinarianName: String = "",
    val serviceType: String = "",
    val date: String = "",
    val time: String = "",
    val dateTime: Long = 0,
    val status: String = "",
    val notes: String = "",
    val userId: String = "",
    val createdAt: Long = 0
)