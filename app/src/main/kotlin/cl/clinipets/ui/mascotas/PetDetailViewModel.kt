package cl.clinipets.ui.mascotas

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.FichaClinicaControllerApi
import cl.clinipets.openapi.apis.MascotaControllerApi
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.CitaDetalladaResponse
import cl.clinipets.openapi.models.FichaResponse
import cl.clinipets.openapi.models.MascotaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

import cl.clinipets.openapi.models.PesoPunto

data class PetDetailUiState(
    val isLoading: Boolean = true,
    val pet: MascotaResponse? = null,
    val history: List<CitaDetalladaResponse> = emptyList(),
    val clinicalRecords: List<FichaResponse> = emptyList(),
    val weightHistory: List<PesoPunto> = emptyList(),
    val error: String? = null,
    val isDeleted: Boolean = false,
    val isDownloading: Set<UUID> = emptySet()
)

@HiltViewModel
class PetDetailViewModel @Inject constructor(
    private val mascotaApi: MascotaControllerApi,
    private val reservaApi: ReservaControllerApi,
    private val fichaApi: FichaClinicaControllerApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val petId: String? = savedStateHandle["petId"]

    private val _uiState = MutableStateFlow(PetDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        petId?.let { loadPet(it) } ?: _uiState.update { it.copy(isLoading = false, error = "Mascota no encontrada") }
    }

    fun refresh() {
        petId?.let { loadPet(it) }
    }

    fun eliminarMascota() {
        val id = petId ?: return
        val uuid = runCatching { UUID.fromString(id) }.getOrNull() ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = mascotaApi.eliminarMascota(uuid)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, isDeleted = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al eliminar mascota: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido al eliminar") }
            }
        }
    }

    private fun loadPet(id: String) {
        val uuid = runCatching { UUID.fromString(id) }.getOrNull()
        if (uuid == null) {
            _uiState.update { it.copy(isLoading = false, error = "Identificador de mascota inválido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val mascotaResponse = mascotaApi.obtenerMascota(uuid)
                val historyResponse = reservaApi.historialMascota(uuid)
                val clinicalHistoryResponse = fichaApi.obtenerHistorial(uuid)
                val weightHistoryResponse = fichaApi.obtenerGraficoPeso(uuid)

                if (mascotaResponse.isSuccessful) {
                    val pet = mascotaResponse.body()
                    val history = if (historyResponse.isSuccessful) {
                        val body = historyResponse.body()
                        if (body is List<*>) {
                            @Suppress("UNCHECKED_CAST")
                            (body as List<CitaDetalladaResponse>).sortedByDescending { it.fechaHoraInicio }
                        } else {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                    val clinicalHistory = if (clinicalHistoryResponse.isSuccessful) {
                        val body = clinicalHistoryResponse.body()
                        if (body is List<*>) {
                             @Suppress("UNCHECKED_CAST")
                             (body as List<FichaResponse>).sortedByDescending { it.fechaAtencion }
                        } else {
                             emptyList()
                        }
                    } else {
                        emptyList()
                    }
                    
                    val weightHistory = if (weightHistoryResponse.isSuccessful) {
                        weightHistoryResponse.body()?.puntos ?: emptyList()
                    } else {
                        emptyList()
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pet = pet,
                            history = history,
                            clinicalRecords = clinicalHistory,
                            weightHistory = weightHistory
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No pudimos cargar la mascota") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }

    fun descargarFicha(context: Context, fichaId: UUID) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isDownloading = it.isDownloading + fichaId) }
            val showToast: suspend (String) -> Unit = { message ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
            try {
                val response = fichaApi.descargarPdf(fichaId)
                if (response.isSuccessful) {
                    val inputStream = response.body()?.byteStream()
                    if (inputStream != null) {
                        val fichasDir = File(context.cacheDir, "fichas")
                        if (!fichasDir.exists()) fichasDir.mkdirs()
                        val pdfFile = File(fichasDir, "ficha_clinipets_${fichaId}.pdf")
                        inputStream.use { input ->
                            pdfFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            pdfFile
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        withContext(Dispatchers.Main) {
                            try {
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "Instala un visor de PDF para abrir la ficha", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        showToast("No pudimos descargar la ficha")
                    }
                } else {
                    showToast("No pudimos descargar la ficha (${response.code()})")
                }
            } catch (e: Exception) {
                showToast(e.message ?: "Error al descargar la ficha")
            } finally {
                _uiState.update { it.copy(isDownloading = it.isDownloading - fichaId) }
            }
        }
    }
}

private suspend fun FichaClinicaControllerApi.descargarPdf(fichaId: UUID) =
    descargarFichaPdf(fichaId)
