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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.CitaDetalladaResponse
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
                if (state.appointments.isEmpty()) {
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
                        items(state.appointments) { cita ->
                            AppointmentItem(cita, onClick = { onCitaClick(cita.id.toString()) })
                        }
                    }
                }
            }
        }
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
                    text = cita.fechaHoraInicio.format(DateTimeFormatter.ofPattern("HH:mm")),
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