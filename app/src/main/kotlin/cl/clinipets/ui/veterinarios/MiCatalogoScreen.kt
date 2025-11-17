package cl.clinipets.ui.veterinarios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.ItemCatalogo
import cl.clinipets.openapi.models.Procedimiento
import java.text.NumberFormat
import java.util.Locale
import java.util.logging.Logger

private val clpFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL")).apply {
    maximumFractionDigits = 0
}

private fun formatCompatItem(set: Set<Procedimiento.CompatibleCon>): String {
    val perro = set.contains(Procedimiento.CompatibleCon.PERRO)
    val gato = set.contains(Procedimiento.CompatibleCon.GATO)
    Logger.getLogger("CatalogoItemRow").info("formatCompatItem: perro=$perro, gato=$gato y el set=$set")
    return when {
        perro && gato -> "Perros y Gatos"
        perro -> "Perros"
        gato -> "Gatos"
        else -> "—"
    }
}

private val fieldShape = RoundedCornerShape(16.dp)
private val chipShape = RoundedCornerShape(12.dp)
private val buttonShape = RoundedCornerShape(24.dp)
private val itemCardShape = RoundedCornerShape(28.dp)
private val pendingCardShape = RoundedCornerShape(28.dp)
private val addCardShape = RoundedCornerShape(16.dp)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatalogoItemRow(
    item: ItemCatalogo,
    onToggleHabilitado: (Boolean) -> Unit,
    onEditDuracion: () -> Unit,
    onEditPrecio: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = itemCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        item.procedimiento.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    val precioEfectivo = item.precioOverride ?: item.procedimiento.precio
                    val precioFormateado = remember(precioEfectivo) { clpFormatter.format(precioEfectivo) }
                    val precioOverrideText = if (item.precioOverride != null) " (personalizado)" else ""
                    Text(
                        "Precio: $precioFormateado$precioOverrideText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.precioOverride != null) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )

                    val duracionEfectiva = item.duracionMinutosOverride ?: item.procedimiento.duracionMinutos
                    val duracionOverrideText = if (item.duracionMinutosOverride != null) " (personalizado)" else ""
                    Text(
                        "Duración: $duracionEfectiva min$duracionOverrideText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.duracionMinutosOverride != null) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )

                    Text(
                        "Compatible con: ${formatCompatItem(item.procedimiento.compatibleCon)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Switch(
                    checked = item.habilitado,
                    onCheckedChange = onToggleHabilitado,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onEditPrecio, shape = buttonShape) { Text("Editar Precio") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onEditDuracion, shape = buttonShape) { Text("Editar Duración") }
            }
        }
    }
}

