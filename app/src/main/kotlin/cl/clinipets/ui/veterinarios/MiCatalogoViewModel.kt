package cl.clinipets.ui.veterinarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.ActualizarCatalogoRequest
import cl.clinipets.openapi.models.CatalogoItemRequest
import cl.clinipets.openapi.models.CatalogoVeterinarioResponse
import cl.clinipets.openapi.models.ItemCatalogoResponse
import cl.clinipets.openapi.models.Procedimiento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MiCatalogoViewModel @Inject constructor(
    private val api: VeterinariosApi
) : ViewModel() {

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _miCatalogo = MutableStateFlow<CatalogoVeterinarioResponse?>(null)
    val miCatalogo: StateFlow<CatalogoVeterinarioResponse?> = _miCatalogo.asStateFlow()

    private val _procedimientos = MutableStateFlow<List<Procedimiento>>(emptyList())
    val procedimientos: StateFlow<List<Procedimiento>> = _procedimientos.asStateFlow()

    private val _habilitadoEdits = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _duracionOverrideEdits = MutableStateFlow<Map<String, Int?>>(emptyMap())
    private val _precioOverrideEdits = MutableStateFlow<Map<String, Int?>>(emptyMap())
    private val _success = MutableStateFlow<String?>(null)

    val success: StateFlow<String?> = _success.asStateFlow()
    val habilitadoEdits = _habilitadoEdits.asStateFlow()
    val duracionOverrideEdits = _duracionOverrideEdits.asStateFlow()
    val precioOverrideEdits = _precioOverrideEdits.asStateFlow()

    val itemsCatalogoEfectivos = combine(
        _miCatalogo,
        _habilitadoEdits,
        _duracionOverrideEdits,
        _precioOverrideEdits
    ) { cat, habilEdits, durEdits, precioEdits ->
        val base = cat?.items.orEmpty()
        base.map { item ->
            var out = item
            habilEdits[item.sku]?.let { out = out.copy(habilitado = it) }
            if (durEdits.containsKey(item.sku)) {
                out = out.copy(duracionMinutosOverride = durEdits[item.sku])
            }
            if (precioEdits.containsKey(item.sku)) {
                out = out.copy(precioOverride = precioEdits[item.sku])
            }
            out
        }
    }

    private val _filtroCompatible = MutableStateFlow<Procedimiento.CompatibleCon?>(null)
    val filtroCompatible: StateFlow<Procedimiento.CompatibleCon?> = _filtroCompatible.asStateFlow()

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _seleccionParaAgregar = MutableStateFlow<Set<String>>(emptySet())
    val seleccionParaAgregar: StateFlow<Set<String>> = _seleccionParaAgregar.asStateFlow()

    // --- CORRECCIÓN 1: Convertir isDirty a un StateFlow reactivo ---
    val isDirty = combine(
        _seleccionParaAgregar,
        _habilitadoEdits,
        _duracionOverrideEdits,
        _precioOverrideEdits
    ) { sel, hab, dur, pre ->
        sel.isNotEmpty() || hab.isNotEmpty() || dur.isNotEmpty() || pre.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    // ---------------------------------------------------------------

    val procedimientosFiltrados = combine(_procedimientos, _filtroCompatible, _busqueda) { lista, filtro, q ->
        val query = q.trim().lowercase()
        lista.asSequence()
            .filter { proc ->
                when (filtro) {
                    null -> true
                    Procedimiento.CompatibleCon.PERRO -> proc.compatibleCon.contains(Procedimiento.CompatibleCon.PERRO)
                    Procedimiento.CompatibleCon.GATO -> proc.compatibleCon.contains(Procedimiento.CompatibleCon.GATO)
                }
            }
            .filter { proc ->
                if (query.isEmpty()) true else (
                        proc.nombre.lowercase().contains(query) ||
                                proc.sku.lowercase().contains(query) ||
                                (proc.descripcion ?: "").lowercase().contains(query)
                        )
            }
            .sortedBy { it.nombre }
            .toList()
    }

    fun limpiarError() { _error.value = null; _success.value = null }

    fun setFiltroCompatibleCon(value: Procedimiento.CompatibleCon?) { _filtroCompatible.value = value }
    fun setBusqueda(value: String) { _busqueda.value = value }

    fun toggleSeleccion(sku: String) {
        _seleccionParaAgregar.value = _seleccionParaAgregar.value.toMutableSet().also { set ->
            if (set.contains(sku)) set.remove(sku) else set.add(sku)
        }
    }

    fun limpiarSeleccion() { _seleccionParaAgregar.value = emptySet() }

    fun cargarCatalogoYProcedimientos() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val cat = api.miCatalogo()
                if (cat.isSuccessful) {
                    _miCatalogo.value = cat.body()
                } else {
                    _error.value = "Error obteniendo catálogo (${cat.code()})"
                }
                val procs = api.listarProcedimientos()
                if (procs.isSuccessful) {
                    _procedimientos.value = procs.body().orEmpty()
                } else {
                    _error.value = "Error listando procedimientos (${procs.code()})"
                }
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _cargando.value = false
            }
        }
    }

    fun guardarCatalogo(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _cargando.value = true
            _success.value = null
            _error.value = null // <-- CORRECCIÓN 2: Limpiar error al iniciar guardado
            try {
                val actualesBase = _miCatalogo.value?.items.orEmpty()
                val habilEdits = _habilitadoEdits.value
                val durEdits = _duracionOverrideEdits.value
                val precioEdits = _precioOverrideEdits.value
                val actuales = actualesBase.map { item ->
                    item.copy(
                        habilitado = habilEdits[item.sku] ?: item.habilitado,
                        duracionMinutosOverride = if (durEdits.containsKey(item.sku)) durEdits[item.sku] else item.duracionMinutosOverride,
                        precioOverride = if (precioEdits.containsKey(item.sku)) precioEdits[item.sku] else item.precioOverride
                    )
                }

                val itemsExistentesRequests = actuales.map { it.toRequestItem() }

                val actualesMap = actuales.associateBy { it.sku }
                val nuevosSkus = _seleccionParaAgregar.value.filter { it !in actualesMap }
                val nuevosItems = nuevosSkus.map { sku ->
                    CatalogoItemRequest(
                        sku = sku,
                        habilitado = true,
                        duracionMinutosOverride = null,
                        precioOverride = null,
                    )
                }

                val payload = ActualizarCatalogoRequest(
                    items = (itemsExistentesRequests + nuevosItems)
                )

                val resp = api.actualizarMiCatalogo(payload)
                if (resp.isSuccessful) {
                    _miCatalogo.value = resp.body()
                    _seleccionParaAgregar.value = emptySet()
                    _habilitadoEdits.value = emptyMap()
                    _duracionOverrideEdits.value = emptyMap()
                    _precioOverrideEdits.value = emptyMap()
                    _success.value = "Catálogo guardado"
                    onSuccess?.invoke()
                } else {
                    _error.value = "Error guardando catálogo (${resp.code()})"
                }
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _cargando.value = false
            }
        }
    }

    fun setItemHabilitado(sku: String, habilitado: Boolean) {
        val original = _miCatalogo.value?.items?.find { it.sku == sku }?.habilitado
        _habilitadoEdits.value = _habilitadoEdits.value.toMutableMap().also { map ->
            if (original != null && original == habilitado) {
                map.remove(sku)
            } else {
                map[sku] = habilitado
            }
        }
    }

    fun setItemDuracionOverride(sku: String, minutosOverride: Int?) {
        val original = _miCatalogo.value?.items?.find { it.sku == sku }?.duracionMinutosOverride
        _duracionOverrideEdits.value = _duracionOverrideEdits.value.toMutableMap().also { map ->
            if (original == minutosOverride) {
                map.remove(sku)
            } else {
                map[sku] = minutosOverride
            }
        }
    }

    fun setItemPrecioOverride(sku: String, precioOverride: Int?) {
        val original = _miCatalogo.value?.items?.find { it.sku == sku }?.precioOverride
        _precioOverrideEdits.value = _precioOverrideEdits.value.toMutableMap().also { map ->
            if (original == precioOverride) {
                map.remove(sku)
            } else {
                map[sku] = precioOverride
            }
        }
    }
}

private fun ItemCatalogoResponse.toRequestItem(): CatalogoItemRequest {
    return CatalogoItemRequest(
        sku = this.sku,
        habilitado = this.habilitado,
        duracionMinutosOverride = this.duracionMinutosOverride,
        precioOverride = this.precioOverride,
    )
}