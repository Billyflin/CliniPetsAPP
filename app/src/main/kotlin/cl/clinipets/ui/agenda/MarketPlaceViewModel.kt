package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.Procedimiento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class MarketPlaceViewModel @Inject constructor(
    private val mascotasApi: MascotasApi,
    private val veterinariosApi: VeterinariosApi
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isSubmitting: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
        val mascotas: List<Mascota> = emptyList(),
        val procedimientos: List<Procedimiento> = emptyList()
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /**
     * Carga la información necesaria para que el cliente pueda publicar una solicitud:
     * mascotas registradas y procedimientos disponibles.
     */
    fun loadFormData(forceRefresh: Boolean = false) {
        val current = _ui.value
        if (!forceRefresh && current.mascotas.isNotEmpty() && current.procedimientos.isNotEmpty()) return
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null, successMessage = null) }
            try {
                val mascotasResp = mascotasApi.listarMisMascotas()
                val procedimientosResp = veterinariosApi.listarProcedimientos()

                val mascotas = mascotasResp.takeBodyOrThrow("Error listando mascotas")
                val procedimientos = procedimientosResp.takeBodyOrThrow("Error listando procedimientos")

                _ui.update { it.copy(isLoading = false, mascotas = mascotas, procedimientos = procedimientos) }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar datos") }
            }
        }
    }



    fun clearStatus() { _ui.update { it.copy(error = null, successMessage = null) } }

    // Extensión usada en loadFormData
    private fun <T> Response<List<T>>.takeBodyOrThrow(defaultMessage: String): List<T> {
        return if (isSuccessful) body().orEmpty() else {
            val msg = errorBody()?.string()?.takeIf { it.isNotBlank() } ?: defaultMessage
            throw IllegalStateException(msg)
        }
    }
}
