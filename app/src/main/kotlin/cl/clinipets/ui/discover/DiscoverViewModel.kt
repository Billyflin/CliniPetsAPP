package cl.clinipets.ui.discover

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.DescubrimientoApi
import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.DiscoveryRequest
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.Procedimiento
import cl.clinipets.openapi.models.UbicacionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val isLoading: Boolean = false,
    val isLoadingFilters: Boolean = false,
    val error: String? = null,
    val mascotas: List<Mascota> = emptyList(),
    val procedimientos: List<Procedimiento> = emptyList(),
    val searchResults: List<DiscoveryRequest> = emptyList()
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val descubrimientoApi: DescubrimientoApi,
    private val mascotasApi: MascotasApi,
    private val veterinariosApi: VeterinariosApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadFilterOptions()
    }

    /**
     * Carga las mascotas del usuario y los procedimientos disponibles
     * para poblar los menús del filtro.
     */
    fun loadFilterOptions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFilters = true) }
            try {
                // Ejecutar en paralelo (si es seguro) o secuencial
                val mascotasResp = mascotasApi.listarMisMascotas()
                val procsResp = veterinariosApi.listarProcedimientos()

                _uiState.update {
                    it.copy(
                        mascotas = mascotasResp.body().orEmpty(),
                        procedimientos = procsResp.body().orEmpty(),
                        isLoadingFilters = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Error al cargar filtros", isLoadingFilters = false)
                }
            }
        }
    }

    /**
     * Ejecuta la búsqueda principal de descubrimiento.
     */
    fun executeSearch(
        mascota: Mascota,
        procedimiento: Procedimiento,
        modo: DiscoveryRequest.ModoAtencion,
        userLocation: Location
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val request = DiscoveryRequest(
                    mascotaId = mascota.id!!,
                    servicioSku = procedimiento.sku,
                    modoAtencion = modo,
                    ubicacionUsuario = UbicacionDto(
                        latitud = userLocation.latitude,
                        longitud = userLocation.longitude
                    )
                )

                val response = descubrimientoApi.buscarDescubrimiento(request)

                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            searchResults = response.body() as List<DiscoveryRequest>
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error ${response.code()}: ${response.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de red") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}