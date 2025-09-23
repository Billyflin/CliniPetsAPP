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

    // Flags para controlar el "loader" y evitar parpadeos
    private var petsLoaded = false
    private var locationReady = false          // primera ubicación o error
    private var vetsFirstEmission = false      // primera lista de vets o error
    private var startedRealtime = false

    init {
        loadPets()
    }

    private fun recomputeLoading() {
        val loadingNow = !(petsLoaded && locationReady && vetsFirstEmission)
        _state.update { it.copy(loading = loadingNow) }
    }

    private fun loadPets() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching { petsRepo.myPets() }
            .onSuccess { pets ->
                petsLoaded = true
                val first = pets.firstOrNull()?.first
                _state.update { it.copy(pets = pets, selectedPetId = first) }
                recomputeLoading()
            }
            .onFailure { e ->
                petsLoaded = true
                _state.update { it.copy(error = e.message ?: "No se pudieron cargar tus mascotas") }
                recomputeLoading()
            }
    }

    fun onPetSelected(petId: String) {
        _state.update { it.copy(selectedPetId = petId) }
    }

    /** Llamar cuando ya hay permiso de ubicación. */
    fun startRealtime() {
        if (startedRealtime) return
        startedRealtime = true

        // Asegura loader visible mientras llegan los primeros datos
        _state.update { it.copy(loading = true) }

        // 1) Ubicación en vivo
        locationService.observeLocation()
            .onEach { gp ->
                _locationFlow.value = gp
                if (!locationReady) {
                    locationReady = true
                }
                _state.update { it.copy(location = gp) }
                recomputeLoading()
            }
            .catch { e ->
                if (!locationReady) locationReady = true
                _state.update { it.copy(error = e.message ?: "Error de ubicación") }
                recomputeLoading()
            }
            .launchIn(viewModelScope)

        // 2) Vets cercanos reaccionando a ese flow
        attentionRepo
            .observeNearbyVets(center = locationFlow.filterNotNull(), radiusMeters = 4000)
            .onEach { vets ->
                if (!vetsFirstEmission) vetsFirstEmission = true
                _state.update { it.copy(vets = vets, error = null) }
                recomputeLoading()
            }
            .catch { e ->
                if (!vetsFirstEmission) vetsFirstEmission = true
                _state.update { it.copy(error = e.message ?: "Error cargando veterinarios") }
                recomputeLoading()
            }
            .launchIn(viewModelScope)
    }
}
