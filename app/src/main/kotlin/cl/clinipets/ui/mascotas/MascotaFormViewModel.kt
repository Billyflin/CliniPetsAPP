package cl.clinipets.ui.mascotas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.apis.MaestrosControllerApi
import cl.clinipets.openapi.models.MascotaCreateRequest
import cl.clinipets.openapi.models.MascotaResponse
import cl.clinipets.openapi.models.MascotaUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class MascotaFormUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val pet: MascotaResponse? = null,
    val isEdit: Boolean = false,
    val razasDisponibles: List<String> = emptyList()
)

@HiltViewModel
class MascotaFormViewModel @Inject constructor(
    private val mascotaApi: MascotaControllerApi,
    private val maestrosApi: MaestrosControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MascotaFormUiState())
    val uiState = _uiState.asStateFlow()

    fun cargarRazas(especie: MascotaCreateRequest.Especie) {
        viewModelScope.launch {
            try {
                // Mapear el enum local al enum del endpoint si es necesario, o usar string
                // El endpoint espera MaestrosControllerApi.EspecieListarRazas
                val especieApi = MaestrosControllerApi.EspecieListarRazas.valueOf(especie.name)
                val response = maestrosApi.listarRazas(especieApi)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(razasDisponibles = response.body() ?: emptyList()) }
                }
            } catch (e: Exception) {
                // Silently fail or log
                _uiState.update { it.copy(razasDisponibles = emptyList()) }
            }
        }
    }

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

    fun guardarMascota(
        id: String?,
        nombre: String,
        especie: MascotaCreateRequest.Especie,
        raza: String,
        sexo: MascotaCreateRequest.Sexo,
        esterilizado: Boolean,
        temperamento: MascotaCreateRequest.Temperamento,
        chip: String?
    ) {
        if (id != null) {
            actualizarMascota(id, nombre, raza, sexo, esterilizado, temperamento, chip)
        } else {
            crearMascota(nombre, especie, raza, sexo, esterilizado, temperamento, chip)
        }
    }

    private fun crearMascota(
        nombre: String,
        especie: MascotaCreateRequest.Especie,
        raza: String,
        sexo: MascotaCreateRequest.Sexo,
        esterilizado: Boolean,
        temperamento: MascotaCreateRequest.Temperamento,
        chip: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = false) }
            try {
                val request = MascotaCreateRequest(
                    nombre = nombre,
                    especie = especie,
                    pesoActual = 0.0,
                    fechaNacimiento = LocalDate.now(),
                    raza = raza,
                    sexo = sexo,
                    esterilizado = esterilizado,
                    temperamento = temperamento,
                    chipIdentificador = chip.takeIf { !it.isNullOrBlank() }
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

    private fun actualizarMascota(
        id: String,
        nombre: String,
        raza: String,
        sexo: MascotaCreateRequest.Sexo,
        esterilizado: Boolean,
        temperamento: MascotaCreateRequest.Temperamento,
        chip: String?
    ) {
        val uuid = runCatching { UUID.fromString(id) }.getOrNull()
        if (uuid == null) {
            _uiState.update { it.copy(error = "ID de mascota inválido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = false) }
            try {
                // Map CreateRequest Enums to UpdateRequest Enums
                val updateSexo = MascotaUpdateRequest.Sexo.valueOf(sexo.name)
                val updateTemperamento = MascotaUpdateRequest.Temperamento.valueOf(temperamento.name)

                val request = MascotaUpdateRequest(
                    nombre = nombre,
                    pesoActual = 0.0,
                    raza = raza,
                    sexo = updateSexo,
                    esterilizado = esterilizado,
                    temperamento = updateTemperamento,
                    chipIdentificador = chip.takeIf { !it.isNullOrBlank() }
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
