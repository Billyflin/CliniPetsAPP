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

    fun addAppointment(
        petId: String,
        serviceType: String,
        date: String,
        time: String,
        notes: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        if (petId.isBlank() || serviceType.isBlank() || date.isBlank() || time.isBlank()) {
            _appointmentsState.value =
                _appointmentsState.value.copy(error = "Complete todos los campos")
            return
        }

        viewModelScope.launch {
            try {
                _appointmentsState.value = _appointmentsState.value.copy(isLoading = true)

                // Encontrar la mascota seleccionada
                val pet = _appointmentsState.value.availablePets.find { it.id == petId }

                // Parsear fecha y hora
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val dateTime = dateFormat.parse("$date $time")?.time ?: System.currentTimeMillis()

                val appointment = hashMapOf(
                    "petId" to petId,
                    "petName" to (pet?.name ?: ""),
                    "serviceType" to serviceType,
                    "dateTime" to dateTime,
                    "veterinarianName" to "Dr. García", // Por defecto
                    "status" to "SCHEDULED",
                    "notes" to notes,
                    "createdAt" to System.currentTimeMillis(),
                    "userId" to userId
                )

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
    val isLoading: Boolean = false,
    val isAppointmentAdded: Boolean = false,
    val error: String? = null
)

data class Appointment(
    val id: String = "",
    val petId: String = "",
    val petName: String = "",
    val serviceType: String = "",
    val dateTime: Long = 0,
    val veterinarianName: String = "",
    val status: String = "",
    val notes: String = "",
    val userId: String = "",
    val createdAt: Long = 0
)