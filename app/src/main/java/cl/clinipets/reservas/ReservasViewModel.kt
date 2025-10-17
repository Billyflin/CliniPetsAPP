package cl.clinipets.reservas

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.clinipets.network.Reserva
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReservasViewModel(private val repo: ReservasRepository) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _reservas = MutableStateFlow<List<Reserva>>(emptyList())
    val reservas: StateFlow<List<Reserva>> = _reservas

    fun loadMisReservas() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val res = repo.misReservas()
            if (res.isSuccess) {
                _reservas.value = res.getOrNull() ?: emptyList()
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    fun crearReserva(mascotaId: String, veterinarioId: String, procedimientoSku: String, inicio: String, modo: String, direccion: String?, notas: String?, onResult: (Boolean, Reserva?) -> Unit = {_,_->}) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.crearReserva(mascotaId, veterinarioId, procedimientoSku, inicio, modo, direccion, notas)
            _isLoading.value = false
            if (res.isSuccess) {
                loadMisReservas()
                onResult(true, res.getOrNull())
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
                onResult(false, null)
            }
        }
    }
}

class ReservasViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReservasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReservasViewModel(ReservasRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

