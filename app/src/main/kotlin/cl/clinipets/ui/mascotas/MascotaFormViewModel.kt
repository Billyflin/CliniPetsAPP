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
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

import java.time.ZoneOffset
import java.util.UUID

@HiltViewModel
class MascotaFormViewModel @Inject constructor(
    private val mascotaApi: MascotaControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MascotaFormUiState())
    val uiState = _uiState.asStateFlow()

    private var editingPetId: UUID? = null

    fun cargarMascota(id: String) {
        val uuid = runCatching { UUID.fromString(id) }.getOrNull() ?: return
        editingPetId = uuid
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isEdit = true)
            try {
                val response = mascotaApi.obtenerMascota(uuid)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        pet = response.body(),
                        isEdit = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No pudimos cargar la mascota (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun crearMascota(
        nombre: String,
        especie: MascotaCreateRequest.Especie,
        peso: Double,
        fechaNacimiento: LocalDate
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = if (editingPetId == null) {
                    val request = MascotaCreateRequest(
                        nombre = nombre,
                        especie = especie,
                        pesoActual = BigDecimal.valueOf(peso),
                        fechaNacimiento = fechaNacimiento.atStartOfDay().atOffset(ZoneOffset.UTC)
                    )
                    mascotaApi.crearMascota(request)
                } else {
                    val request = MascotaUpdateRequest(
                        nombre = nombre,
                        pesoActual = BigDecimal.valueOf(peso)
                    )
                    mascotaApi.actualizarMascota(editingPetId!!, request)
                }

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isLoading = false, success = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (editingPetId == null) {
                            "Error al crear mascota: ${response.code()}"
                        } else {
                            "Error al actualizar mascota: ${response.code()}"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error desconocido")
            }
        }
    }
}

data class MascotaFormUiState(
    val isLoading: Boolean = false,
    val pet: MascotaResponse? = null,
    val isEdit: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)
