package cl.clinipets.ui.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.DiscoveryRequest
import java.util.UUID

private val buttonShape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
private val summaryCardShape = RoundedCornerShape(topStart = 32.dp, topEnd = 8.dp, bottomStart = 32.dp, bottomEnd = 8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservaConfirmScreen(
    procedimientoSku: String,
    modo: DiscoveryRequest.ModoAtencion,
    fecha: String,
    horaInicio: String,
    lat: Double?,
    lng: Double?,
    veterinarioId: UUID?,
    direccion: String?,
    referencias: String?,
    onBack: () -> Unit,
    onDone: () -> Unit,
    vm: ReservaViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        vm.init(
            mascotaId = UUID.randomUUID(),
            procedimientoSku = procedimientoSku,
            modo = modo,
            lat = lat,
            lng = lng,
            veterinarioId = veterinarioId
        )
        vm.setFecha(fecha)
        vm.setHoraInicio(horaInicio)
        direccion?.let(vm::setDireccion)
        referencias?.let(vm::setReferencias)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        topBar = {
            TopAppBar(
                title = { Text("Confirmación de reserva") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Revisa la información antes de enviar:",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = summaryCardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            InfoItem(Icons.Default.MedicalServices, "Procedimiento", procedimientoSku)
                            InfoItem(Icons.Default.Info, "Modo", modo.name)
                            InfoItem(Icons.Default.CalendarToday, "Fecha", fecha)
                            InfoItem(Icons.Default.Schedule, "Hora", horaInicio)

                            veterinarioId?.let {
                                InfoItem(Icons.Default.Person, "Veterinario", it.toString())
                            }
                            direccion?.let {
                                if (it.isNotBlank()) InfoItem(Icons.Default.LocationOn, "Dirección", it)
                            }
                            referencias?.let {
                                if (it.isNotBlank()) InfoItem(Icons.Default.Notes, "Referencias", it)
                            }
                        }
                    }

                    ui.error?.let {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }


                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { vm.crearReserva(onDone) },
                        enabled = !ui.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = buttonShape
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (ui.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text("Confirmando...")
                            } else {
                                Text("Confirmar reserva")
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onBack,
                        enabled = !ui.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = buttonShape
                    ) {
                        Text("Volver")
                    }
                }

            }
        }
    }
}


@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}