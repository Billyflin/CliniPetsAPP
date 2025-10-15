package cl.clinipets.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.domain.discovery.DiscoveryRepository
import cl.clinipets.domain.discovery.VetNearby
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MapUiState(
    val isLoading: Boolean = false,
    val vets: List<VetNearby> = emptyList(),
    val error: String? = null,
    val selected: VetNearby? = null,
    val centerLat: Double = -33.45,
    val centerLon: Double = -70.66,
    val radioMeters: Int = 3000,
)

class MapDiscoverViewModel(
    private val repo: DiscoveryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state

    fun setRadio(meters: Int) {
        _state.value = _state.value.copy(radioMeters = meters)
    }

    fun setCenter(lat: Double, lon: Double) {
        _state.value = _state.value.copy(centerLat = lat, centerLon = lon)
    }

    fun select(v: VetNearby?) {
        _state.value = _state.value.copy(selected = v)
    }

    fun buscar(lat: Double, lon: Double, radio: Int = _state.value.radioMeters,
               procedimientoId: String? = null,
               abiertoAhora: Boolean? = null,
               conStock: Boolean? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching {
                repo.buscarVets(lat, lon, radio, procedimientoId, abiertoAhora, conStock)
            }.onSuccess { list ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    vets = list,
                    error = null,
                    centerLat = lat,
                    centerLon = lon
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Error desconocido")
            }
        }
    }
}

