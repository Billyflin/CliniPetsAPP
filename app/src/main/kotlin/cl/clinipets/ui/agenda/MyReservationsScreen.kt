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
    onPay: (String) -> Unit,
    onCancel: (UUID) -> Unit,
    viewModel: MyReservationsViewModel = hiltViewModel()
) {
    val reservas by viewModel.reservas.collectAsState()
    val loading by viewModel.isLoading.collectAsState()

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
        }
    ) { padding ->
        if (loading) {
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
                    ReservationCard(
                        cita = cita,
                        onPay = { url ->
                            if (!url.isNullOrBlank()) {
                                onPay(url)
                            }
                        },
                        onCancel = { id ->
                            onCancel(id)
                            viewModel.cancelReservation(id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReservationCard(
    cita: CitaDetalladaResponse,
    onPay: (String?) -> Unit,
    onCancel: (UUID) -> Unit,
) {
    val isPendingPago = cita.estado?.name == "PENDIENTE_PAGO"
    val isConfirmada = cita.estado?.name == "CONFIRMADA"
    val isCancelada = cita.estado?.name == "CANCELADA"

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
                        text = "Cita #${cita.id.toString().take(4)}", // ID corto pal estilo
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
                    isPendingPago -> {
                        // Botón Pagar
                        TextButton(onClick = { onPay(cita.paymentUrl) }) {
                            Text("Pagar")
                        }
                        Spacer(Modifier.width(8.dp))
                        // Botón/ícono Cancelar
                        IconButton(onClick = { cita.id?.let { onCancel(it) } }) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancelar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    isConfirmada -> {
                        // Solo texto o botón de cancelar opcional; por ahora, solo texto
                        Text(
                            text = "Confirmada",
                            style = MaterialTheme.typography.bodyMedium,
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
                        // Otros estados: por ahora solo mostramos el estado como texto
                        Text(
                            text = cita.estado?.name ?: "Estado desconocido",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
