package cl.clinipets.ui.agenda

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.CitaDetalladaResponse
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
                    Text("(Endpoint 'listar' pendiente en backend)", style = MaterialTheme.typography.bodySmall)
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
    val isConfirmada = cita.estado.name == "CONFIRMADA"
    val isCancelada = cita.estado.name == "CANCELADA"

    val cardAlpha = if (isCancelada) 0.5f else 1f

    Card(
        modifier = Modifier.alpha(cardAlpha),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Cita #${cita.id.toString().take(4)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Estado: ${cita.estado}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isConfirmada -> MaterialTheme.colorScheme.primary
                            isCancelada -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
                    Text(
                        text = "Precio: $${cita.precioFinal}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isConfirmada -> {
                        Text(
                            text = "Pago en clínica",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    isCancelada -> {
                        Text(
                            text = "Cancelada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    else -> {
                        // Allow cancelling other states if needed, or just show state
                        if (!isCancelada && !isConfirmada) { // Assuming simplistic logic
                             IconButton(
                                onClick = { onCancel(cita.id) },
                                enabled = !isBusy
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Cancelar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
