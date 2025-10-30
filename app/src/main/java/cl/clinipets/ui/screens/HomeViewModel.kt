package cl.clinipets.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.auth.TokenRepository
import cl.clinipets.openapi.apis.DefaultApi
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.MeResponse
import cl.clinipets.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val defaultApi: DefaultApi,
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _meResponseState = MutableStateFlow<Result<MeResponse>>(Result.Loading)
    val meResponseState: StateFlow<Result<MeResponse>> = _meResponseState

    private val _petsState = MutableStateFlow<Result<List<Mascota>>>(Result.Loading)
    val petsState: StateFlow<Result<List<Mascota>>> = _petsState

    private val _logoutState = MutableStateFlow<Result<Unit>>(Result.Success(Unit))
    val logoutState: StateFlow<Result<Unit>> = _logoutState

    init {
        fetchUserData()
    }

    fun fetchUserData() {
        viewModelScope.launch {
            _meResponseState.value = Result.Loading
            _petsState.value = Result.Loading
            try {
                val meResponse = defaultApi.apiAuthMeGet()
                if (meResponse.isSuccessful) {
                    meResponse.body()?.let { _meResponseState.value = Result.Success(it) }
                        ?: run { _meResponseState.value = Result.Error(Exception("MeResponse is null")) }
                } else {
                    _meResponseState.value = Result.Error(Exception(meResponse.errorBody()?.string() ?: "Unknown error"))
                }

                val petsResponse = defaultApi.listarMisMascotas()
                if (petsResponse.isSuccessful) {
                    petsResponse.body()?.let { _petsState.value = Result.Success(it) }
                        ?: run { _petsState.value = Result.Error(Exception("Pets response is null")) }
                } else {
                    _petsState.value = Result.Error(Exception(petsResponse.errorBody()?.string() ?: "Unknown error"))
                }

            } catch (e: Exception) {
                _meResponseState.value = Result.Error(e)
                _petsState.value = Result.Error(e)
            }
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            _logoutState.value = Result.Loading
            try {
                val response = defaultApi.apiAuthLogoutPost()
                if (response.isSuccessful) {
                    tokenRepository.clear()
                    _logoutState.value = Result.Success(Unit)
                    onLogoutSuccess()
                } else {
                    _logoutState.value = Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _logoutState.value = Result.Error(e)
            }
        }
    }
}
