package cl.clinipets.attention.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.attention.model.VetLite
import cl.clinipets.core.common.location.LocationService
import cl.clinipets.core.data.model.common.GeoPoint
import cl.clinipets.core.domain.AttentionRepository
import cl.clinipets.core.domain.PetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RequestUiState(
    val loading: Boolean = true,
    val location: GeoPoint? = null,
    val pets: List<Pair<String, String>> = emptyList(), // (id, nombre)
    val selectedPetId: String? = null,
    val vets: List<VetLite> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AttentionViewModel @Inject constructor(
    private val locationService: LocationService,
    private val petsRepo: PetsRepository,
    private val attentionRepo: AttentionRepository
) : ViewModel() {

    private val _locationFlow = MutableStateFlow<GeoPoint?>(null)
    val locationFlow: StateFlow<GeoPoint?> = _locationFlow

    private val _state = MutableStateFlow(RequestUiState())
    val state: StateFlow<RequestUiState> = _state

    init {
        // Cargar mascotas apenas inicia
        loadPets()
    }

    private fun loadPets() = viewModelScope.launch {
        runCatching { petsRepo.myPets() }
            .onSuccess { pets ->
                // selecciona la primera por defecto si hay
                val first = pets.firstOrNull()?.first
                _state.update { it.copy(pets = pets, selectedPetId = first) }
            }
            .onFailure { e ->
                _state.update { it.copy(error = e.message ?: "No se pudieron cargar tus mascotas") }
            }
        _state.update { it.copy(loading = false) }
    }

    fun onPetSelected(petId: String) {
        _state.update { it.copy(selectedPetId = petId) }
    }

    /** Llama esto cuando ya hay permiso de ubicación. Se suscribe a ubicación + vets en vivo. */
    fun startRealtime() {
        // evita duplicar suscripciones
        if (_locationFlow.value != null) return

        viewModelScope.launch {
            // 1) Ubicación en vivo
            locationService.observeLocation()
                .onEach { gp ->
                    _locationFlow.value = gp
                    _state.update { it.copy(location = gp, loading = false) }
                }
                .catch { e ->
                    _state.update { it.copy(error = e.message ?: "Error de ubicación", loading = false) }
                }
                .launchIn(this)

            // 2) Vets cercanos reaccionando a ese flow
            attentionRepo
                .observeNearbyVets(center = locationFlow.filterNotNull(), radiusMeters = 4000)
                .onEach { vets ->
                    _state.update { it.copy(vets = vets, loading = false, error = null) }
                }
                .catch { e ->
                    _state.update { it.copy(error = e.message ?: "Error cargando veterinarios", loading = false) }
                }
                .launchIn(this)
        }
    }
}
