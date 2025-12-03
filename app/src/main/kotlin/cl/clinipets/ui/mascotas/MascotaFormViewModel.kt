package cl.clinipets.ui.mascotas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.models.MascotaCreateRequest
import cl.clinipets.openapi.models.MascotaResponse
import cl.clinipets.openapi.models.MascotaUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class MascotaFormUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val pet: MascotaResponse? = null,
    val isEdit: Boolean = false
)

@HiltViewModel
class MascotaFormViewModel @Inject constructor(
    private val mascotaApi: MascotaControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MascotaFormUiState())
    val uiState = _uiState.asStateFlow()

    fun cargarMascota(id: String) {
        val uuid = runCatching { UUID.fromString(id) }.getOrNull()
        if (uuid == null) {
            _uiState.update { it.copy(error = "ID de mascota inválido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEdit = true) }
            try {
                val response = mascotaApi.obtenerMascota(uuid)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, pet = response.body()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al cargar mascota") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }

    fun guardarMascota(id: String?, nombre: String, especie: MascotaCreateRequest.Especie, peso: Double, fechaNacimiento: LocalDate) {
        if (id != null) {
            actualizarMascota(id, nombre, peso)
        } else {
            crearMascota(nombre, especie, peso, fechaNacimiento)
        }
    }

    private fun crearMascota(nombre: String, especie: MascotaCreateRequest.Especie, peso: Double, fechaNacimiento: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = false) }
            try {
                val request = MascotaCreateRequest(
                    nombre = nombre,
                    especie = especie,
                    pesoActual = BigDecimal.valueOf(peso),
                    fechaNacimiento = fechaNacimiento
                )
                val response = mascotaApi.crearMascota(request)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, success = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }

    private fun actualizarMascota(id: String, nombre: String, peso: Double) {
        val uuid = runCatching { UUID.fromString(id) }.getOrNull()
        if (uuid == null) {
            _uiState.update { it.copy(error = "ID de mascota inválido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = false) }
            try {
                val request = MascotaUpdateRequest(
                    nombre = nombre,
                    pesoActual = BigDecimal.valueOf(peso)
                )
                val response = mascotaApi.actualizarMascota(uuid, request)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, success = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al actualizar: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }
}
