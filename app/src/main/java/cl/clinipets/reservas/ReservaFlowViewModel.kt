package cl.clinipets.reservas

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.clinipets.descubrimiento.DescubrimientoRepository
import cl.clinipets.mascotas.MascotasRepository
import cl.clinipets.network.Bloque
import cl.clinipets.network.Mascota
import cl.clinipets.network.Procedimiento
import cl.clinipets.network.Reserva
import cl.clinipets.network.VetItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ReservaFlowViewModel(
    private val mascotasRepo: MascotasRepository,
    private val descRepo: DescubrimientoRepository,
    private val reservasRepo: ReservasRepository
) : ViewModel() {
    // state
    private val _mascotas = MutableStateFlow<List<Mascota>>(emptyList())
    val mascotas: StateFlow<List<Mascota>> = _mascotas

    private val _procedimientos = MutableStateFlow<List<Procedimiento>>(emptyList())
    val procedimientos: StateFlow<List<Procedimiento>> = _procedimientos

    private val _veterinarios = MutableStateFlow<List<VetItem>>(emptyList())
    val veterinarios: StateFlow<List<VetItem>> = _veterinarios

    private val _bloques = MutableStateFlow<List<Bloque>>(emptyList())
    val bloques: StateFlow<List<Bloque>> = _bloques

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // selections
    var selectedMascota: Mascota? = null
    var selectedProcedimiento: Procedimiento? = null
    var selectedVet: VetItem? = null
    var selectedFecha: String? = null
    var selectedInicioIso: String? = null
    var selectedModo: String = "CLINICA"
    var direccionAtencion: String? = null
    var notas: String? = null

    fun cargarMascotas() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val res = mascotasRepo.misMascotas()
            _isLoading.value = false
            if (res.isSuccess) _mascotas.value = res.getOrNull() ?: emptyList() else _error.value = res.exceptionOrNull()?.localizedMessage
        }
    }

    fun cargarProcedimientos(especie: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val res = descRepo.procedimientos(especie, null)
            _isLoading.value = false
            if (res.isSuccess) _procedimientos.value = res.getOrNull() ?: emptyList() else _error.value = res.exceptionOrNull()?.localizedMessage
        }
    }

    fun cargarVeterinarios(especie: String?, procedimientoSku: String?, lat: Double?, lng: Double?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val res = descRepo.veterinarios(lat, lng, 10, null, especie, procedimientoSku, true)
            _isLoading.value = false
            if (res.isSuccess) _veterinarios.value = res.getOrNull() ?: emptyList() else _error.value = res.exceptionOrNull()?.localizedMessage
        }
    }

    fun cargarBloques(vetId: String, fecha: String) {
        selectedFecha = fecha
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val res = reservasRepo.disponibilidad(vetId, fecha)
            _isLoading.value = false
            if (res.isSuccess) {
                _bloques.value = res.getOrNull() ?: emptyList()
            } else {
                _error.value = res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun crearReserva(onResult: (Boolean, Reserva?) -> Unit) {
        val mascota = selectedMascota ?: return onResult(false, null)
        val mascotaId = mascota.id ?: return onResult(false, null)
        val procedimiento = selectedProcedimiento ?: return onResult(false, null)
        val vet = selectedVet ?: return onResult(false, null)
        val inicio = selectedInicioIso ?: return onResult(false, null)

        viewModelScope.launch {
            _isLoading.value = true
            val res = reservasRepo.crearReserva(
                mascotaId = mascotaId,
                veterinarioId = vet.id,
                procedimientoSku = procedimiento.sku,
                inicio = inicio,
                modo = selectedModo,
                direccionAtencion = if (selectedModo == "DOMICILIO") direccionAtencion else null,
                notas = notas
            )
            _isLoading.value = false
            if (res.isSuccess) {
                onResult(true, res.getOrNull())
            } else {
                val ex = res.exceptionOrNull()
                if (ex is HttpException && ex.code() == 400) {
                    // Bloque no disponible: recargar disponibilidad para la fecha actual
                    val fecha = selectedFecha
                    if (!fecha.isNullOrBlank()) {
                        cargarBloques(vet.id, fecha)
                        _error.value = "Ese bloque ya no está disponible. Actualizamos la disponibilidad."
                    } else {
                        _error.value = "Ese bloque ya no está disponible. Elige otra hora."
                    }
                } else {
                    _error.value = ex?.localizedMessage
                }
                onResult(false, null)
            }
        }
    }
}

class ReservaFlowViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReservaFlowViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReservaFlowViewModel(
                MascotasRepository(context),
                cl.clinipets.descubrimiento.DescubrimientoRepository(context),
                ReservasRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
