package cl.clinipets.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.RegistrarVeterinarioRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeterinarianOnboardingScreen(
    suggestedName: String?,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    vm: VeterinarianOnboardingViewModel = hiltViewModel()
) {
    val uiState by vm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var nombre by rememberSaveable { mutableStateOf(suggestedName.orEmpty()) }
    var licencia by rememberSaveable { mutableStateOf("") }
    var latitud by rememberSaveable { mutableStateOf("") }
    var longitud by rememberSaveable { mutableStateOf("") }
    var radio by rememberSaveable { mutableStateOf("") }
    var selectedModes by remember { mutableStateOf(setOf<RegistrarVeterinarioRequest.ModosAtencion>()) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
            vm.resetMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            showSuccessDialog = true
            vm.resetMessages()
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Solicitud enviada") },
            text = { Text("Tu perfil está pendiente de verificación.") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    onCompleted()
                }) {
                    Text("Aceptar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ser veterinario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
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
                text = "Completa tus datos para comenzar el proceso de verificación.",
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
                onToggle = { mode ->
                    selectedModes = if (selectedModes.contains(mode)) {
                        selectedModes - mode
                    } else {
                        selectedModes + mode
                    }
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
                    val request = RegistrarVeterinarioRequest(
                        nombreCompleto = nombre,
                        numeroLicencia = licencia.takeIf { it.isNotBlank() },
                        modosAtencion = if (selectedModes.isEmpty()) null else selectedModes,
                        latitud = latitud.toDoubleOrNull(),
                        longitud = longitud.toDoubleOrNull(),
                        radioCobertura = radio.toDoubleOrNull()
                    )
                    vm.submit(request)
                },
                enabled = nombre.isNotBlank() && !uiState.submitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.submitting) {
                    Text("Enviando…")
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text("Enviar solicitud", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(
    selectedModes: Set<RegistrarVeterinarioRequest.ModosAtencion>,
    onToggle: (RegistrarVeterinarioRequest.ModosAtencion) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RegistrarVeterinarioRequest.ModosAtencion.values().forEach { mode ->
            FilterChip(
                selected = selectedModes.contains(mode),
                onClick = { onToggle(mode) },
                label = { Text(mode.name) },
                leadingIcon = if (selectedModes.contains(mode)) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null
            )
        }
    }
}
