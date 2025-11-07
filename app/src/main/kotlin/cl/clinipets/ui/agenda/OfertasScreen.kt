package cl.clinipets.ui.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.CrearOfertaRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfertasScreen(
    onBack: () -> Unit,
    vm: OfertasViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    var selectedSolicitud by remember { mutableStateOf<SolicitudDisponibleUi?>(null) }
    var precioServicio by remember { mutableStateOf("16000") }
    var precioLogistica by remember { mutableStateOf("0") }
    var horaCirugia by remember { mutableStateOf("10:00") }
    var horaRetiro by remember { mutableStateOf("") }
    var horaEntrega by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.refrescarSolicitudes() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitudes disponibles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refrescarSolicitudes() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                if (ui.error != null) {
                    AssistChip(onClick = { vm.clearStatus() }, label = { Text(ui.error!!) })
                }
                if (ui.successMessage != null) {
                    AssistChip(onClick = { vm.clearStatus() }, label = { Text(ui.successMessage!!) })
                }
                if (ui.isLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }

            items(ui.solicitudesDisponibles, key = { it.id }) { solicitud ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(solicitud.procedimientoNombre ?: "Procedimiento", fontWeight = FontWeight.Bold)
                        solicitud.mascotaNombre?.let {
                            Text("Mascota: $it (${solicitud.mascotaEspecie ?: "?"})")
                        }
                        solicitud.fecha?.let { Text("Fecha: $it (${solicitud.bloqueSolicitado ?: "-"})") }
                        solicitud.modoAtencion?.let { Text("Modo: $it") }
                        solicitud.preferenciaLogistica?.let { Text("Logística: $it") }
                        solicitud.clienteNombre?.let { Text("Cliente: $it") }
                        Button(
                            onClick = {
                                selectedSolicitud = solicitud
                                dialogError = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enviar oferta")
                        }
                    }
                }
            }

            if (ui.solicitudesDisponibles.isEmpty() && !ui.isLoading) {
                item {
                    Text(
                        text = "Aún no hay solicitudes abiertas que coincidan contigo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    val solicitudDialog = selectedSolicitud
    if (solicitudDialog != null) {
        AlertDialog(
            onDismissRequest = { selectedSolicitud = null },
            title = { Text("Oferta para ${solicitudDialog.procedimientoNombre ?: ""}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dialogError != null) {
                        AssistChip(onClick = { dialogError = null }, label = { Text(dialogError!!) })
                    }
                    OutlinedTextField(
                        value = precioServicio,
                        onValueChange = { precioServicio = it },
                        label = { Text("Precio servicio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = precioLogistica,
                        onValueChange = { precioLogistica = it },
                        label = { Text("Precio logística") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = horaCirugia,
                        onValueChange = { horaCirugia = it },
                        label = { Text("Hora cirugía propuesta (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = horaRetiro,
                        onValueChange = { horaRetiro = it },
                        label = { Text("Hora retiro (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = horaEntrega,
                        onValueChange = { horaEntrega = it },
                        label = { Text("Hora entrega estimada (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val precioSrv = precioServicio.toIntOrNull()
                    val precioLog = precioLogistica.toIntOrNull()
                    if (precioSrv == null || precioLog == null) {
                        dialogError = "Precios inválidos"
                        return@TextButton
                    }
                    if (horaCirugia.isBlank()) {
                        dialogError = "Hora de cirugía requerida"
                        return@TextButton
                    }
                    val request = CrearOfertaRequest(
                        precioServicio = precioSrv,
                        precioLogistica = precioLog,
                        horaCirugiaPropuesta = horaCirugia,
                        horaRetiroPropuesta = horaRetiro.takeIf { it.isNotBlank() },
                        horaEntregaEstimada = horaEntrega.takeIf { it.isNotBlank() }
                    )
                    vm.enviarOferta(solicitudDialog.id, request) {
                        selectedSolicitud = null
                    }
                }) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSolicitud = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
