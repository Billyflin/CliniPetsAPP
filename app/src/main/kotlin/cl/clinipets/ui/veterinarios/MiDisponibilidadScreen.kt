package cl.clinipets.ui.veterinarios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.ExcepcionHorario
import cl.clinipets.openapi.models.HorarioAtencion
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun MiDisponibilidadScreen(
    onBack: () -> Unit,
    viewModel: MiDisponibilidadViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        // carga inicial sin fecha
        viewModel.cargarTodo()
    }

    var fechaSeleccionada by remember { mutableStateOf(LocalDate.now()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text("Gestionar Disponibilidad", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        // ------------------------------
        // Selector de Fecha
        // ------------------------------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fecha: $fechaSeleccionada")
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                // cambiar fecha → recargar disponibilidad
                fechaSeleccionada = fechaSeleccionada.plusDays(1)
                viewModel.cargarDisponibilidadParaFecha(fechaSeleccionada)
            }) {
                Text("Mañana")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                fechaSeleccionada = fechaSeleccionada.minusDays(1)
                viewModel.cargarDisponibilidadParaFecha(fechaSeleccionada)
            }) {
                Text("Ayer")
            }
        }

        Spacer(Modifier.height(16.dp))

        // ------------------------------
        // Disponibilidad del día
        // ------------------------------
        Text("Disponibilidad para ${fechaSeleccionada}:")
        if (state.disponibilidad.isEmpty()) {
            Text("No hay disponibilidad.", color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn {
                items(state.disponibilidad) { intervalo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Text("${intervalo.inicio} - ${intervalo.fin}")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ------------------------------
        // Horarios recurrentes
        // ------------------------------
        Text("Horarios Semanales:")
        LazyColumn {
            items(state.horarios) { horario ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${horario.diaSemana}: ${horario.horaInicio} - ${horario.horaFin}")
                        IconButton(onClick = {
                            viewModel.eliminarHorario(horario.id!!, fechaSeleccionada)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar horario")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ------------------------------
        // Excepciones
        // ------------------------------
        Text("Excepciones:")
        LazyColumn {
            items(state.excepciones) { ex ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (ex.cerrado == true) {
                            Text("CERRADO el ${ex.fecha}")
                        } else {
                            Text("${ex.fecha}: ${ex.horaInicio} - ${ex.horaFin}")
                        }

                        IconButton(onClick = {
                            viewModel.eliminarExcepcion(ex.id!!, fechaSeleccionada)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar excepción")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ------------------------------
        // Crear EXCEPCIÓN simple
        // ------------------------------
        Button(onClick = {
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
        }) {
            Text("Agregar Excepción (cerrado día entero)")
        }

        Spacer(Modifier.height(16.dp))

        // ------------------------------
        // Crear HORARIO rápido (ejemplo)
        // ------------------------------
        Button(onClick = {
            val nuevo = HorarioAtencion(
                id = null,
                tipoOwner = HorarioAtencion.TipoOwner.VETERINARIO,
                diaSemana = HorarioAtencion.DiaSemana.MONDAY,
                horaInicio = LocalTime.of(9, 0).toString(),
                horaFin = LocalTime.of(13, 0).toString()
            )
            viewModel.crearHorario(nuevo, fechaSeleccionada)
        }) {
            Text("Agregar Horario 09:00–13:00")
        }
    }

    // ------------------------------
    // Loading overlay
    // ------------------------------
    if (state.cargando) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // ------------------------------
    // Error Snackbar
    // ------------------------------
    state.error?.let { msg ->
        LaunchedEffect(msg) {
            println("Error: $msg")
        }
    }
}
