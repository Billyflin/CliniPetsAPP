package cl.clinipets.ui.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.DiscoveryRequest
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservaFormScreen(
    mascotaId: UUID,
    procedimientoSku: String,
    modo: DiscoveryRequest.ModoAtencion,
    lat: Double?,
    lng: Double?,
    veterinarioId: UUID? = null,
    precioSugerido: Int? = null,
    onBack: () -> Unit,
    onReservada: () -> Unit,
    vm: ReservaViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.init(mascotaId, procedimientoSku, modo, lat, lng, veterinarioId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Confirmar reserva")
                        Text(
                            text = procedimientoSku,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mensaje informativo
            Text(
                text = "Revisa los datos antes de confirmar tu reserva.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Error en un contenedor más visible
            ui.error?.let {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // --- Sección: Fecha y horario ---
            Text("Fecha y horario", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = ui.fecha,
                onValueChange = vm::setFecha,
                label = { Text("Fecha") },
                placeholder = { Text("Ej: 2025-07-21") },
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = ui.horaInicio,
                    onValueChange = vm::setHoraInicio,
                    modifier = Modifier.weight(1f),
                    label = { Text("Hora inicio") },
                    placeholder = { Text("Ej: 09:00") },
                    singleLine = true
                )
            }

            // --- Sección: Ubicación ---
            Text("Ubicación", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = ui.direccion,
                onValueChange = vm::setDireccion,
                label = { Text("Dirección") },
                placeholder = { Text("Calle, número, comuna") }
            )
            OutlinedTextField(
                value = ui.referencias,
                onValueChange = vm::setReferencias,
                label = { Text("Referencias para el profesional") },
                placeholder = { Text("Piso, portón, timbre, etc.") },
                supportingText = {
                    Text(
                        text = "Opcional",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            // --- Botón principal ---
            Button(
                onClick = { vm.crearReserva(onReservada) },
                enabled = !ui.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(if (ui.isSubmitting) "Creando reserva..." else "Confirmar reserva")
            }
        }
    }
}
