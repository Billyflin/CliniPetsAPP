package cl.clinipets.feature.descubrimiento.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.ProcedimientoItem
import cl.clinipets.openapi.models.VetItem
import java.util.Locale

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
                title = { Text(text = "Descubrir servicios") },
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
                            contentDescription = "Refrescar resultados",
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
                DescubrimientoHeader(estado = estado)
            }

            item {
                MascotaSelectorCard(
                    mascotas = estado.mascotas,
                    seleccionada = estado.mascotaSeleccionada,
                    onMascotaSeleccionada = onMascotaSeleccionada,
                )
            }

            item {
                ProcedimientosSection(
                    cargando = estado.cargando,
                    procedimientos = estado.procedimientos,
                    onBuscar = onBuscarProcedimientos,
                )
            }

            item {
                VeterinariosSection(
                    cargando = estado.cargando,
                    veterinarios = estado.veterinarios,
                )
            }

            estado.error?.let { error ->
                item {
                    ErrorBanner(error = error)
                }
            }
        }
    }
}

@Composable
private fun DescubrimientoHeader(estado: DescubrimientoUiState) {
    val fondo = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        ),
    )
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(fondo, shape = RoundedCornerShape(28.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Explora servicios para tus mascotas",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                ),
            )
            Text(
                text = if (estado.mascotaSeleccionada != null) {
                    "Recomendaciones personalizadas para ${estado.mascotaSeleccionada.nombre}."
                } else {
                    "Selecciona una mascota para ver procedimientos sugeridos."
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MascotaSelectorCard(
    mascotas: List<MascotaResumen>,
    seleccionada: MascotaResumen?,
    onMascotaSeleccionada: (MascotaResumen) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Mascota a evaluar",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            if (mascotas.isEmpty()) {
                Text(
                    text = "Registra mascotas para descubrir servicios compatibles.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = TextFieldDefaults.colors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        mascotas.forEach { mascota ->
                            DropdownItemMascota(
                                mascota = mascota,
                                onClick = {
                                    expanded = false
                                    onMascotaSeleccionada(mascota)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownItemMascota(
    mascota: MascotaResumen,
    onClick: () -> Unit,
) {
    androidx.compose.material3.DropdownMenuItem(
        text = {
            Column {
                Text(text = mascota.nombre, style = MaterialTheme.typography.bodyLarge)
                mascota.especie?.let {
                    Text(
                        text = it.lowercase(Locale.getDefault()).replaceFirstChar { c -> c.titlecase(Locale.getDefault()) },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun ProcedimientosSection(
    cargando: Boolean,
    procedimientos: List<ProcedimientoItem>,
    onBuscar: (String?) -> Unit,
) {
    var consulta by rememberSaveable { mutableStateOf("") }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Procedimientos sugeridos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
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
                Text(
                    text = "No se encontraron procedimientos para esta mascota.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    procedimientos.forEach { procedimiento ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = procedimiento.nombre,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                )
                                Text(
                                    text = "Compatible con: ${procedimiento.compatibleCon.value}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                procedimiento.descripcion?.let { descripcion ->
                                    Text(
                                        text = descripcion,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    )
                                }
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
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Veterinarios cercanos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            if (cargando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (veterinarios.isEmpty() && !cargando) {
                Text(
                    text = "No encontramos veterinarios con los filtros actuales.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    veterinarios.forEach { vet ->
                        VeterinarioCard(vet)
                    }
                }
            }
        }
    }
}

@Composable
private fun VeterinarioCard(vet: VetItem) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = vet.nombre,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
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
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(error: Resultado.Error) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Text(
            text = error.mensaje ?: "Ocurrió un error inesperado.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}
