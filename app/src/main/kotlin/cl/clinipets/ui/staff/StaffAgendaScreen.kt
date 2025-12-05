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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var horaInicio by remember { mutableStateOf("") }
    var horaFin by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

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
                dialogError = null
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
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    dialogError = null
                    val inicio = runCatching { LocalTime.parse(horaInicio, timeFormatter) }.getOrNull()
                    val fin = runCatching { LocalTime.parse(horaFin, timeFormatter) }.getOrNull()
                    if (inicio == null || fin == null) {
                        dialogError = "Formato de hora inválido (usa HH:mm)"
                        return@TextButton
                    }
                    if (!fin.isAfter(inicio)) {
                        dialogError = "La hora fin debe ser posterior al inicio"
                        return@TextButton
                    }
                    viewModel.crearBloqueo(inicio, fin, motivo)
                    showBlockDialog = false
                    horaInicio = ""
                    horaFin = ""
                    motivo = ""
                }) {
                    Text("Bloquear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Nuevo bloqueo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = horaInicio,
                        onValueChange = { horaInicio = it },
                        label = { Text("Hora inicio (HH:mm)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = horaFin,
                        onValueChange = { horaFin = it },
                        label = { Text("Hora fin (HH:mm)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text("Motivo (opcional)") }
                    )
                    dialogError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
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
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bloqueo.motivo ?: "Bloqueo de agenda",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
