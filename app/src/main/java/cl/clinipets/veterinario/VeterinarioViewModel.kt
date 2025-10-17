package cl.clinipets.veterinario

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.clinipets.network.CreateVeterinarioRequest
import cl.clinipets.network.VeterinarioPerfil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VeterinarioViewModel(private val repo: VeterinarioRepository) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _perfil = MutableStateFlow<VeterinarioPerfil?>(null)
    val perfil: StateFlow<VeterinarioPerfil?> = _perfil

    fun crearVeterinario(nombreCompleto: String, numeroLicencia: String?, modos: List<String>, lat: Double?, lng: Double?, radioKm: Int?, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val req = CreateVeterinarioRequest(nombreCompleto, numeroLicencia, modos, lat, lng, radioKm)
            val res = repo.crearVeterinario(req)
            _isLoading.value = false
            if (res.isSuccess) {
                _perfil.value = res.getOrNull()
                onResult(true)
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
                onResult(false)
            }
        }
    }

    fun cargarMiPerfil() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.miPerfil()
            _isLoading.value = false
            if (res.isSuccess) {
                _perfil.value = res.getOrNull()
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun actualizarPerfil(nombreCompleto: String, numeroLicencia: String?, modos: List<String>, lat: Double?, lng: Double?, radioKm: Int?, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val req = CreateVeterinarioRequest(nombreCompleto, numeroLicencia, modos, lat, lng, radioKm)
            val res = repo.actualizarMiPerfil(req)
            _isLoading.value = false
            if (res.isSuccess) {
                _perfil.value = res.getOrNull()
                onResult(true)
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
                onResult(false)
            }
        }
    }
}

class VeterinarioViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VeterinarioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VeterinarioViewModel(VeterinarioRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

