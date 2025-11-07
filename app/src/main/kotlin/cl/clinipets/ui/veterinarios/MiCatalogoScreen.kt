package cl.clinipets.ui.veterinarios

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
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
import cl.clinipets.openapi.models.ItemCatalogoResponse
import cl.clinipets.openapi.models.Procedimiento
import java.text.NumberFormat
import java.util.Locale

// Helper para formatear moneda CLP ($10.000)
private val clpFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL")).apply {
    maximumFractionDigits = 0
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // Añadido ExperimentalLayoutApi
@Composable
fun CatalogoItemRow(
    item: ItemCatalogoResponse,
    onToggleHabilitado: (Boolean) -> Unit,
    onEditDuracion: () -> Unit,
    onEditPrecio: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f), // Devolver el weight para que el Switch no se comprima
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        item.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // --- Lógica de Precio ---
                    val precioEfectivo = item.precioOverride ?: item.precio
                    val precioFormateado = remember(precioEfectivo) { clpFormatter.format(precioEfectivo) }
                    val precioOverrideText = if (item.precioOverride != null) " (personalizado)" else ""
                    Text(
                        "Precio: $precioFormateado$precioOverrideText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.precioOverride != null) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )

                    // --- Lógica de Duración ---
                    val duracionEfectiva = item.duracionMinutosOverride ?: item.duracionMinutos
                    val duracionOverrideText = if (item.duracionMinutosOverride != null) " (personalizado)" else ""
                    Text(
                        "Duración: $duracionEfectiva min$duracionOverrideText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.duracionMinutosOverride != null) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )

                    // --- INICIO DE LA MEJORA ---
                    // Asumiendo que `compatibleCon` y `modosHabilitados` están en tu DTO ItemCatalogoResponse
                    // (tal como están en tu JSON de ejemplo)

                    // 1. Mostrar Compatibilidad
                    Text(
                        "Compatible con: ${item.compatibleCon}", // AÑADIDO
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
                OutlinedButton(onClick = onEditPrecio) { Text("Editar Precio") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onEditDuracion) { Text("Editar Duración") }
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
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = selected, onCheckedChange = { if (enabled) onToggle() }, enabled = enabled
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
                // --- MEJORA: Formato consistente con el RowItem ---
                Text(
                    "Compatible con: ${proc.compatibleCon}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold // Añadido para consistencia
                )
                proc.descripcion?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiCatalogoScreen(
    onBack: () -> Unit,
    vm: MiCatalogoViewModel = hiltViewModel()
) {
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    val catalogo by vm.miCatalogo.collectAsState()
    val seleccion by vm.seleccionParaAgregar.collectAsState()
    val filtro by vm.filtroCompatible.collectAsState()
    val procsFiltrados by vm.procedimientosFiltrados.collectAsState(initial = emptyList())
    val procedimientosAll by vm.procedimientos.collectAsState(initial = emptyList())
    val itemsEfectivos by vm.itemsCatalogoEfectivos.collectAsState(initial = emptyList())

    val showSheet = remember { mutableStateOf(false) }

    // Estados separados para cada diálogo
    var editDuracionSku by remember { mutableStateOf<String?>(null) }
    var editPrecioSku by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.cargarCatalogoYProcedimientos() }

    Scaffold(
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
                }
            )
        }, bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { showSheet.value = true }, enabled = !cargando) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar servicios")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { vm.guardarCatalogo() }, enabled = !cargando) {
                    Text("Guardar cambios")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (error != null) {
                item {
                    AssistChip(onClick = { vm.limpiarError() }, label = { Text(error ?: "") })
                }
            }

            if (cargando && catalogo == null) {
                item {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }

            // Sección de pendientes por agregar (previa al guardado)
            if (seleccion.isNotEmpty()) {
                item {
                    ElevatedCard(
                        Modifier.fillMaxWidth()
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

            // Encabezado para la lista principal
            item {
                Text(
                    "Servicios en mi catálogo",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            // Lista principal de items
            items(
                items = itemsEfectivos,
                key = { it.sku } // Key para mejor performance
            ) { item: ItemCatalogoResponse ->
                CatalogoItemRow(
                    item = item,
                    onToggleHabilitado = { newVal: Boolean ->
                        vm.setItemHabilitado(item.sku, newVal)
                    },
                    onEditDuracion = { editDuracionSku = item.sku },
                    onEditPrecio = { editPrecioSku = item.sku }
                )
            }
        }

        // Bottom sheet para agregar
        if (showSheet.value) {
            ModalBottomSheet(onDismissRequest = { showSheet.value = false }) {
                Column(Modifier.padding(16.dp)) {
                    Text("Agregar desde procedimientos", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = (filtro == null), onClick = { vm.setFiltroCompatibleCon(null) }, label = { Text("Todos") })
                        FilterChip(selected = (filtro == Procedimiento.CompatibleCon.PERRO), onClick = { vm.setFiltroCompatibleCon(Procedimiento.CompatibleCon.PERRO) }, label = { Text("Perro") })
                        FilterChip(selected = (filtro == Procedimiento.CompatibleCon.GATO), onClick = { vm.setFiltroCompatibleCon(Procedimiento.CompatibleCon.GATO) }, label = { Text("Gato") })
                    }
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = vm.busqueda.collectAsState().value,
                        onValueChange = vm::setBusqueda,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Buscar por nombre o SKU") }
                    )
                    Spacer(Modifier.height(8.dp))
                    val existentesSkus = remember(catalogo) { catalogo?.items?.map { it.sku }?.toSet() ?: emptySet() }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp), // Altura fija es ok dentro de un BottomSheet
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
                        OutlinedButton(onClick = { vm.limpiarSeleccion() }) { Text("Limpiar selección") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { showSheet.value = false }) { Text("Listo") }
                    }
                }
            }
        }

        // --- Diálogo para editar DURACIÓN ---
        val itemParaEditarDuracion = itemsEfectivos.find { it.sku == editDuracionSku }
        if (itemParaEditarDuracion != null) {
            var input by remember(editDuracionSku) {
                mutableStateOf(
                    itemParaEditarDuracion.duracionMinutosOverride?.toString() ?: ""
                )
            }
            AlertDialog(
                onDismissRequest = { editDuracionSku = null },
                title = { Text("Duración personalizada (${itemParaEditarDuracion.nombre})") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Define un tiempo en minutos. Déjalo vacío para usar el estándar (${itemParaEditarDuracion.duracionMinutos} min).")
                        OutlinedTextField(
                            value = input,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() }) input = value
                            },
                            label = { Text("Minutos") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Text("Vista previa: ${input.toIntOrNull() ?: itemParaEditarDuracion.duracionMinutos} min efectivos")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.setItemDuracionOverride(itemParaEditarDuracion.sku, input.toIntOrNull())
                        editDuracionSku = null
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                vm.setItemDuracionOverride(itemParaEditarDuracion.sku, null)
                                editDuracionSku = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Quitar override") }
                        TextButton(onClick = { editDuracionSku = null }) { Text("Cancelar") }
                    }
                }
            )
        }

        // --- NUEVO: Diálogo para editar PRECIO ---
        val itemParaEditarPrecio = itemsEfectivos.find { it.sku == editPrecioSku }
        if (itemParaEditarPrecio != null) {
            var input by remember(editPrecioSku) {
                mutableStateOf(
                    itemParaEditarPrecio.precioOverride?.toString() ?: ""
                )
            }
            AlertDialog(
                onDismissRequest = { editPrecioSku = null },
                title = { Text("Precio personalizado (${itemParaEditarPrecio.nombre})") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val precioBaseFormateado = remember(itemParaEditarPrecio.precio) { clpFormatter.format(itemParaEditarPrecio.precio) }
                        Text("Define un precio (CLP). Déjalo vacío para usar el estándar ($precioBaseFormateado).")
                        OutlinedTextField(
                            value = input,
                            onValueChange = { value -> if (value.all { it.isDigit() }) input = value },
                            label = { Text("Precio (CLP)") },
                            prefix = { Text("$ ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        val precioEfectivo = input.toIntOrNull() ?: itemParaEditarPrecio.precio
                        val precioFormateado = remember(precioEfectivo) { clpFormatter.format(precioEfectivo) }
                        Text("Vista previa: $precioFormateado efectivos")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.setItemPrecioOverride(itemParaEditarPrecio.sku, input.toIntOrNull())
                        editPrecioSku = null
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                vm.setItemPrecioOverride(itemParaEditarPrecio.sku, null)
                                editPrecioSku = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Quitar override") }
                        TextButton(onClick = { editPrecioSku = null }) { Text("Cancelar") }
                    }
                }
            )
        }
    }
}
