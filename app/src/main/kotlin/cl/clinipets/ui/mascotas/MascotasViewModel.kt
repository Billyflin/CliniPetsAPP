package cl.clinipets.ui.mascotas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.models.ActualizarMascota
import cl.clinipets.openapi.models.CrearMascota
import cl.clinipets.openapi.models.ListarRazasRequest
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.Raza
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MascotasViewModel @Inject constructor(
    private val api: MascotasApi
) : ViewModel() {
    private val _items = MutableStateFlow<List<Mascota>>(emptyList())
    val items = _items.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _seleccionada = MutableStateFlow<Mascota?>(null)
    val seleccionada = _seleccionada.asStateFlow()

    private val _razas = MutableStateFlow<List<Raza>>(emptyList())
    val razas = _razas.asStateFlow()

    fun cargar() = viewModelScope.launch {
        ejecutarConCarga {
            val r = api.listarMisMascotas()
            if (r.isSuccessful) {
                _items.value = r.body().orEmpty()
                _error.value = null
            } else {
                _error.value = "Error ${r.code()}: no se pudo obtener la lista"
            }
        }
    }

    fun refrescar() = cargar()

    fun detalle(id: UUID) = viewModelScope.launch {
        ejecutarConCarga {
            val r = api.detalleMascota(id)
            if (r.isSuccessful) {
                val body = r.body()
                if (body != null) {
                    _seleccionada.value = body
                    val lista = _items.value.toMutableList()
                    val idx = lista.indexOfFirst { it.id == body.id }
                    if (idx >= 0) {
                        lista[idx] = body
                    } else {
                        lista.add(body)
                    }
                    _items.value = lista
                    _error.value = null
                } else {
                    _error.value = "Respuesta vacía al obtener detalle"
                }
            } else {
                _error.value = "Error ${r.code()}: no se pudo obtener el detalle"
            }
        }
    }

    fun limpiarSeleccion() {
        _seleccionada.value = null
        _razas.value = emptyList()
    }

    fun crear(datos: CrearMascota) = viewModelScope.launch {
        ejecutarConCarga {
            val r = api.crearMascota(datos)
            if (r.isSuccessful) {
                val creada = r.body()
                if (creada != null) {
                    _items.value = _items.value + creada
                    _error.value = null
                } else {
                    _error.value = "Mascota creada sin contenido devuelto"
                }
            } else {
                _error.value = "Error ${r.code()}: no se pudo crear la mascota"
            }
        }
    }

    fun actualizar(id: UUID, datos: ActualizarMascota) = viewModelScope.launch {
        ejecutarConCarga {
            val r = api.actualizarMascota(id, datos)
            if (r.isSuccessful) {
                val actualizada = r.body()
                if (actualizada != null) {
                    val lista = _items.value.toMutableList()
                    val idx = lista.indexOfFirst { it.id == actualizada.id }
                    if (idx >= 0) {
                        lista[idx] = actualizada
                        _items.value = lista
                    } else {
                        _items.value = _items.value + actualizada
                    }
                    if (_seleccionada.value?.id == actualizada.id) {
                        _seleccionada.value = actualizada
                    }
                    _error.value = null
                } else {
                    _error.value = "Mascota actualizada sin contenido devuelto"
                }
            } else {
                _error.value = "Error ${r.code()}: no se pudo actualizar la mascota"
            }
        }
    }

    fun eliminar(id: UUID) = viewModelScope.launch {
        ejecutarConCarga {
            val r = api.eliminarMascota(id)
            if (r.isSuccessful) {
                _items.value = _items.value.filterNot { it.id == id }
                if (_seleccionada.value?.id == id) {
                    _seleccionada.value = null
                }
                _error.value = null
            } else {
                _error.value = "Error ${r.code()}: no se pudo eliminar la mascota"
            }
        }
    }

    fun cargarRazas(especie: ListarRazasRequest.Especie) {
        viewModelScope.launch {
            try {
                val r = api.buscarRazas(ListarRazasRequest(especie = especie))
                if (r.isSuccessful) {
                    _razas.value = r.body().orEmpty()
                } else {
                    _error.value = "Error ${r.code()}: no se pudieron cargar las razas"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error de red inesperado"
            }
        }
    }

    fun limpiarError() { _error.value = null }

    private suspend fun ejecutarConCarga(bloque: suspend () -> Unit) {
        if (_cargando.value) return
        _cargando.value = true
        _error.value = null
        try {
            withContext(Dispatchers.IO) { bloque() }
        } catch (e: Exception) {
            _error.value = e.message ?: "Error inesperado"
        } finally {
            _cargando.value = false
        }
    }
}