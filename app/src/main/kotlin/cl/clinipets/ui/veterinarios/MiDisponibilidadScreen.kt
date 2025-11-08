package cl.clinipets.ui.veterinarios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.BloqueHorarioDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiDisponibilidadScreen(
    onBack: () -> Unit,
    vm: MiDisponibilidadViewModel = hiltViewModel()
) {
    val uiState by vm.miDisponibilidadState.collectAsState()
    val globalError by vm.error.collectAsState()
    val bloques = uiState.bloquesEditables
    val isBusy = uiState.isLoading || uiState.isSaving
    val errorMessage = uiState.errorMessage ?: globalError
    val showAddForDay = remember { mutableStateOf<BloqueHorarioDto.Dia?>(null) }

    LaunchedEffect(Unit) { vm.cargarDisponibilidad() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi disponibilidad") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
                actions = {
                    IconButton(onClick = { vm.cargarDisponibilidad() }, enabled = !uiState.isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                }
            )
        },
        bottomBar = {
            // Alineamos los botones a la derecha (End)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.End, // Cambiado
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { vm.resetEdicionDisponibilidad() },
                    enabled = uiState.isDirty && !isBusy
                ) { Text("Restablecer cambios") }
                Spacer(Modifier.width(8.dp)) // Añadido spacer
                Button(onClick = { vm.guardarDisponibilidad() }, enabled = uiState.isDirty && !isBusy) { Text("Guardar") }
            }
        }
    ) { padding ->
        Column(Modifier
            .padding(padding)
            .fillMaxSize()) {
            if (errorMessage != null) {
                AssistChip(onClick = { vm.limpiarError() }, label = { Text(errorMessage) })
            }
            if (uiState.isLoading && bloques.isEmpty()) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            uiState.disponibilidad?.let { disponibilidad ->
                val lastUpdated = remember(disponibilidad) {
                    disponibilidad.modificadoEn.format(disponibilidadFormatter)
                }
                Text(
                    "Última actualización: $lastUpdated",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // Agrupar por día y ordenar por inicio
            val grupos = remember(bloques) {
                BloqueHorarioDto.Dia.entries.associateWith { dia ->
                    bloques.mapIndexed { idx, b -> idx to b }.filter { it.second.dia == dia }
                        .sortedBy { it.second.inicio }
                }
            }

            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(BloqueHorarioDto.Dia.entries.toList()) { dia ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(diaSpanish(dia), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(onClick = { showAddForDay.value = dia }, enabled = !isBusy) { Text("Agregar") }
                                    // Cambiado a TextButton con color de error para acción destructiva
                                    TextButton(
                                        onClick = { vm.removeBloquesDia(dia) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Vaciar") }
                                }
                            }
                            HorizontalDivider()
                            val lista = grupos[dia].orEmpty()
                            if (lista.isEmpty()) {
                                Text(
                                    "Sin bloques para este día",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                // Usamos forEachIndexed para añadir el divisor
                                lista.forEachIndexed { index, (globalIndex, b) ->
                                    BloqueRow(
                                        bloque = b,
                                        onInicioChange = { vm.setInicio(globalIndex, it) },
                                        onFinChange = { vm.setFin(globalIndex, it) },
                                        onHabilitadoChange = { vm.setHabilitado(globalIndex, it) },
                                        onDelete = { vm.removeBloque(globalIndex) }
                                    )
                                    // Añadir divisor entre bloques, pero no después del último
                                    if (index < lista.size - 1) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Diálogo Agregar bloque
            val diaDialog = showAddForDay.value
            if (diaDialog != null) {
                var inicio by remember(diaDialog) { mutableStateOf("09:00") }
                var fin by remember(diaDialog) { mutableStateOf("18:00") }
                var habil by remember(diaDialog) { mutableStateOf(true) }
                var errorLocal by remember(diaDialog) { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showAddForDay.value = null },
                    title = { Text("Nuevo bloque - ${diaSpanish(diaDialog)}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Apilados verticalmente para mejor layout
                            OutlinedTextField(value = inicio, onValueChange = { inicio = it }, label = { Text("Inicio (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = fin, onValueChange = { fin = it }, label = { Text("Fin (HH:mm)") }, modifier = Modifier.fillMaxWidth())

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically // Añadido
                            ) {
                                Text("Habilitado")
                                Switch(checked = habil, onCheckedChange = { habil = it })
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Plantillas:")
                                // Envueltos en LazyRow para evitar desbordamiento
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item { OutlinedButton(onClick = { inicio = "09:00"; fin = "13:00" }) { Text("Mañana") } }
                                    item { OutlinedButton(onClick = { inicio = "15:00"; fin = "19:00" }) { Text("Tarde") } }
                                    item { OutlinedButton(onClick = { inicio = "09:00"; fin = "18:00" }) { Text("Completa") } }
                                }
                            }

                            if (errorLocal.isNotEmpty()) {
                                AssistChip(onClick = { errorLocal = "" }, label = { Text(errorLocal) })
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val msg = vm.puedeAgregarBloque(diaDialog, inicio, fin)
                            if (msg != null) {
                                errorLocal = msg
                            } else {
                                vm.addBloqueCustom(diaDialog, inicio, fin, habil)
                                showAddForDay.value = null
                            }
                        }) { Text("Agregar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddForDay.value = null }) { Text("Cancelar") }
                    }
                )
            }
        }
    }
}

private fun diaSpanish(dia: BloqueHorarioDto.Dia): String = when (dia) {
    BloqueHorarioDto.Dia.MONDAY -> "Lunes"
    BloqueHorarioDto.Dia.TUESDAY -> "Martes"
    BloqueHorarioDto.Dia.WEDNESDAY -> "Miércoles"
    BloqueHorarioDto.Dia.THURSDAY -> "Jueves"
    BloqueHorarioDto.Dia.FRIDAY -> "Viernes"
    BloqueHorarioDto.Dia.SATURDAY -> "Sábado"
    BloqueHorarioDto.Dia.SUNDAY -> "Domingo"
}

@Composable
private fun BloqueRow(
    bloque: BloqueHorarioDto,
    onInicioChange: (String) -> Unit,
    onFinChange: (String) -> Unit,
    onHabilitadoChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    // Eliminada la ElevatedCard anidada. Ahora es una Column simple.
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Padding vertical para separar
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // Eliminado el LazyRow de FilterChips. Es confuso para la UX.

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Usamos weights para que compartan el espacio
            OutlinedTextField(
                value = bloque.inicio,
                onValueChange = onInicioChange,
                label = { Text("Inicio (HH:mm)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = bloque.fin,
                onValueChange = onFinChange,
                label = { Text("Fin (HH:mm)") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically // Añadido
        ) {
            Text(if (bloque.habilitado) "Habilitado" else "Deshabilitado")
            Switch(checked = bloque.habilitado, onCheckedChange = onHabilitadoChange)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
        }
    }
}

private val disponibilidadFormatter: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", java.util.Locale.forLanguageTag("es-CL"))
