package cl.clinipets.feature.mascotas.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.CrearMascota
import java.util.UUID

@Composable
fun MascotaFormRoute(
    onBack: () -> Unit,
    onMascotaGuardada: (UUID, Boolean) -> Unit,
    viewModel: MascotaFormViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is MascotaFormEvento.MascotaGuardada -> onMascotaGuardada(evento.id, evento.fueEdicion)
            }
        }
    }

    MascotaFormScreen(
        estado = estado,
        onBack = onBack,
        onGuardar = viewModel::guardar,
        onNombreChange = viewModel::onNombreChange,
        onEspecieChange = viewModel::onEspecieChange,
        onRazaChange = viewModel::onRazaChange,
        onSexoChange = viewModel::onSexoChange,
        onFechaNacimientoChange = viewModel::onFechaNacimientoChange,
        onPesoChange = viewModel::onPesoChange,
    )
}

private data class FormField(
    val key: String,
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit,
    val placeholder: String? = null,
    val keyboard: KeyboardType = KeyboardType.Text,
    val supportingText: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MascotaFormScreen(
    estado: MascotaFormUiState,
    onBack: () -> Unit,
    onGuardar: () -> Unit,
    onNombreChange: (String) -> Unit,
    onEspecieChange: (CrearMascota.Especie) -> Unit,
    onRazaChange: (String) -> Unit,
    onSexoChange: (String) -> Unit,
    onFechaNacimientoChange: (String) -> Unit,
    onPesoChange: (String) -> Unit,
) {
    val especies = CrearMascota.Especie.values().toList()
    var dropdownAbierto by remember { mutableStateOf(false) }
    val sexoOpciones = listOf("Macho", "Hembra")

    val campos = listOf(
        FormField(
            key = "nombre",
            label = "Nombre",
            value = estado.nombre,
            onValueChange = onNombreChange,
            supportingText = estado.nombreError,
        ),
        FormField(
            key = "raza",
            label = "Raza (opcional)",
            value = estado.raza,
            onValueChange = onRazaChange,
        ),
        FormField(
            key = "fechaNacimiento",
            label = "Fecha de nacimiento",
            value = estado.fechaNacimiento,
            onValueChange = onFechaNacimientoChange,
            placeholder = "AAAA-MM-DD",
        ),
        FormField(
            key = "pesoKg",
            label = "Peso en kg (opcional)",
            value = estado.pesoKg,
            onValueChange = onPesoChange,
            keyboard = KeyboardType.Number,
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (estado.modo == MascotaFormularioModo.CREAR) "Nueva mascota" else "Editar mascota",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Text(
                    text = "Completa los datos para mantener la ficha clínica al día.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (estado.error != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text(
                            text = estado.error.mensaje ?: "Ocurrió un error inesperado.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text(
                            text = "Datos básicos",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )

                        campos.forEach { campo ->
                            OutlinedTextField(
                                value = campo.value,
                                onValueChange = campo.onValueChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(text = campo.label) },
                                placeholder = campo.placeholder?.let { { Text(text = it) } },
                                supportingText = campo.supportingText?.let { { Text(text = it) } },
                                isError = campo.supportingText != null,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = campo.keyboard,
                                ),
                                singleLine = true,
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text(
                            text = "Detalles",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )

                        if (estado.modo == MascotaFormularioModo.CREAR) {
                            ExposedDropdownMenuBox(
                                expanded = dropdownAbierto,
                                onExpandedChange = { dropdownAbierto = it },
                            ) {
                                TextField(
                                    value = estado.especie?.value ?: "Selecciona especie",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(text = "Especie") },
                                    isError = estado.especieError != null,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownAbierto)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                )
                                ExposedDropdownMenu(
                                    expanded = dropdownAbierto,
                                    onDismissRequest = { dropdownAbierto = false },
                                ) {
                                    especies.forEach { especie ->
                                        DropdownMenuItem(
                                            text = { Text(text = especie.value) },
                                            onClick = {
                                                dropdownAbierto = false
                                                onEspecieChange(especie)
                                            },
                                        )
                                    }
                                }
                            }
                            estado.especieError?.let { Text(text = it) }
                        } else {
                            OutlinedTextField(
                                value = estado.especie?.value ?: "",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(text = "Especie") },
                                enabled = false,
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Sexo (opcional)",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                sexoOpciones.forEach { opcion ->
                                    AssistChip(
                                        onClick = { onSexoChange(opcion) },
                                        label = { Text(text = opcion) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (estado.sexo.equals(opcion, ignoreCase = true)) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            labelColor = MaterialTheme.colorScheme.onSurface,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onGuardar,
                    enabled = !estado.guardando,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (estado.guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(text = if (estado.modo == MascotaFormularioModo.CREAR) "Crear mascota" else "Guardar cambios")
                }
            }
        }
    }
}
