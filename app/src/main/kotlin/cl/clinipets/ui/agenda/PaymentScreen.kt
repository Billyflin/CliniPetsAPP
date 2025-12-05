package cl.clinipets.ui.agenda

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.CitaDetalladaResponse
import java.util.UUID

@Composable
fun PaymentScreen(
    citaId: String, // Changed to receive ID
    citaResponse: CitaDetalladaResponse?, // Full object can be passed initially
    viewModel: PaymentViewModel = hiltViewModel(),
    onPaymentConfirmed: () -> Unit,
    onHomeClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Start polling when the screen is launched
    LaunchedEffect(key1 = citaId) {
        val citaUuid = try {
            UUID.fromString(citaId)
        } catch (e: IllegalArgumentException) {
            // Handle invalid UUID format, maybe navigate back or show an error
            onHomeClick() // Simple fallback
            return@LaunchedEffect
        }
        viewModel.startPolling(citaUuid)
    }

    // Navigate when payment is confirmed
    LaunchedEffect(uiState.isPaymentConfirmed) {
        if (uiState.isPaymentConfirmed) {
            onPaymentConfirmed()
        }
    }

    // Use the initially passed response until the poller provides a newer one
    val cita = uiState.cita ?: citaResponse

    if (cita == null) {
        // Show a loading state or an error if the initial cita is also null
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Cargando datos de la cita...")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(100.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "¡Reserva Creada!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Para confirmar tu cita, realiza el pago ahora.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Total a Pagar", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "$ ${cita.precioFinal}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        val paymentUrl = cita.paymentUrl
        if (!paymentUrl.isNullOrBlank()) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Pagar con MercadoPago")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        AnimatedVisibility(visible = uiState.isPollingActive) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verificando pago...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedButton(
            onClick = onHomeClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver al Inicio")
        }
    }
}
