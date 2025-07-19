// ui/viewmodels/VetScheduleViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class VetScheduleViewModel @Inject constructor() : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _scheduleState = MutableStateFlow(VetScheduleState())
    val scheduleState: StateFlow<VetScheduleState> = _scheduleState

    init {
        loadSchedule()
    }

    private fun loadSchedule() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _scheduleState.value = _scheduleState.value.copy(isLoading = true)

                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val schedule = userDoc.get("schedule") as? Map<String, Any> ?: emptyMap()

                val daySchedules = mutableMapOf<Int, DaySchedule>()

                // Inicializar con valores por defecto
                for (day in 2..7) { // Lunes a Sábado
                    val dayData = schedule[day.toString()] as? Map<String, Any>
                    daySchedules[day] = if (dayData != null) {
                        DaySchedule(
                            isActive = dayData["isActive"] as? Boolean == true,
                            startTime = dayData["startTime"] as? String ?: "09:00",
                            endTime = dayData["endTime"] as? String ?: "18:00"
                        )
                    } else {
                        DaySchedule()
                    }
                }

                _scheduleState.value = _scheduleState.value.copy(
                    schedule = daySchedules,
                    isLoading = false
                )
            } catch (e: Exception) {
                _scheduleState.value = _scheduleState.value.copy(
                    isLoading = false,
                    error = "Error al cargar horarios: ${e.message}"
                )
            }
        }
    }

    fun toggleDay(dayNumber: Int, isActive: Boolean) {
        val currentSchedule = _scheduleState.value.schedule.toMutableMap()
        currentSchedule[dayNumber] = currentSchedule[dayNumber]?.copy(isActive = isActive)
            ?: DaySchedule(isActive = isActive)
        _scheduleState.value = _scheduleState.value.copy(schedule = currentSchedule)
    }

    fun updateStartTime(dayNumber: Int, time: String) {
        val currentSchedule = _scheduleState.value.schedule.toMutableMap()
        currentSchedule[dayNumber] =
            currentSchedule[dayNumber]?.copy(startTime = time) ?: DaySchedule(startTime = time)
        _scheduleState.value = _scheduleState.value.copy(schedule = currentSchedule)
    }

    fun updateEndTime(dayNumber: Int, time: String) {
        val currentSchedule = _scheduleState.value.schedule.toMutableMap()
        currentSchedule[dayNumber] =
            currentSchedule[dayNumber]?.copy(endTime = time) ?: DaySchedule(endTime = time)
        _scheduleState.value = _scheduleState.value.copy(schedule = currentSchedule)
    }

    fun saveSchedule() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _scheduleState.value = _scheduleState.value.copy(isLoading = true)

                // Convertir el schedule a un formato que Firestore pueda guardar
                val scheduleMap = mutableMapOf<String, Any>()
                _scheduleState.value.schedule.forEach { (day, schedule) ->
                    scheduleMap[day.toString()] = mapOf(
                        "isActive" to schedule.isActive,
                        "startTime" to schedule.startTime,
                        "endTime" to schedule.endTime
                    )
                }

                firestore.collection("users")
                    .document(userId)
                    .update("schedule", scheduleMap)
                    .await()

                _scheduleState.value = _scheduleState.value.copy(
                    isLoading = false,
                    isSaved = true
                )
            } catch (e: Exception) {
                _scheduleState.value = _scheduleState.value.copy(
                    isLoading = false,
                    error = "Error al guardar horarios: ${e.message}"
                )
            }
        }
    }
}

data class VetScheduleState(
    val schedule: Map<Int, DaySchedule> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

data class DaySchedule(
    val isActive: Boolean = false,
    val startTime: String = "09:00",
    val endTime: String = "18:00"
)