package cl.clinipets.disponibilidad

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.clinipets.network.Bloque
import cl.clinipets.veterinario.VeterinarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DisponibilidadViewModel(
    private val repo: DisponibilidadRepository,
    private val vetRepo: VeterinarioRepository
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _bloques = MutableStateFlow<List<Bloque>>(emptyList())
    val bloques: StateFlow<List<Bloque>> = _bloques

    private val _vetId = MutableStateFlow<String?>(null)
    val vetId: StateFlow<String?> = _vetId

    fun cargarMiVetId() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = vetRepo.miPerfil()
            _isLoading.value = false
            if (res.isSuccess) {
                _vetId.value = res.getOrNull()?.id
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun consultar(fecha: String) {
        val id = _vetId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val r = repo.disponibilidad(id, fecha)
            _isLoading.value = false
            if (r.isSuccess) {
                _bloques.value = r.getOrNull() ?: emptyList()
            } else {
                _error.value = r.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun crearRegla(diaSemana: Int, horaInicio: String, horaFin: String, onDone: (Boolean) -> Unit = {}) {
        val id = _vetId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val r = repo.crearRegla(id, diaSemana, horaInicio, horaFin)
            _isLoading.value = false
            onDone(r.isSuccess)
            if (!r.isSuccess) _error.value = r.exceptionOrNull()?.localizedMessage
        }
    }

    fun crearExcepcion(fecha: String, tipo: String, horaInicio: String?, horaFin: String?, motivo: String?, onDone: (Boolean) -> Unit = {}) {
        val id = _vetId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val r = repo.crearExcepcion(id, fecha, tipo, horaInicio, horaFin, motivo)
            _isLoading.value = false
            onDone(r.isSuccess)
            if (!r.isSuccess) _error.value = r.exceptionOrNull()?.localizedMessage
        }
    }
}

class DisponibilidadViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DisponibilidadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DisponibilidadViewModel(DisponibilidadRepository(context), VeterinarioRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

