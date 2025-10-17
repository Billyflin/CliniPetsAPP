package cl.clinipets.juntas

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.clinipets.network.Junta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JuntasViewModel(private val repo: JuntasRepository) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _junta = MutableStateFlow<Junta?>(null)
    val junta: StateFlow<Junta?> = _junta

    fun crearJunta(reservaId: String, onResult: (Boolean, Junta?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.crearJunta(reservaId)
            _isLoading.value = false
            if (res.isSuccess) {
                _junta.value = res.getOrNull()
                onResult(true, res.getOrNull())
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
                onResult(false, null)
            }
        }
    }

    fun obtenerJunta(juntaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.obtenerJunta(juntaId)
            _isLoading.value = false
            if (res.isSuccess) {
                _junta.value = res.getOrNull()
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun cambiarEstado(juntaId: String, estado: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.cambiarEstado(juntaId, estado)
            _isLoading.value = false
            if (res.isSuccess) {
                obtenerJunta(juntaId)
                onResult(true)
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
                onResult(false)
            }
        }
    }

    fun actualizarUbicacion(juntaId: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.actualizarUbicacion(juntaId, lat, lng)
            _isLoading.value = false
            if (!res.isSuccess) _error.value = res.exceptionOrNull()?.localizedMessage
        }
    }

    fun finalizarJunta(juntaId: String, notas: String?, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.finalizarJunta(juntaId, notas)
            _isLoading.value = false
            if (res.isSuccess) {
                obtenerJunta(juntaId)
                onResult(true)
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
                onResult(false)
            }
        }
    }
}

class JuntasViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JuntasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JuntasViewModel(JuntasRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

