package cl.clinipets.ui.staff

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.CitaDetalladaResponse
import cl.clinipets.ui.util.toLocalHour

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffCitaDetailScreen(
    citaId: String,
    onBack: () -> Unit,
    onPetClick: (String) -> Unit,
    onStartAtencion: (String, String) -> Unit,
    viewModel: StaffCitaDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(citaId) {
        viewModel.cargarCita(citaId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalle de Cita") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            if (state.cita != null && !state.isLoading) {
                val cita = state.cita!!
                val mascotaId = cita.detalles.firstOrNull()?.mascotaId?.toString()
                val isFinalizada = cita.estado == CitaDetalladaResponse.Estado.FINALIZADA

                Column(Modifier.padding(16.dp)) {
                    if (isFinalizada) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Atención Finalizada ✅",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        if (cita.estado == CitaDetalladaResponse.Estado.CONFIRMADA) {
                            Button(
                                onClick = { 
                                    viewModel.iniciarTriaje(onStartAtencion)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isCancelled && mascotaId != null,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("Iniciar Atención")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = { 
                                if (mascotaId != null) {
                                    onStartAtencion(cita.id.toString(), mascotaId)
                                } else {
                                    Toast.makeText(context, "Error: No hay mascota asociada", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isCancelled && mascotaId != null
                        ) {
                            Text(if (cita.estado == CitaDetalladaResponse.Estado.EN_ATENCION) "Continuar Atención" else "Ir a Ficha")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.cancelarCita() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isCancelled,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cancelar Cita")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Text(
                    text = state.error ?: "Error desconocido",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                state.cita?.let { cita ->
                    Content(cita = cita, onPetClick = onPetClick)
                }
            }
        }
    }
}

@Composable
private fun Content(
    cita: CitaDetalladaResponse,
    onPetClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val detalle = cita.detalles.firstOrNull()
    val nombreServicio = detalle?.nombreServicio ?: "Servicio General"
    val nombreMascota = detalle?.nombreMascota ?: "Mascota"
    val mascotaId = detalle?.mascotaId
    val isFinalizada = cita.estado == CitaDetalladaResponse.Estado.FINALIZADA

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = nombreServicio,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${cita.fechaHoraInicio.toLocalHour()} hrs",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            StatusChip(cita.estado)
        }

        HorizontalDivider()

        // Patient Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = mascotaId != null) {
                    mascotaId?.let { onPetClick(it.toString()) }
                },
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Paciente", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(
                        text = nombreMascota,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (mascotaId != null) {
                        Text(
                            text = "Ver ficha >",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialRow(
    label: String,
    amount: Int,
    isBold: Boolean = false,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "$ $amount", 
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = color
            )
        )
    }
}
