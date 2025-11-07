package cl.clinipets.ui.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import cl.clinipets.openapi.models.CrearSolicitudRequest
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.openapi.models.Procedimiento
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudesScreen(
    onBack: () -> Unit,
    vm: SolicitudesViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    var selectedMascota by remember { mutableStateOf<Mascota?>(null) }
    var selectedProcedimiento by remember { mutableStateOf<Procedimiento?>(null) }
    var modo by remember { mutableStateOf(CrearSolicitudRequest.ModoAtencion.DOMICILIO) }
    var preferencia by remember { mutableStateOf(CrearSolicitudRequest.PreferenciaLogistica.YO_LLEVO) }
    var bloque by remember { mutableStateOf(CrearSolicitudRequest.BloqueSolicitado.MANANA) }
    var fechaText by remember { mutableStateOf(LocalDate.now().plusDays(2).toString()) }
    var latitudText by remember { mutableStateOf("-33.45") }
    var longitudText by remember { mutableStateOf("-70.66") }
    var formError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.loadFormData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Solicitud") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.loadFormData(forceRefresh = true) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar"
                        )
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
                if (formError != null) {
                    AssistChip(onClick = { formError = null }, label = { Text(formError!!) })
                }
                if (ui.isLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }

            item {
                SelectorCard(
                    title = "Mascota",
                    placeholder = "Selecciona una mascota",
                    items = ui.mascotas,
                    selectedLabel = selectedMascota?.nombre,
                    itemLabel = { "${it.nombre} (${it.especie})" }
                ) { selectedMascota = it }
            }

            item {
                SelectorCard(
                    title = "Procedimiento",
                    placeholder = "Selecciona un servicio",
                    items = ui.procedimientos,
                    selectedLabel = selectedProcedimiento?.nombre,
                    itemLabel = { "${it.nombre} (${it.sku})" }
                ) { selectedProcedimiento = it }
            }

            item {
                EnumSelectorRow(
                    title = "Modo de atención",
                    options = CrearSolicitudRequest.ModoAtencion.entries.toList(),
                    selected = modo,
                    label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    onSelected = { modo = it }
                )
            }

            item {
                EnumSelectorRow(
                    title = "Preferencia logística",
                    options = CrearSolicitudRequest.PreferenciaLogistica.entries.toList(),
                    selected = preferencia,
                    label = { it.name.replace('_', ' ') },
                    onSelected = { preferencia = it }
                )
            }

            item {
                EnumSelectorRow(
                    title = "Bloque preferido",
                    options = CrearSolicitudRequest.BloqueSolicitado.entries.toList(),
                    selected = bloque,
                    label = { if (it == CrearSolicitudRequest.BloqueSolicitado.MANANA) "Mañana" else "Tarde" },
                    onSelected = { bloque = it }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fechaText,
                        onValueChange = { fechaText = it },
                        label = { Text("Fecha (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = latitudText,
                        onValueChange = { latitudText = it },
                        label = { Text("Latitud") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = longitudText,
                        onValueChange = { longitudText = it },
                        label = { Text("Longitud") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val mascotaUuid = selectedMascota?.id ?: run {
                            formError = "Selecciona una mascota"
                            return@Button
                        }
                        val procedimientoSku = selectedProcedimiento?.sku ?: run {
                            formError = "Selecciona un procedimiento"
                            return@Button
                        }
                        val fecha = runCatching { LocalDate.parse(fechaText) }.getOrNull() ?: run {
                            formError = "Fecha inválida"
                            return@Button
                        }
                        val lat = latitudText.toDoubleOrNull() ?: run {
                            formError = "Latitud inválida"
                            return@Button
                        }
                        val lng = longitudText.toDoubleOrNull() ?: run {
                            formError = "Longitud inválida"
                            return@Button
                        }

                        val request = CrearSolicitudRequest(
                            mascotaId = mascotaUuid,
                            procedimientoSku = procedimientoSku,
                            modoAtencion = modo,
                            preferenciaLogistica = preferencia,
                            fecha = fecha,
                            bloqueSolicitado = bloque,
                            latitud = lat,
                            longitud = lng
                        )
                        formError = null
                        vm.submitSolicitud(request) {
                            selectedProcedimiento = null
                        }
                    },
                    enabled = !ui.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (ui.isSubmitting) "Enviando..." else "Publicar solicitud")
                }
            }
        }
    }
}

@Composable
private fun <T> SelectorCard(
    title: String,
    placeholder: String,
    items: List<T>,
    selectedLabel: String?,
    itemLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = items.isNotEmpty()
            ) {
                Text(selectedLabel ?: placeholder)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (items.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Sin opciones") },
                        onClick = { expanded = false }
                    )
                } else {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(itemLabel(item)) },
                            onClick = {
                                expanded = false
                                onSelected(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> EnumSelectorRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) where T : Enum<T> {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    label = { Text(label(option)) }
                )
            }
        }
    }
}
