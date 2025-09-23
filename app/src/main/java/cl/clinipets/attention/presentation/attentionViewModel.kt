package cl.clinipets.attention.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.attention.model.AttentionRequest
import cl.clinipets.core.common.location.LocationService
import cl.clinipets.core.data.model.common.GeoPoint
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RequestUiState(
    val loading: Boolean = false,
    val location: GeoPoint? = null,
    val error: String? = null
)

@HiltViewModel
class AttentionViewModel @Inject constructor(
    private val location: LocationService,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(RequestUiState())
    val state: StateFlow<RequestUiState> = _state

    fun loadLocation() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { location.getLastKnownLocation() }
                .onSuccess { gp ->
                    _state.value = _state.value.copy(loading = false, location = gp)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = e.message ?: "No se pudo obtener ubicación"
                    )
                }
        }
    }

    fun createRequest(petId: String?, note: String?, onCreated: (AttentionRequest) -> Unit) {
        val loc = _state.value.location ?: return
        val uid = auth.currentUser?.uid ?: return
        val req =
            AttentionRequest(tutorUid = uid, petId = petId, desiredLocation = loc, note = note)
        // TODO: aquí llama a repo para persistir (Firestore) y luego:
        onCreated(req)
    }
}
