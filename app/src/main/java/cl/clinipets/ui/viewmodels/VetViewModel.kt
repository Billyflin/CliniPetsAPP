package cl.clinipets.ui.viewmodels

import android.util.Log
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
import java.text.SimpleDateFormat
import java.util.Calendar
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
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date())

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

                // Cargar nombres de dueños
                val appointmentDetails = appointmentsWithPets.map { (appointment, pet) ->
                    val ownerName = pet?.ownerId?.let { ownerId ->
                        try {
                            firestore.collection("users")
                                .document(ownerId)
                                .get()
                                .await()
                                .getString("name")
                        } catch (e: Exception) {
                            null
                        }
                    }

                    AppointmentDetail(
                        appointment = appointment,
                        pet = pet,
                        ownerName = ownerName
                    )
                }
                _vetState.value = _vetState.value.copy(
                    todayAppointments = appointmentDetails,
                    error = null
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

                Log.d("VetViewModel", "Consultas de la semana: $totalConsultations")

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


    fun loadWeekAppointments(dateStr: String) {
        viewModelScope.launch {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                val calendar = Calendar.getInstance().apply {
                    time = date ?: Date()
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                }
                val startOfWeek = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(calendar.time)

                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val endOfWeek = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(calendar.time)

                val snapshot = firestore.collection("appointments")
                    .whereGreaterThanOrEqualTo("date", startOfWeek)
                    .whereLessThanOrEqualTo("date", endOfWeek)
                    .orderBy("date")
                    .orderBy("time")
                    .get()
                    .await()

                val appointments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Appointment>()?.copy(id = doc.id)
                }

                // Cargar detalles completos
                val appointmentDetails = appointments.map { appointment ->
                    val petDoc = firestore.collection("pets")
                        .document(appointment.petId)
                        .get()
                        .await()
                    val pet = petDoc.toObject<Pet>()?.copy(id = petDoc.id)

                    val ownerName = pet?.ownerId?.let { ownerId ->
                        try {
                            firestore.collection("users")
                                .document(ownerId)
                                .get()
                                .await()
                                .getString("name")
                        } catch (e: Exception) {
                            null
                        }
                    }

                    AppointmentDetail(
                        appointment = appointment,
                        pet = pet,
                        ownerName = ownerName
                    )
                }

                _vetState.value = _vetState.value.copy(
                    agendaAppointments = appointmentDetails
                )
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al cargar agenda: ${e.message}"
                )
            }
        }
    }

    //loadDayAppointments
    fun loadDayAppointments(dateStr: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("appointments")
                    .whereEqualTo("date", dateStr)
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
                // Cargar nombres de dueños
                val appointmentDetails = appointmentsWithPets.map { (appointment, pet) ->
                    val ownerName = pet?.ownerId?.let { ownerId ->
                        try {
                            firestore.collection("users")
                                .document(ownerId)
                                .get()
                                .await()
                                .getString("name")
                        } catch (e: Exception) {
                            null
                        }
                    }

                    AppointmentDetail(
                        appointment = appointment,
                        pet = pet,
                        ownerName = ownerName
                    )
                }
                _vetState.value = _vetState.value.copy(
                    todayAppointments = appointmentDetails,
                    error = null
                )


            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al cargar citas del día: ${e.message}"
                )
            }
        }
    }

    //loadMonthAppointments

    fun loadMonthAppointments(dateStr: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("appointments")
                    .whereEqualTo("date", dateStr)
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

                // Cargar nombres de dueños
                val appointmentDetails = appointmentsWithPets.map { (appointment, pet) ->
                    val ownerName = pet?.ownerId?.let { ownerId ->
                        try {
                            firestore.collection("users")
                                .document(ownerId)
                                .get()
                                .await()
                                .getString("name")
                        } catch (e: Exception) {
                            null
                        }
                    }

                    AppointmentDetail(
                        appointment = appointment,
                        pet = pet,
                        ownerName = ownerName
                    )
                }


            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error al cargar citas del mes: ${e.message}"
                )
            }
        }
    }


    fun searchPetsAndOwners(query: String) {
        if (query.isBlank()) {
            _vetState.value = _vetState.value.copy(searchResults = emptyList())
            return
        }

        viewModelScope.launch {
            try {
                // Buscar mascotas por nombre
                val petsSnapshot = firestore.collection("pets")
                    .whereEqualTo("active", true)
                    .get()
                    .await()

                val pets = petsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<Pet>()?.copy(id = doc.id)
                }.filter {
                    it.name.contains(query, ignoreCase = true)
                }

                // Buscar dueños por nombre, email o teléfono
                val ownersSnapshot = firestore.collection("users")
                    .get()
                    .await()

                val ownerMatches = ownersSnapshot.documents.filter { doc ->
                    val name = doc.getString("name") ?: ""
                    val email = doc.getString("email") ?: ""
                    val phone = doc.getString("phone") ?: ""

                    name.contains(query, ignoreCase = true) ||
                            email.contains(query, ignoreCase = true) ||
                            phone.contains(query, ignoreCase = true)
                }

                // Si hay dueños que coinciden, buscar sus mascotas
                val ownerIds = ownerMatches.map { it.id }
                val petsByOwner = if (ownerIds.isNotEmpty()) {
                    firestore.collection("pets")
                        .whereIn("ownerId", ownerIds)
                        .whereEqualTo("active", true)
                        .get()
                        .await()
                        .documents.mapNotNull { doc ->
                            doc.toObject<Pet>()?.copy(id = doc.id)
                        }
                } else emptyList()

                // Combinar resultados únicos
                val allPets = (pets + petsByOwner).distinctBy { it.id }

                // Crear resultados de búsqueda con información del dueño
                val searchResults = allPets.map { pet ->
                    val ownerName = pet.ownerId?.let { ownerId ->
                        ownerMatches.find { it.id == ownerId }?.getString("name")
                            ?: ownersSnapshot.documents
                                .find { it.id == ownerId }
                                ?.getString("name")
                    }

                    SearchResult(
                        pet = pet,
                        ownerName = ownerName,
                        matchType = when {
                            pets.contains(pet) -> "Mascota"
                            else -> "Dueño"
                        }
                    )
                }

                _vetState.value = _vetState.value.copy(
                    searchResults = searchResults.take(10)
                )
            } catch (e: Exception) {
                _vetState.value = _vetState.value.copy(
                    error = "Error en búsqueda: ${e.message}"
                )
            }
        }
    }

    fun refreshData() {
        loadTodayAppointments()
        loadWeekStats()
    }

    fun clearError() {
        _vetState.value = _vetState.value.copy(error = null)
    }
}

data class VetState(
    val isVeterinarian: Boolean = false,
    val currentVetId: String? = null,
    val todayAppointments: List<AppointmentDetail> = emptyList(),
    val weeklyStats: WeeklyStats = WeeklyStats(),
    val vetSchedules: List<VetSchedule> = emptyList(),
    val agendaAppointments: List<AppointmentDetail> = emptyList(),
    val weekAppointments: Int = 0,
    val pendingConsultations: Int = 0,
    val alerts: List<String> = emptyList(),
    val searchResults: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class WeeklyStats(
    val consultations: Int = 0,
    val revenue: Double = 0.0
)

data class AppointmentDetail(
    val appointment: Appointment,
    val pet: Pet?,
    val ownerName: String? = null
)

data class SearchResult(
    val pet: Pet,
    val ownerName: String? = null,
    val matchType: String = ""
)