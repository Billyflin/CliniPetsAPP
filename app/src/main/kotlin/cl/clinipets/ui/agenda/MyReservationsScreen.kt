package cl.clinipets.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.CitaDetalladaResponse
import cl.clinipets.ui.util.toLocalDateStr
import cl.clinipets.ui.util.toLocalHour
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReservationsScreen(
    onBack: () -> Unit,
    onCancel: (UUID) -> Unit,
    viewModel: MyReservationsViewModel = hiltViewModel()
) {
    val reservas by viewModel.reservas.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val actionInProgressId by viewModel.actionInProgressId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar Snackbar cuando haya error
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Reservas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (loading && reservas.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (reservas.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No tienes reservas aún 📅")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reservas) { cita ->
                    val isBusy = actionInProgressId == cita.id
                    ReservationCard(
                        cita = cita,
                        onCancel = { id ->
                            if (!isBusy) {
                                onCancel(id)
                                viewModel.cancelReservation(id)
                            }
                        },
                        isBusy = isBusy
                    )
                }
            }
        }
    }
}

@Composable
fun ReservationCard(
    cita: CitaDetalladaResponse,
    onCancel: (UUID) -> Unit,
    isBusy: Boolean,
) {
    val isConfirmada = cita.estado == CitaDetalladaResponse.Estado.CONFIRMADA
    val isCancelada = cita.estado == CitaDetalladaResponse.Estado.CANCELADA
    val isEnAtencion = cita.estado == CitaDetalladaResponse.Estado.EN_ATENCION

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCancelada) 0.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnAtencion) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = cita.fechaHoraInicio.toLocalDateStr(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${cita.fechaHoraInicio.toLocalHour()} hrs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                StatusBadge(cita.estado)
            }

            Spacer(Modifier.height(16.dp))
            
            cita.detalles.forEach { detalle ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${detalle.nombreServicio} - ${detalle.nombreMascota}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: $${cita.precioFinal}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )

                if (isConfirmada && !isBusy) {
                    TextButton(
                        onClick = { onCancel(cita.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(estado: CitaDetalladaResponse.Estado) {
    val color = when (estado) {
        CitaDetalladaResponse.Estado.CONFIRMADA -> Color(0xFF2196F3)
        CitaDetalladaResponse.Estado.EN_ATENCION -> Color(0xFFFF9800)
        CitaDetalladaResponse.Estado.FINALIZADA -> Color(0xFF4CAF50)
        CitaDetalladaResponse.Estado.CANCELADA -> Color.Gray
        CitaDetalladaResponse.Estado.NO_ASISTIO -> Color.Red
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = CircleShape
    ) {
        Text(
            text = estado.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}