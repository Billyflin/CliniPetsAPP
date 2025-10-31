package cl.clinipets.feature.descubrimiento.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.ProcedimientoItem
import cl.clinipets.openapi.models.VetItem

@Composable
fun DescubrimientoRoute(
    viewModel: DescubrimientoViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val estado by viewModel.estado.collectAsState()
    DescubrimientoScreen(
        estado = estado,
        onMascotaSeleccionada = viewModel::seleccionarMascota,
        onBuscarProcedimientos = viewModel::cargarProcedimientos,
        onRefrescar = viewModel::refrescar,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescubrimientoScreen(
    estado: DescubrimientoUiState,
    onMascotaSeleccionada: (MascotaResumen) -> Unit,
    onBuscarProcedimientos: (String?) -> Unit,
    onRefrescar: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Descubrir") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefrescar, enabled = !estado.cargando) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refrescar",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MascotaSelector(
                mascotas = estado.mascotas,
                seleccionada = estado.mascotaSeleccionada,
                onMascotaSeleccionada = onMascotaSeleccionada,
            )

            ProcedimientosSection(
                cargando = estado.cargando,
                procedimientos = estado.procedimientos,
                onBuscar = onBuscarProcedimientos,
            )

            VeterinariosSection(
                cargando = estado.cargando,
                veterinarios = estado.veterinarios,
            )

            estado.error?.let { ErrorBanner(error = it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MascotaSelector(
    mascotas: List<MascotaResumen>,
    seleccionada: MascotaResumen?,
    onMascotaSeleccionada: (MascotaResumen) -> Unit,
) {
    if (mascotas.isEmpty()) {
        Text(text = "Registra mascotas para explorar atención cercana.")
        return
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            value = seleccionada?.nombre ?: "Selecciona mascota",
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Mascota") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            mascotas.forEach { mascota ->
                DropdownMenuItem(
                    text = { Text(text = mascota.nombre) },
                    onClick = {
                        onMascotaSeleccionada(mascota)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ProcedimientosSection(
    cargando: Boolean,
    procedimientos: List<ProcedimientoItem>,
    onBuscar: (String?) -> Unit,
) {
    var consulta by rememberSaveable { mutableStateOf("") }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Procedimientos sugeridos")
        OutlinedTextField(
            value = consulta,
            onValueChange = { consulta = it },
            label = { Text(text = "Buscar procedimiento") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(
            onClick = { onBuscar(consulta.takeIf { it.isNotBlank() }) },
            enabled = !cargando,
        ) {
            Text(text = "Buscar")
        }
        if (procedimientos.isEmpty() && !cargando) {
            Text(text = "No se encontraron procedimientos.")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                procedimientos.forEach { procedimiento ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(text = procedimiento.nombre)
                            Text(
                                text = "Compatible con: ${procedimiento.compatibleCon.value}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            procedimiento.descripcion?.let { descripcion ->
                                Text(
                                    text = descripcion,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VeterinariosSection(
    cargando: Boolean,
    veterinarios: List<VetItem>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Veterinarios cercanos")
        if (cargando) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (veterinarios.isEmpty() && !cargando) {
            Text(text = "No encontramos veterinarios con los filtros actuales.")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                veterinarios.forEach { vet ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(text = vet.nombre)
                            Text(
                                text = "Estado: ${vet.estado}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "Atiende: ${vet.modosAtencion.joinToString { it.value }}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            vet.distanciaKm?.let { distancia ->
                                Text(
                                    text = "Distancia: ${"%.1f".format(distancia)} km",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(error: Resultado.Error) {
    Text(
        text = error.mensaje ?: "Ocurrió un error inesperado.",
        color = MaterialTheme.colorScheme.error,
    )
}
