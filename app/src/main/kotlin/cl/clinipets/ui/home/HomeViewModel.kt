package cl.clinipets.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.AgendaApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val agendaApi: AgendaApi // Reservado para futuro endpoint de "próximas reservas"
) : ViewModel() {
    data class UiState(
        val loading: Boolean = false,
        val error: String? = null,
        val citas: List<String> = emptyList()
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    fun cargarCitas(force: Boolean = false) {
        if (_ui.value.loading) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                // Simulación: hasta que exista un GET de reservas futuras.
                withContext(Dispatchers.IO) { delay(250) }
                val demo = listOf(
                    "Vacuna Flow - FlowPet - 2025-11-12 10:30",
                    "Control general - Luna - 2025-11-18 16:00"
                )
                _ui.value = UiState(loading = false, citas = demo)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Error cargando citas")
            }
        }
    }

    fun reintentar() = cargarCitas(force = true)
}

