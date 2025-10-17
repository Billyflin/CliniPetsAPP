package cl.clinipets.agenda

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.clinipets.network.Reserva
import cl.clinipets.veterinario.VeterinarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AgendaVetViewModel(
    private val repo: AgendaVetRepository,
    private val vetRepo: VeterinarioRepository
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _reservas = MutableStateFlow<List<Reserva>>(emptyList())
    val reservas: StateFlow<List<Reserva>> = _reservas

    private val _vetId = MutableStateFlow<String?>(null)
    val vetId: StateFlow<String?> = _vetId

    fun cargarVetYAgenda() {
        viewModelScope.launch {
            _isLoading.value = true
            val vetRes = vetRepo.miPerfil()
            if (vetRes.isSuccess) {
                val id = vetRes.getOrNull()?.id
                _vetId.value = id
                if (id != null) {
                    val res = repo.reservasPorVet(id)
                    if (res.isSuccess) {
                        _reservas.value = res.getOrNull() ?: emptyList()
                    } else {
                        _error.value = res.exceptionOrNull()?.localizedMessage
                    }
                }
            } else {
                _error.value = vetRes.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    fun cambiarEstado(reservaId: String, nuevoEstado: String, motivo: String? = null, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val r = repo.cambiarEstado(reservaId, nuevoEstado, motivo)
            _isLoading.value = false
            if (r.isSuccess) {
                cargarVetYAgenda()
                onDone(true)
            } else {
                _error.value = r.exceptionOrNull()?.localizedMessage
                onDone(false)
            }
        }
    }
}

class AgendaVetViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgendaVetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AgendaVetViewModel(AgendaVetRepository(context), VeterinarioRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

