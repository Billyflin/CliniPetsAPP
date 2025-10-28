package cl.clinipets.descubrimiento

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.clinipets.network.Oferta
import cl.clinipets.network.Procedimiento
import cl.clinipets.network.VetItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DescubrimientoViewModel(private val repo: DescubrimientoRepository) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _veterinarios = MutableStateFlow<List<VetItem>>(emptyList())
    val veterinarios: StateFlow<List<VetItem>> = _veterinarios

    private val _procedimientos = MutableStateFlow<List<Procedimiento>>(emptyList())
    val procedimientos: StateFlow<List<Procedimiento>> = _procedimientos

    private val _ofertas = MutableStateFlow<List<Oferta>>(emptyList())
    val ofertas: StateFlow<List<Oferta>> = _ofertas

    fun buscarVeterinarios(lat: Double? = null, lng: Double? = null, radioKm: Int? = 10, especie: String? = null, procedimientoSku: String? = null, abiertoAhora: Boolean? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val res = repo.veterinarios(lat, lng, radioKm, null, especie, procedimientoSku, abiertoAhora)
            if (res.isSuccess) {
                _veterinarios.value = res.getOrNull() ?: emptyList()
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    fun cargarProcedimientos(especie: String? = null, q: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.procedimientos(especie, q)
            if (res.isSuccess) {
                _procedimientos.value = res.getOrNull() ?: emptyList()
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }

    fun buscarOfertas(especie: String? = null, procedimientoSku: String? = null, vetId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.ofertas(especie, procedimientoSku, vetId)
            if (res.isSuccess) {
                _ofertas.value = res.getOrNull() ?: emptyList()
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
            }
            _isLoading.value = false
        }
    }
}

class DescubrimientoViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DescubrimientoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DescubrimientoViewModel(DescubrimientoRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
