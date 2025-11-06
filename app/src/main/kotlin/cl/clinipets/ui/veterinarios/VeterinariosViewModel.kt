package cl.clinipets.ui.veterinarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.VeterinariosApi
import cl.clinipets.openapi.models.ActualizarCatalogoRequest
import cl.clinipets.openapi.models.ActualizarDisponibilidadRequest
import cl.clinipets.openapi.models.BloqueHorario
import cl.clinipets.openapi.models.BloqueHorarioDto
import cl.clinipets.openapi.models.CatalogoItemRequest
import cl.clinipets.openapi.models.CatalogoVeterinarioResponse
import cl.clinipets.openapi.models.DisponibilidadSemanal
import cl.clinipets.openapi.models.ItemCatalogoResponse
import cl.clinipets.openapi.models.Procedimiento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VeterinariosViewModel @Inject constructor(
    private val api: VeterinariosApi
) : ViewModel() {

    // Estado general
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun limpiarError() { _error.value = null }

    // Disponibilidad (para compatibilidad con MiDisponibilidadScreen) + edición
    private val _disponibilidad = MutableStateFlow<DisponibilidadSemanal?>(null)
    val disponibilidad: StateFlow<DisponibilidadSemanal?> = _disponibilidad.asStateFlow()

    private val _bloquesEdit = MutableStateFlow<List<BloqueHorarioDto>>(emptyList())
    val bloquesEdit: StateFlow<List<BloqueHorarioDto>> = _bloquesEdit.asStateFlow()

    fun cargarDisponibilidad() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val resp = api.miDisponibilidad()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _disponibilidad.value = body
                    // sincronizar edición
                    _bloquesEdit.value = body?.bloques?.map { it.toDto() } ?: emptyList()
                } else {
                    _error.value = "Error obteniendo disponibilidad (${resp.code()})"
                }
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _cargando.value = false
            }
        }
    }

    fun resetEdicionDisponibilidad() {
        _bloquesEdit.value = _disponibilidad.value?.bloques?.map { it.toDto() } ?: emptyList()
    }

    fun addBloque(dia: BloqueHorarioDto.Dia, inicio: String = "09:00", fin: String = "18:00", habilitado: Boolean = true) {
        _bloquesEdit.value = _bloquesEdit.value + BloqueHorarioDto(dia = dia, inicio = inicio, fin = fin, habilitado = habilitado)
    }

    fun removeBloque(indexGlobal: Int) {
        _bloquesEdit.value = _bloquesEdit.value.toMutableList().also { if (indexGlobal in it.indices) it.removeAt(indexGlobal) }
    }

    fun setInicio(indexGlobal: Int, value: String) {
        _bloquesEdit.value = _bloquesEdit.value.toMutableList().also { list ->
            if (indexGlobal in list.indices) list[indexGlobal] = list[indexGlobal].copy(inicio = value)
        }
    }

    fun setFin(indexGlobal: Int, value: String) {
        _bloquesEdit.value = _bloquesEdit.value.toMutableList().also { list ->
            if (indexGlobal in list.indices) list[indexGlobal] = list[indexGlobal].copy(fin = value)
        }
    }

    fun setHabilitado(indexGlobal: Int, habilitado: Boolean) {
        _bloquesEdit.value = _bloquesEdit.value.toMutableList().also { list ->
            if (indexGlobal in list.indices) list[indexGlobal] = list[indexGlobal].copy(habilitado = habilitado)
        }
    }

    fun setDia(indexGlobal: Int, dia: BloqueHorarioDto.Dia) {
        _bloquesEdit.value = _bloquesEdit.value.toMutableList().also { list ->
            if (indexGlobal in list.indices) list[indexGlobal] = list[indexGlobal].copy(dia = dia)
        }
    }

    fun removeBloquesDia(dia: BloqueHorarioDto.Dia) {
        _bloquesEdit.value = _bloquesEdit.value.filterNot { it.dia == dia }
    }

    private fun validarBloques(bloques: List<BloqueHorarioDto>): String? {
        val hhmm = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        for ((i, b) in bloques.withIndex()) {
            if (!hhmm.matches(b.inicio)) return "Bloque #${i + 1}: hora inicio inválida"
            if (!hhmm.matches(b.fin)) return "Bloque #${i + 1}: hora fin inválida"
            val ini = toMin(b.inicio)
            val end = toMin(b.fin)
            if (ini >= end) return "Bloque #${i + 1}: inicio debe ser menor que fin"
        }
        return null
    }

    private fun toMin(hhmm: String): Int {
        val p = hhmm.split(":")
        return p[0].toInt() * 60 + p[1].toInt()
    }

    fun guardarDisponibilidad(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val bloques = _bloquesEdit.value
                validarBloques(bloques)?.let { msg ->
                    _error.value = msg
                    _cargando.value = false
                    return@launch
                }
                val resp = api.actualizarMiDisponibilidad(ActualizarDisponibilidadRequest(bloques = bloques))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    _disponibilidad.value = body
                    _bloquesEdit.value = body?.bloques?.map { it.toDto() } ?: emptyList()
                    onSuccess?.invoke()
                } else {
                    _error.value = "Error guardando disponibilidad (${resp.code()})"
                }
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _cargando.value = false
            }
        }
    }

    // Catálogo
    private val _miCatalogo = MutableStateFlow<CatalogoVeterinarioResponse?>(null)
    val miCatalogo: StateFlow<CatalogoVeterinarioResponse?> = _miCatalogo.asStateFlow()

    private val _procedimientos = MutableStateFlow<List<Procedimiento>>(emptyList())
    val procedimientos: StateFlow<List<Procedimiento>> = _procedimientos.asStateFlow()

    // Ediciones locales de habilitado por SKU
    private val _habilitadoEdits = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    // Ediciones locales de duración override por SKU (null para borrar)
    private val _duracionOverrideEdits = MutableStateFlow<Map<String, Int?>>(emptyMap())
    // Ediciones locales de precio override por SKU (null para borrar)
    private val _precioOverrideEdits = MutableStateFlow<Map<String, Int?>>(emptyMap())

    // Lista de items del catálogo con ediciones aplicadas
    val itemsCatalogoEfectivos = combine(_miCatalogo, _habilitadoEdits, _duracionOverrideEdits, _precioOverrideEdits) { cat, habilEdits, durEdits, precioEdits ->
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

    fun setItemHabilitado(sku: String, habilitado: Boolean) {
        val original = _miCatalogo.value?.items?.find { it.sku == sku }?.habilitado
        _habilitadoEdits.value = _habilitadoEdits.value.toMutableMap().also { map ->
            if (original != null && original == habilitado) {
                map.remove(sku) // sin cambios vs original
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

    // Filtros/agregar
    private val _filtroCompatible = MutableStateFlow<Procedimiento.CompatibleCon?>(null) // null = Todos
    val filtroCompatible: StateFlow<Procedimiento.CompatibleCon?> = _filtroCompatible.asStateFlow()

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _seleccionParaAgregar = MutableStateFlow<Set<String>>(emptySet()) // sku seleccionados
    val seleccionParaAgregar: StateFlow<Set<String>> = _seleccionParaAgregar.asStateFlow()

    // Lista filtrada de procedimientos segun filtro de compatibleCon y busqueda
    val procedimientosFiltrados = combine(_procedimientos, _filtroCompatible, _busqueda) { lista, filtro, q ->
        val query = q.trim().lowercase()
        lista.asSequence()
            .filter { proc ->
                when (filtro) {
                    null -> true // todos
                    Procedimiento.CompatibleCon.PERRO -> proc.compatibleCon == Procedimiento.CompatibleCon.PERRO || proc.compatibleCon == Procedimiento.CompatibleCon.AMBOS
                    Procedimiento.CompatibleCon.GATO -> proc.compatibleCon == Procedimiento.CompatibleCon.GATO || proc.compatibleCon == Procedimiento.CompatibleCon.AMBOS
                    Procedimiento.CompatibleCon.AMBOS -> proc.compatibleCon == Procedimiento.CompatibleCon.AMBOS
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

    fun setFiltroCompatibleCon(value: Procedimiento.CompatibleCon?) { _filtroCompatible.value = value }
    fun setBusqueda(value: String) { _busqueda.value = value }

    fun toggleSeleccion(sku: String) {
        _seleccionParaAgregar.value = _seleccionParaAgregar.value.toMutableSet().also { set ->
            if (set.contains(sku)) set.remove(sku) else set.add(sku)
        }
    }

    fun limpiarSeleccion() { _seleccionParaAgregar.value = emptySet() }

    fun cargarCatalogo() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val resp = api.miCatalogo()
                if (resp.isSuccessful) {
                    _miCatalogo.value = resp.body()
                } else {
                    _error.value = "Error obteniendo catálogo (${resp.code()})"
                }
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _cargando.value = false
            }
        }
    }

    fun cargarProcedimientos() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val resp = api.listarProcedimientos()
                if (resp.isSuccessful) {
                    _procedimientos.value = resp.body().orEmpty()
                } else {
                    _error.value = "Error listando procedimientos (${resp.code()})"
                }
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                _cargando.value = false
            }
        }
    }

    fun cargarCatalogoYProcedimientos() {
        // secuencial simple para evitar múltiples banderas de carga
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
            try {
                // Construir items actuales del catálogo aplicando ediciones locales (habilitado, duración y precio)
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

                // Agregar nuevos seleccionados que no existan
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

    fun puedeAgregarBloque(dia: BloqueHorarioDto.Dia, inicio: String, fin: String): String? {
        val hhmm = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        if (!hhmm.matches(inicio)) return "Hora de inicio inválida"
        if (!hhmm.matches(fin)) return "Hora de fin inválida"
        if (toMin(inicio) >= toMin(fin)) return "El inicio debe ser menor que el fin"
        return null
    }

    fun addBloqueCustom(dia: BloqueHorarioDto.Dia, inicio: String, fin: String, habilitado: Boolean) {
        _bloquesEdit.value = _bloquesEdit.value + BloqueHorarioDto(dia = dia, inicio = inicio, fin = fin, habilitado = habilitado)
    }
}

// Mappers utilitarios
private fun ItemCatalogoResponse.toRequestItem(): CatalogoItemRequest {
    return CatalogoItemRequest(
        sku = this.sku,
        habilitado = this.habilitado,
        duracionMinutosOverride = this.duracionMinutosOverride,
        precioOverride = this.precioOverride,
    )
}

private fun BloqueHorario.toDto(): BloqueHorarioDto = BloqueHorarioDto(
    dia = BloqueHorarioDto.Dia.valueOf(this.dia.name),
    inicio = this.inicio,
    fin = this.fin,
    habilitado = this.habilitado
)
