package cl.clinipets.ui.staff

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.BloqueoAgenda
import cl.clinipets.openapi.models.CitaDetalladaResponse
import cl.clinipets.ui.util.toLocalHour
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAgendaScreen(
    onProfileClick: () -> Unit = {},
    onCitaClick: (String) -> Unit = {},
    viewModel: StaffAgendaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var showBlockDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Staff Agenda") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Perfil")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                showBlockDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo bloqueo")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Selector de Fecha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.cambiarFecha(state.date.minusDays(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Anterior")
                }
                Text(
                    text = state.date.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { viewModel.cambiarFecha(state.date.plusDays(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Siguiente")
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.refresh() },
                state = pullRefreshState,
                modifier = Modifier.weight(1f)
            ) {
                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (state.agendaItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Agenda libre", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.agendaItems) { item ->
                            when (item) {
                                is ItemCita -> AppointmentItem(item.data, onClick = { onCitaClick(item.data.id.toString()) })
                                is ItemBloqueo -> BlockItem(
                                    bloqueo = item.data,
                                    onDelete = { item.data.id?.let { viewModel.eliminarBloqueo(it) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBlockDialog) {
        NuevoBloqueoDialog(
            onDismiss = { showBlockDialog = false },
            onConfirm = { inicio, fin, motivo ->
                viewModel.crearBloqueo(inicio, fin, motivo)
                showBlockDialog = false
            }
        )
    }
}

@Composable
fun AppointmentItem(cita: CitaDetalladaResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cita.fechaHoraInicio.toLocalHour(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(cita.estado)
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            val detalle = cita.detalles.firstOrNull()
            val nombreMascota = detalle?.nombreMascota ?: "Mascota"
            val nombreServicio = detalle?.nombreServicio ?: "Servicio"

            Text(
                text = "Mascota: $nombreMascota",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Servicio: $nombreServicio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusChip(estado: CitaDetalladaResponse.Estado) {
    val (color, text) = when (estado) {
        CitaDetalladaResponse.Estado.EN_SALA, CitaDetalladaResponse.Estado.EN_ATENCION -> Color(0xFF4CAF50) to "En Atención"
        CitaDetalladaResponse.Estado.CONFIRMADA -> Color(0xFF2196F3) to "Confirmada"
        CitaDetalladaResponse.Estado.PENDIENTE_PAGO -> Color(0xFFFF9800) to "Pendiente Pago"
        CitaDetalladaResponse.Estado.FINALIZADA -> Color.Gray to "Finalizada"
        CitaDetalladaResponse.Estado.CANCELADA -> Color.Gray to "Cancelada"
        else -> Color.Gray to estado.name
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = text,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BlockItem(
    bloqueo: BloqueoAgenda,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${bloqueo.fechaHoraInicio.toLocalHour()} - ${bloqueo.fechaHoraFin.toLocalHour()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onErrorContainer
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar bloqueo", tint = colorScheme.onErrorContainer)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = colorScheme.onErrorContainer)
                Column {
                    Text(
                        text = "Bloqueo: ${bloqueo.motivo ?: "Agenda bloqueada"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${bloqueo.fechaHoraInicio.toLocalHour()} - ${bloqueo.fechaHoraFin.toLocalHour()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoBloqueoDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalTime, LocalTime, String?) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val now = LocalTime.now()
    val startState = remember { TimePickerState(now.hour, now.minute, true) }
    val endState = remember { TimePickerState(now.plusHours(1).hour % 24, now.minute, true) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val formatTime: (TimePickerState) -> String = { state ->
        val hour = state.hour.toString().padStart(2, '0')
        val minute = state.minute.toString().padStart(2, '0')
        "$hour:$minute"
    }

    fun validateAndConfirm() {
        val inicio = LocalTime.of(startState.hour, startState.minute)
        val fin = LocalTime.of(endState.hour, endState.minute)
        if (!fin.isAfter(inicio)) {
            error = "La hora fin debe ser posterior al inicio"
            return
        }
        onConfirm(inicio, fin, motivo.takeUnless { it.isBlank() })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { validateAndConfirm() }) { Text("Bloquear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        title = { Text("Nuevo bloqueo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeSelectorRow(
                    label = "Hora inicio",
                    timeText = formatTime(startState),
                    onClick = { showStartPicker = true }
                )
                TimeSelectorRow(
                    label = "Hora fin",
                    timeText = formatTime(endState),
                    onClick = { showEndPicker = true }
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(text = it, color = colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )

    if (showStartPicker) {
        TimePickerDialog(
            onDismissRequest = { showStartPicker = false },
            onConfirm = { showStartPicker = false },
            state = startState
        )
    }
    if (showEndPicker) {
        TimePickerDialog(
            onDismissRequest = { showEndPicker = false },
            onConfirm = { showEndPicker = false },
            state = endState
        )
    }
}

@Composable
private fun TimeSelectorRow(
    label: String,
    timeText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
            Text(text = timeText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    state: TimePickerState
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancelar") }
        },
        text = {
            TimePicker(state = state)
        }
    )
}
