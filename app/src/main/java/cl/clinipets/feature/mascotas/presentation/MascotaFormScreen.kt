package cl.clinipets.feature.mascotas.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (estado.modo == MascotaFormularioModo.CREAR) "Nueva mascota" else "Editar mascota") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (estado.error != null) {
                Text(text = estado.error.mensaje ?: "Ocurrió un error")
            }

            OutlinedTextField(
                value = estado.nombre,
                onValueChange = onNombreChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Nombre") },
                isError = estado.nombreError != null,
                supportingText = estado.nombreError?.let { { Text(text = it) } },
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownAbierto) },
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

            OutlinedTextField(
                value = estado.raza,
                onValueChange = onRazaChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Raza (opcional)") },
            )

            OutlinedTextField(
                value = estado.sexo,
                onValueChange = onSexoChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Sexo (opcional)") },
            )

            OutlinedTextField(
                value = estado.fechaNacimiento,
                onValueChange = onFechaNacimientoChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Fecha nacimiento (YYYY-MM-DD)") },
            )

            OutlinedTextField(
                value = estado.pesoKg,
                onValueChange = onPesoChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Peso en kg (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Button(
                onClick = onGuardar,
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (estado.guardando) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(text = if (estado.modo == MascotaFormularioModo.CREAR) "Crear" else "Guardar")
            }
        }
    }
}