@Composable
fun ProcedimientoSelectableRow(
    proc: Procedimiento,
    selected: Boolean,
    enabled: Boolean,
    badge: String? = null,
    onToggle: () -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle),
        shape = addCardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = selected, onCheckedChange = null, enabled = enabled
            )
            Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(proc.nombre, fontWeight = FontWeight.SemiBold)
                    if (badge != null) {
                        Text(
                            badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                Text("SKU: ${proc.sku}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Compatible con: ${formatCompatItem(proc.compatibleCon)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                proc.descripcion?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MiCatalogoScreen(
    onBack: () -> Unit,
    vm: MiCatalogoViewModel = hiltViewModel()
) {
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    val success by vm.success.collectAsState()
    val catalogo by vm.miCatalogo.collectAsState()
    val seleccion by vm.seleccionParaAgregar.collectAsState()
    val filtro by vm.filtroCompatible.collectAsState()
    val procsFiltrados by vm.procedimientosFiltrados.collectAsState(initial = emptyList())
    val procedimientosAll by vm.procedimientos.collectAsState(initial = emptyList())
    val itemsEfectivos by vm.itemsCatalogoEfectivos.collectAsState(initial = emptyList())
    val isDirty by vm.isDirty.collectAsState()

    val showSheet = remember { mutableStateOf(false) }

    var editDuracionSku by remember { mutableStateOf<String?>(null) }
    var editPrecioSku by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.cargarCatalogoYProcedimientos() }

    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(
                message = error!!,
                duration = SnackbarDuration.Long
            )
            vm.limpiarError()
        }
    }

    LaunchedEffect(success) {
        if (success != null) {
            snackbarHostState.showSnackbar(message = success!!)
            vm.limpiarError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        topBar = {
            TopAppBar(
                title = { Text("Mi catálogo") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                }, actions = {
                    IconButton(
                        onClick = { vm.cargarCatalogoYProcedimientos() }, enabled = !cargando
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { showSheet.value = true }, enabled = !cargando, shape = buttonShape) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar servicios")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { vm.guardarCatalogo() },
                        enabled = !cargando && isDirty,
                        shape = buttonShape
                    ) {
                        Text("Guardar cambios")
                    }
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                if (cargando && catalogo == null) {
                    item {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }

                if (seleccion.isNotEmpty()) {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = pendingCardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Pendientes por agregar (${seleccion.size})",
                                    fontWeight = FontWeight.Bold
                                )
                                val mapProc = procedimientosAll.associateBy { it.sku }
                                seleccion.sorted().forEach { sku ->
                                    val p = mapProc[sku]
                                    Text(text = p?.let { "${it.nombre} (${it.sku})" } ?: sku, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Servicios en mi catálogo",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                items(
                    items = itemsEfectivos,
                    key = { it.procedimiento.sku }
                ) { item: ItemCatalogo ->
                    CatalogoItemRow(
                        item = item,
                        onToggleHabilitado = { newVal: Boolean ->
                            vm.setItemHabilitado(item.procedimiento.sku, newVal)
                        },
                        onEditDuracion = { editDuracionSku = item.procedimiento.sku },
                        onEditPrecio = { editPrecioSku = item.procedimiento.sku }
                    )
                }
            }
        }

        if (showSheet.value) {
            ModalBottomSheet(onDismissRequest = { showSheet.value = false }) {
                Column(Modifier.padding(16.dp)) {
                    Text("Agregar desde procedimientos", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = (filtro == null), onClick = { vm.setFiltroCompatibleCon(null) }, label = { Text("Todos") }, shape = chipShape)
                        FilterChip(selected = (filtro == Procedimiento.CompatibleCon.PERRO), onClick = { vm.setFiltroCompatibleCon(Procedimiento.CompatibleCon.PERRO) }, label = { Text("Perro") }, shape = chipShape)
                        FilterChip(selected = (filtro == Procedimiento.CompatibleCon.GATO), onClick = { vm.setFiltroCompatibleCon(Procedimiento.CompatibleCon.GATO) }, label = { Text("Gato") }, shape = chipShape)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = vm.busqueda.collectAsState().value,
                        onValueChange = vm::setBusqueda,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Buscar por nombre o SKU") },
                        shape = fieldShape
                    )
                    Spacer(Modifier.height(8.dp))
                    val existentesSkus = remember(catalogo) { catalogo?.items?.map { it.procedimiento.sku }?.toSet() ?: emptySet() }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(procsFiltrados, key = { it.sku }) { proc ->
                            val yaEnCatalogo = existentesSkus.contains(proc.sku)
                            ProcedimientoSelectableRow(
                                proc = proc,
                                selected = yaEnCatalogo || seleccion.contains(proc.sku),
                                enabled = !yaEnCatalogo,
                                badge = if (yaEnCatalogo) "Ya en catálogo" else null,
                                onToggle = { vm.toggleSeleccion(proc.sku) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { vm.limpiarSeleccion() }, shape = buttonShape) { Text("Limpiar selección") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { showSheet.value = false }, shape = buttonShape) { Text("Listo") }
                    }
                }
            }
        }

        val itemParaEditarDuracion = itemsEfectivos.find { it.procedimiento.sku == editDuracionSku }
        if (itemParaEditarDuracion != null) {
            var input by remember(editDuracionSku) {
                mutableStateOf(
                    itemParaEditarDuracion.duracionMinutosOverride?.toString() ?: ""
                )
            }
            AlertDialog(
                onDismissRequest = { editDuracionSku = null },
                title = { Text("Duración personalizada (${itemParaEditarDuracion.procedimiento.nombre})") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Define un tiempo en minutos. Déjalo vacío para usar el estándar (${itemParaEditarDuracion.procedimiento.duracionMinutos} min).")
                        OutlinedTextField(
                            value = input,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() }) input = value
                            },
                            label = { Text("Minutos") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = fieldShape
                        )
                        Text("Vista previa: ${input.toIntOrNull() ?: itemParaEditarDuracion.procedimiento.duracionMinutos} min efectivos")

                        OutlinedButton(
                            onClick = {
                                vm.setItemDuracionOverride(itemParaEditarDuracion.procedimiento.sku, null)
                                editDuracionSku = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.padding(top = 8.dp),
                            shape = buttonShape
                        ) { Text("Reestablecer a valor estándar") }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            vm.setItemDuracionOverride(itemParaEditarDuracion.procedimiento.sku, input.toIntOrNull())
                            editDuracionSku = null
                        },
                        shape = buttonShape
                    ) { Text("Guardar") }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { editDuracionSku = null },
                        shape = buttonShape
                    ) { Text("Cancelar") }
                }
            )
        }

        val itemParaEditarPrecio = itemsEfectivos.find { it.procedimiento.sku == editPrecioSku }
        if (itemParaEditarPrecio != null) {
            var input by remember(editPrecioSku) {
                mutableStateOf(
                    itemParaEditarPrecio.precioOverride?.toString() ?: ""
                )
            }
            AlertDialog(
                onDismissRequest = { editPrecioSku = null },
                title = { Text("Precio personalizado (${itemParaEditarPrecio.procedimiento.nombre})") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val precioBaseFormateado = remember(itemParaEditarPrecio.procedimiento.precio) { clpFormatter.format(itemParaEditarPrecio.procedimiento.precio) }
                        Text("Define un precio (CLP). Déjalo vacío para usar el estándar ($precioBaseFormateado).")
                        OutlinedTextField(
                            value = input,
                            onValueChange = { value -> if (value.all { it.isDigit() }) input = value },
                            label = { Text("Precio (CLP)") },
                            prefix = { Text("$ ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = fieldShape
                        )
                        val precioEfectivo = input.toIntOrNull() ?: itemParaEditarPrecio.procedimiento.precio
                        val precioFormateado = remember(precioEfectivo) { clpFormatter.format(precioEfectivo) }
                        Text("Vista previa: $precioFormateado efectivos")

                        OutlinedButton(
                            onClick = {
                                vm.setItemPrecioOverride(itemParaEditarPrecio.procedimiento.sku, null)
                                editPrecioSku = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.padding(top = 8.dp),
                            shape = buttonShape
                        ) { Text("Reestablecer a valor estándar") }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            vm.setItemPrecioOverride(itemParaEditarPrecio.procedimiento.sku, input.toIntOrNull())
                            editPrecioSku = null
                        },
                        shape = buttonShape
                    ) { Text("Guardar") }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { editPrecioSku = null },
                        shape = buttonShape
                    ) { Text("Cancelar") }
                }
            )
        }
    }
}