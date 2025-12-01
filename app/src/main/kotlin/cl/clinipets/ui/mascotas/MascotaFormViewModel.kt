package cl.clinipets.ui.mascotas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.models.MascotaCreateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

import java.time.ZoneOffset

@HiltViewModel
class MascotaFormViewModel @Inject constructor(
    private val mascotaApi: MascotaControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<MascotaFormUiState>(MascotaFormUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun crearMascota(nombre: String, especie: MascotaCreateRequest.Especie, peso: Double, fechaNacimiento: LocalDate) {
        viewModelScope.launch {
            _uiState.value = MascotaFormUiState.Loading
            try {
                val request = MascotaCreateRequest(
                    nombre = nombre,
                    especie = especie,
                    pesoActual = BigDecimal.valueOf(peso),
                    fechaNacimiento = fechaNacimiento.atStartOfDay().atOffset(ZoneOffset.UTC)
                )
                val response = mascotaApi.crearMascota(request)
                if (response.isSuccessful) {
                    _uiState.value = MascotaFormUiState.Success
                } else {
                    _uiState.value = MascotaFormUiState.Error("Error al crear mascota: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = MascotaFormUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class MascotaFormUiState {
    object Idle : MascotaFormUiState()
    object Loading : MascotaFormUiState()
    object Success : MascotaFormUiState()
    data class Error(val message: String) : MascotaFormUiState()
}
