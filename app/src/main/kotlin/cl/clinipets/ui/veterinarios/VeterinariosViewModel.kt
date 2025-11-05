package cl.clinipets.ui.veterinarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.ActualizarCatalogoRequest
import cl.clinipets.openapi.models.ActualizarDisponibilidadRequest
import cl.clinipets.openapi.models.CatalogoVeterinario
import cl.clinipets.openapi.models.DisponibilidadSemanal
import cl.clinipets.openapi.models.Procedimiento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class VeterinariosViewModel @Inject constructor(
    private val api: VeterinariosApi
) : ViewModel() {

    private val _procedimientos = MutableStateFlow<List<Procedimiento>>(emptyList())
    val procedimientos = _procedimientos.asStateFlow()

    private val _catalogo = MutableStateFlow<CatalogoVeterinario?>(null)
    val catalogo = _catalogo.asStateFlow()

    private val _disponibilidad = MutableStateFlow<DisponibilidadSemanal?>(null)
    val disponibilidad = _disponibilidad.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun limpiarError() {
        _error.value = null
    }

    fun cargarProcedimientos() = viewModelScope.launch {
        ejecutarConCarga {
            val procedimientos = api.listarProcedimientos()
            if (procedimientos.isSuccessful) {
                _procedimientos.value = procedimientos.body().orEmpty()
                _error.value = null
            } else {
                _error.value =
                    "Error ${procedimientos.code()}: no se pudo obtener el listado de procedimientos"
            }
        }
    }


    fun cargarCatalogo() = viewModelScope.launch {
        ejecutarConCarga {
            val r = api.miCatalogo()
            if (r.isSuccessful) {
                _catalogo.value = r.body()
                _error.value = null
            } else {
                _error.value = "Error ${r.code()}: no se pudo obtener el catálogo"
            }
        }
    }

    fun guardarCatalogo(request: ActualizarCatalogoRequest) = viewModelScope.launch {
        ejecutarConCarga {
            val r = api.actualizarMiCatalogo(request)
            if (r.isSuccessful) {
                _catalogo.value = r.body()
                _error.value = null
            } else {
                _error.value = "Error ${r.code()}: no se pudo guardar el catálogo"
            }
        }
    }

    fun cargarDisponibilidad() = viewModelScope.launch {
        ejecutarConCarga {
            val r = api.miDisponibilidad()
            if (r.isSuccessful) {
                _disponibilidad.value = r.body()
                _error.value = null
            } else {
                _error.value = "Error ${r.code()}: no se pudo obtener la disponibilidad"
            }
        }
    }

    fun guardarDisponibilidad(request: ActualizarDisponibilidadRequest) = viewModelScope.launch {
        ejecutarConCarga {
            val r = api.actualizarMiDisponibilidad(request)
            if (r.isSuccessful) {
                _disponibilidad.value = r.body()
                _error.value = null
            } else {
                _error.value = "Error ${r.code()}: no se pudo guardar la disponibilidad"
            }
        }
    }

    private suspend fun ejecutarConCarga(bloque: suspend () -> Unit) {
        if (_cargando.value) return
        _cargando.value = true
        try {
            withContext(Dispatchers.IO) { bloque() }
        } catch (e: Exception) {
            _error.value = e.message ?: "Error inesperado"
        } finally {
            _cargando.value = false
        }
    }
}
