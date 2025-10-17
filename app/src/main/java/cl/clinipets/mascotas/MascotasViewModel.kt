package cl.clinipets.mascotas

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.clinipets.network.Mascota
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MascotasViewModel(private val repo: MascotasRepository) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _mascotas = MutableStateFlow<List<Mascota>>(emptyList())
    val mascotas: StateFlow<List<Mascota>> = _mascotas

    fun loadMascotas() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val res = repo.misMascotas()
            if (res.isSuccess) {
                _mascotas.value = res.getOrNull() ?: emptyList()
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    fun crearMascota(nombre: String, especie: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.crearMascota(nombre, especie)
            _isLoading.value = false
            if (res.isSuccess) {
                loadMascotas()
                onResult(true)
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
                onResult(false)
            }
        }
    }

    fun eliminarMascota(id: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.eliminarMascota(id)
            _isLoading.value = false
            if (res.isSuccess) {
                loadMascotas()
                onResult(true)
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
                onResult(false)
            }
        }
    }
}

class MascotasViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MascotasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MascotasViewModel(MascotasRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

