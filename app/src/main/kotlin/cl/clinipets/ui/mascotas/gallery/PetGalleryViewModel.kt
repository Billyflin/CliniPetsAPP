package cl.clinipets.ui.mascotas.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.di.ApiModule
import cl.clinipets.openapi.apis.GaleriaControllerApi
import cl.clinipets.openapi.models.MediaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class GalleryUiState(
    val isLoading: Boolean = false,
    val mediaList: List<MediaResponse> = emptyList(),
    val error: String? = null,
    val isUploading: Boolean = false
)

@HiltViewModel
class PetGalleryViewModel @Inject constructor(
    private val galeriaApi: GaleriaControllerApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState = _uiState.asStateFlow()

    fun loadGallery(petId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = galeriaApi.listarGaleria(UUID.fromString(petId))
                if (response.isSuccessful) {
                    val baseUrl = ApiModule.resolveBaseUrl()
                    val media = response.body()
                        ?.map { item ->
                            val fullUrl = item.url.toFullUrl(baseUrl)
                            item.copy(url = fullUrl)
                        }
                        ?: emptyList()
                    _uiState.update { it.copy(isLoading = false, mediaList = media) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Error al cargar galería: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }

    fun uploadImage(petId: String, file: File, titulo: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }
            try {
                // Prepare file part
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = galeriaApi.uploadFile(
                    mascotaId = UUID.fromString(petId),
                    file = body,
                    titulo = titulo,
                    tipo = GaleriaControllerApi.TipoUploadFile.IMAGE
                )

                if (response.isSuccessful) {
                    // Refresh gallery
                    loadGallery(petId)
                    _uiState.update { it.copy(isUploading = false) }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.update { it.copy(isUploading = false, error = "Error al subir imagen: $errorBody") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }
}

private fun String.toFullUrl(baseUrl: String): String {
    if (startsWith("http://") || startsWith("https://")) return this
    val sanitizedBase = baseUrl.trimEnd('/')
    val sanitizedPath = trimStart('/')
    return "$sanitizedBase/$sanitizedPath"
}
