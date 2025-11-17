package cl.clinipets.ui.veterinarios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.ExcepcionHorario
import cl.clinipets.openapi.models.HorarioAtencion
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val buttonShape = RoundedCornerShape(24.dp)
private val itemCardShape = RoundedCornerShape(28.dp)
private val availCardShape = RoundedCornerShape(20.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiDisponibilidadScreen(
    onBack: () -> Unit,
    viewModel: MiDisponibilidadViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var fechaSeleccionada by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(Unit) {
        viewModel.cargarTodo()
    }

    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Disponibilidad") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Surface(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Seleccionar Fecha", style = MaterialTheme.typography.titleMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            fechaSeleccionada.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                fechaSeleccionada = fechaSeleccionada.minusDays(1)
                                viewModel.cargarDisponibilidadParaFecha(fechaSeleccionada)
                            },
                            shape = buttonShape
                        ) {
                            Text("Ayer")
                        }
                        OutlinedButton(
                            onClick = {
                                fechaSeleccionada = LocalDate.now()
                                viewModel.cargarDisponibilidadParaFecha(fechaSeleccionada)
                            },
                            shape = buttonShape
                        ) {
                            Text("Hoy")
                        }
                        OutlinedButton(
                            onClick = {
                                fechaSeleccionada = fechaSeleccionada.plusDays(1)
                                viewModel.cargarDisponibilidadParaFecha(fechaSeleccionada)
                            },
                            shape = buttonShape
                        ) {
                            Text("Mañana")
                        }
                    }
                }

                item {
                    Text("Disponibilidad para ${fechaSeleccionada.format(DateTimeFormatter.ISO_LOCAL_DATE)}:", style = MaterialTheme.typography.titleMedium)
                }

                if (state.disponibilidad.isEmpty()) {
                    item {
                        Text("No hay disponibilidad para esta fecha.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(state.disponibilidad) { intervalo ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = availCardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Text("${intervalo.inicio} - ${intervalo.fin}")
                            }
                        }
                    }
                }

                item {
                    Text("Horarios Semanales", style = MaterialTheme.typography.titleMedium)
                }

                if (state.horarios.isEmpty()) {
                    item {
                        Text("No hay horarios semanales definidos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(state.horarios) { horario ->
                        HorarioItemCard(
                            texto = "${horario.diaSemana}: ${horario.horaInicio} - ${horario.horaFin}",
                            onDelete = { viewModel.eliminarHorario(horario.id!!, fechaSeleccionada) }
                        )
                    }
                }

                item {
                    Text("Excepciones", style = MaterialTheme.typography.titleMedium)
                }

                if (state.excepciones.isEmpty()) {
                    item {
                        Text("No hay excepciones definidas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(state.excepciones) { ex ->
                        val texto = if (ex.cerrado == true) {
                            "CERRADO el ${ex.fecha}"
                        } else {
                            "${ex.fecha}: ${ex.horaInicio} - ${ex.horaFin}"
                        }
                        HorarioItemCard(
                            texto = texto,
                            onDelete = { viewModel.eliminarExcepcion(ex.id!!, fechaSeleccionada) }
                        )
                    }
                }

                item {
                    Text("Acciones Rápidas", style = MaterialTheme.typography.titleMedium)
                }

                item {
                    OutlinedButton(
                        onClick = {
                            val nueva = ExcepcionHorario(
                                id = null,
                                tipoOwner = ExcepcionHorario.TipoOwner.VETERINARIO,
                                fecha = fechaSeleccionada,
                                cerrado = true,
                                horaInicio = null,
                                horaFin = null,
                                motivo = "Cerrado manual"
                            )
                            viewModel.crearExcepcion(nueva, fechaSeleccionada)
                        },
                        shape = buttonShape,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("Marcar ${fechaSeleccionada.format(DateTimeFormatter.ISO_LOCAL_DATE)} como CERRADO")
                    }
                }

                item {
                    Button(
                        onClick = {
                            val nuevo = HorarioAtencion(
                                id = null,
                                tipoOwner = HorarioAtencion.TipoOwner.VETERINARIO,
                                diaSemana = HorarioAtencion.DiaSemana.MONDAY,
                                horaInicio = LocalTime.of(9, 0).toString(),
                                horaFin = LocalTime.of(13, 0).toString()
                            )
                            viewModel.crearHorario(nuevo, fechaSeleccionada)
                        },
                        shape = buttonShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Agregar Horario Lunes 09:00–13:00 (Ejemplo)")
                    }
                }
            }
        }

        if (state.cargando) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun HorarioItemCard(
    texto: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = itemCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(texto, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}