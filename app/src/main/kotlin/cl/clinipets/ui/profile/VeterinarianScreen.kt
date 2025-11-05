package cl.clinipets.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.Veterinario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeterinarianScreen(
    suggestedName: String?,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val uiState by vm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Campos de formulario (compartidos para crear/actualizar)
    var nombre by rememberSaveable { mutableStateOf(suggestedName.orEmpty()) }
    var licencia by rememberSaveable { mutableStateOf("") }
    var latitud by rememberSaveable { mutableStateOf("") }
    var longitud by rememberSaveable { mutableStateOf("") }
    var radio by rememberSaveable { mutableStateOf("") }
    var selectedModes by remember { mutableStateOf(setOf<String>()) }

    // Estado local para saber si ya precargamos los campos desde el perfil
    var didPrefill by rememberSaveable { mutableStateOf(false) }
    // Estado para mostrar diálogo de éxito post acción
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastAction by rememberSaveable { mutableStateOf("idle") } // "idle" | "create" | "update"

    // Cargar perfil al entrar
    LaunchedEffect(Unit) { vm.loadMyProfile() }

    // Prefill cuando llegue el perfil por primera vez
    LaunchedEffect(uiState.perfil) {
        val p = uiState.perfil
        if (p != null && !didPrefill) {
            nombre = p.nombreCompleto
            licencia = p.numeroLicencia.orEmpty()
            latitud = p.latitud?.toString().orEmpty()
            longitud = p.longitud?.toString().orEmpty()
            radio = p.radioCobertura?.toString().orEmpty()
            selectedModes = p.modosAtencion.map { it.name }.toSet()
            didPrefill = true
        }
        if (p != null && lastAction != "idle") {
            showSuccessDialog = true
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (lastAction == "update") "Perfil actualizado" else "Solicitud enviada") },
            text = { Text(if (lastAction == "update") "Tus datos profesionales fueron actualizados." else "Tu perfil está pendiente de verificación.") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    lastAction = "idle"
                    onCompleted()
                }) { Text("Aceptar") }
            }
        )
    }

    val isUpdating = uiState.perfil != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Datos profesionales") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = if (isUpdating) "Actualiza tus datos profesionales." else "Completa tus datos para comenzar el proceso de verificación.",
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre completo") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = licencia,
                onValueChange = { licencia = it },
                label = { Text("Número de licencia (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Modos de atención",
                style = MaterialTheme.typography.titleMedium
            )

            ModeSelector(
                selectedModes = selectedModes,
                onToggle = { modeName ->
                    selectedModes = if (selectedModes.contains(modeName)) selectedModes - modeName else selectedModes + modeName
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = latitud,
                    onValueChange = { latitud = it },
                    label = { Text("Latitud (opcional)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = longitud,
                    onValueChange = { longitud = it },
                    label = { Text("Longitud (opcional)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            OutlinedTextField(
                value = radio,
                onValueChange = { radio = it },
                label = { Text("Radio de cobertura (km)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Button(
                onClick = {
                    if (isUpdating) {

                    } else {

                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text(if (isUpdating) "Actualizar" else "Registrar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(
    selectedModes: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Veterinario.ModosAtencion.entries.forEach { mode ->
            val name = mode.name
            FilterChip(
                selected = selectedModes.contains(name),
                onClick = { onToggle(name) },
                label = { Text(name) },
                leadingIcon = if (selectedModes.contains(name)) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null
            )
        }
    }
}
