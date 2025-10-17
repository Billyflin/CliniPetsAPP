package cl.clinipets.clinica

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClinicaViewModel(private val repo: ClinicaRepository) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _diagnosticos = MutableStateFlow<List<String>>(emptyList())
    val diagnosticos: StateFlow<List<String>> = _diagnosticos

    private val _tratamientos = MutableStateFlow<List<String>>(emptyList())
    val tratamientos: StateFlow<List<String>> = _tratamientos

    fun sugerenciasDiagnostico(sintomas: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val res = repo.sugerenciasDiagnostico(sintomas)
            _isLoading.value = false
            if (res.isSuccess) _diagnosticos.value = res.getOrNull() ?: emptyList() else _error.value = res.exceptionOrNull()?.localizedMessage
        }
    }

    fun sugerenciasTratamiento(diagnostico: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val res = repo.sugerenciasTratamiento(diagnostico)
            _isLoading.value = false
            if (res.isSuccess) _tratamientos.value = res.getOrNull() ?: emptyList() else _error.value = res.exceptionOrNull()?.localizedMessage
        }
    }
}

class ClinicaViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClinicaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClinicaViewModel(ClinicaRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

