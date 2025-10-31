package cl.clinipets.feature.mascotas.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.Mascota
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun MascotaDetalleRoute(
    onBack: () -> Unit,
    onEditar: (UUID) -> Unit,
    onMascotaEliminada: () -> Unit,
    viewModel: MascotaDetalleViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                MascotaDetalleEvento.MascotaEliminada -> onMascotaEliminada()
            }
        }
    }

    MascotaDetalleScreen(
        estado = estado,
        onBack = onBack,
        onEditar = {
            estado.mascota?.id?.let(onEditar)
        },
        onEliminar = { viewModel.mostrarConfirmacionEliminar(true) },
        onConfirmarEliminar = { viewModel.eliminarMascota() },
        onCancelarEliminar = { viewModel.mostrarConfirmacionEliminar(false) },
        onReintentar = { viewModel.refrescar() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MascotaDetalleScreen(
    estado: MascotaDetalleUiState,
    onBack: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onConfirmarEliminar: () -> Unit,
    onCancelarEliminar: () -> Unit,
    onReintentar: () -> Unit,
) {
    val mascota = estado.mascota
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = mascota?.nombre ?: "Mascota") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditar, enabled = mascota != null) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar",
                        )
                    }
                    IconButton(onClick = onEliminar, enabled = mascota != null) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Eliminar",
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                estado.cargando -> Text(text = "Cargando...")
                estado.error != null -> ErrorDetalle(
                    error = estado.error,
                    onReintentar = onReintentar,
                )
                mascota != null -> ContenidoMascota(mascota = mascota)
            }
        }
    }

    if (estado.mostrandoConfirmacion) {
        AlertDialog(
            onDismissRequest = onCancelarEliminar,
            title = { Text(text = "Eliminar mascota") },
            text = { Text(text = "¿Seguro que deseas eliminar esta mascota? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = onConfirmarEliminar) {
                    Text(text = "Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelarEliminar) {
                    Text(text = "Cancelar")
                }
            },
        )
    }
}

@Composable
private fun ErrorDetalle(
    error: Resultado.Error,
    onReintentar: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = error.mensaje ?: "No se pudo cargar la mascota.")
        Button(onClick = onReintentar) {
            Text(text = "Reintentar")
        }
    }
}

@Composable
private fun ContenidoMascota(mascota: Mascota) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Nombre: ${mascota.nombre}")
        Text(text = "Especie: ${mascota.especie.value.lowercase().replaceFirstChar { it.uppercase() }}")
        mascota.raza?.let { Text(text = "Raza: $it") }
        mascota.sexo?.let { Text(text = "Sexo: $it") }
        mascota.pesoKg?.let { Text(text = "Peso: ${"%.1f".format(it)} kg") }
        mascota.fechaNacimiento?.let {
            Text(text = "Fecha de nacimiento: ${DateTimeFormatter.ISO_DATE.format(it)}")
        }
    }
}
