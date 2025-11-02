package cl.clinipets.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VeterinarianOnboardingViewModel @Inject constructor(
    private val veterinariosApi: VeterinariosApi
) : ViewModel() {

    data class UiState(
        val submitting: Boolean = false,
        val successMessage: String? = null,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    fun submit(request: RegistrarVeterinarioRequest) {
        if (_ui.value.submitting) return
        viewModelScope.launch {
            _ui.update { UiState(submitting = true) }
            val result = runCatching { veterinariosApi.registrar(request) }
            result.onSuccess { response ->
                if (response.isSuccessful) {
                    _ui.update {
                        UiState(
                            submitting = false,
                            successMessage = "Tu perfil está pendiente de verificación."
                        )
                    }
                } else {
                    _ui.update {
                        UiState(
                            submitting = false,
                            error = "HTTP ${response.code()}: ${response.errorBody()?.string()}"
                        )
                    }
                }
            }.onFailure { throwable ->
                _ui.update {
                    UiState(
                        submitting = false,
                        error = throwable.message ?: "Error inesperado"
                    )
                }
            }
        }
    }

    fun resetMessages() {
        _ui.update { it.copy(successMessage = null, error = null) }
    }
}
