package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Appointment
import cl.clinipets.data.model.AppointmentStatus
import cl.clinipets.data.model.Consultation
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.VetSchedule
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
                    loadTodayAppointments()
                } else {
                    _vetState.value = _vetState.value.copy(isVeterinarian = false)
                }
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(isVeterinarian = false)
            }
        }
    }

    fun loadTodayAppointments() {
        viewModelScope.launch {
            try {
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())

                val snapshot = firestore.collection("appointments")
                    .whereEqualTo("date", today)
                    .whereEqualTo("status", AppointmentStatus.SCHEDULED.name)
                    .orderBy("time")
                    .get()
                    .await()

                val appointments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Appointment>()?.copy(id = doc.id)
                }

                // Cargar información de mascotas
                val appointmentsWithPets = appointments.map { appointment ->
                    val petDoc = firestore.collection("pets")
                        .document(appointment.petId)
                        .get()
                        .await()

                    val pet = petDoc.toObject<Pet>()?.copy(id = petDoc.id)
                    appointment to pet
                }

                _vetState.value = _vetState.value.copy(
                    todayAppointments = appointmentsWithPets
                )
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al cargar citas: ${e.message}"
                )
            }
        }
    }

    fun loadVetSchedules() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("vetSchedules")
                    .get()
                    .await()

                val schedules = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<VetSchedule>()?.copy(id = doc.id)
                }

                _vetState.value = _vetState.value.copy(
                    vetSchedules = schedules
                )
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al cargar horarios: ${e.message}"
                )
            }
        }
    }

    fun saveVetSchedules(schedules: Map<Int, Any>) {
        viewModelScope.launch {
            try {
                // Primero, eliminar horarios existentes
                val existingSchedules = firestore.collection("vetSchedules")
                    .get()
                    .await()

                existingSchedules.documents.forEach { doc ->
                    doc.reference.delete().await()
                }

                // Guardar nuevos horarios
                schedules.forEach { (dayNumber, schedule) ->
                    // Convertir el objeto DaySchedule a VetSchedule
                    val daySchedule = schedule as? cl.clinipets.ui.screens.vet.DaySchedule
                    if (daySchedule != null) {
                        val vetSchedule = VetSchedule(
                            dayOfWeek = dayNumber,
                            startTime = daySchedule.startTime,
                            endTime = daySchedule.endTime,
                            active = daySchedule.active
                        )
                        firestore.collection("vetSchedules")
                            .add(vetSchedule)
                            .await()
                    }
                }

                _vetState.value = _vetState.value.copy(
                    error = null
                )

                loadVetSchedules()
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al guardar horarios: ${e.message}"
                )
            }
        }
    }

    fun loadWeekStats() {
        viewModelScope.launch {
            try {
                val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

                // Contar consultas de la semana
                val consultationsSnapshot = firestore.collection("consultations")
                    .whereGreaterThan("createdAt", oneWeekAgo)
                    .get()
                    .await()

                val totalConsultations = consultationsSnapshot.size()
                val totalRevenue = consultationsSnapshot.documents
                    .mapNotNull { it.toObject<Consultation>() }
                    .filter { it.paid }
                    .sumOf { it.total }

                _vetState.value = _vetState.value.copy(
                    weeklyStats = WeeklyStats(
                        consultations = totalConsultations,
                        revenue = totalRevenue
                    )
                )
            } catch (e: Exception) {
                // Error handling silencioso
            }
        }
    }

    fun clearError() {
        _vetState.value = _vetState.value.copy(error = null)
    }
}

data class VetState(
    val isVeterinarian: Boolean = false,
    val currentVetId: String? = null,
    val todayAppointments: List<Pair<Appointment, Pet?>> = emptyList(),
    val weeklyStats: WeeklyStats = WeeklyStats(),
    val vetSchedules: List<VetSchedule> = emptyList(),
    val error: String? = null
)

data class WeeklyStats(
    val consultations: Int = 0,
    val revenue: Double = 0.0
)
