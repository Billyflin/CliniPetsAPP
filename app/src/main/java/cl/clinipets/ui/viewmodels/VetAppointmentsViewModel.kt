// ui/viewmodels/VetAppointmentsViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class VetAppointmentsViewModel @Inject constructor() : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _vetAppointmentsState = MutableStateFlow(VetAppointmentsState())
    val vetAppointmentsState: StateFlow<VetAppointmentsState> = _vetAppointmentsState

    init {
        loadVetAppointments()
    }

    fun loadVetAppointments() {
        val vetId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _vetAppointmentsState.value = _vetAppointmentsState.value.copy(isLoading = true)

                var query: Query = firestore.collection("appointments")
                    .whereEqualTo("veterinarianId", vetId)

                // Aplicar filtros
                when (_vetAppointmentsState.value.filter) {
                    "TODAY" -> {
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val today = dateFormat.format(Date())
                        query = query.whereEqualTo("date", today)
                    }

                    "PENDING" -> {
                        query = query.whereIn("status", listOf("SCHEDULED", "CONFIRMED"))
                    }
                }

                val snapshot = query
                    .orderBy("dateTime", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val appointments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<VetAppointment>()?.copy(id = doc.id)
                }

                _vetAppointmentsState.value = _vetAppointmentsState.value.copy(
                    appointments = appointments,
                    isLoading = false
                )
            } catch (e: Exception) {
                _vetAppointmentsState.value = _vetAppointmentsState.value.copy(
                    isLoading = false,
                    error = "Error al cargar citas: ${e.message}"
                )
            }
        }
    }

    fun setFilter(filter: String) {
        _vetAppointmentsState.value = _vetAppointmentsState.value.copy(filter = filter)
        loadVetAppointments()
    }

    fun confirmAppointment(appointmentId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("appointments")
                    .document(appointmentId)
                    .update("status", "CONFIRMED")
                    .await()

                // También actualizar en la colección del usuario
                val appointment = firestore.collection("appointments")
                    .document(appointmentId)
                    .get()
                    .await()

                val userId = appointment.getString("userId")
                if (userId != null) {
                    firestore.collection("users")
                        .document(userId)
                        .collection("appointments")
                        .whereEqualTo("appointmentId", appointmentId)
                        .get()
                        .await()
                        .documents
                        .forEach { doc ->
                            doc.reference.update("status", "CONFIRMED").await()
                        }
                }

                loadVetAppointments()
            } catch (e: Exception) {
                _vetAppointmentsState.value = _vetAppointmentsState.value.copy(
                    error = "Error al confirmar cita: ${e.message}"
                )
            }
        }
    }
}

data class VetAppointmentsState(
    val appointments: List<VetAppointment> = emptyList(),
    val filter: String = "TODAY", // TODAY, PENDING, ALL
    val isLoading: Boolean = false,
    val error: String? = null
)

data class VetAppointment(
    val id: String = "",
    val petId: String = "",
    val petName: String = "",
    val userId: String = "",
    val veterinarianId: String = "",
    val veterinarianName: String = "",
    val serviceType: String = "",
    val date: String = "",
    val time: String = "",
    val dateTime: Long = 0,
    val status: String = "",
    val notes: String = "",
    val createdAt: Long = 0
)