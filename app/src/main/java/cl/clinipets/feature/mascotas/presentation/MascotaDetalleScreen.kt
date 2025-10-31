package cl.clinipets.feature.mascotas.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.Mascota
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
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
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "No pudimos cargar la información",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(text = error.mensaje ?: "Intenta nuevamente en unos minutos.")
            Button(onClick = onReintentar) {
                Text(text = "Reintentar")
            }
        }
    }
}

@Composable
private fun ContenidoMascota(mascota: Mascota) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            MascotaHeaderCard(mascota = mascota)
        }
        item {
            MascotaInfoCard(mascota = mascota)
        }
        mascota.fechaNacimiento?.let { fecha ->
            edadAproximada(fecha)?.let { edad ->
                item {
                    MascotaTipCard(texto = "Edad aproximada: $edad. Mantén sus controles al día para acompañar cada etapa.")
                }
            }
        }
    }
}

@Composable
private fun MascotaHeaderCard(mascota: Mascota) {
    val fondo = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        ),
    )
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .background(fondo, shape = RoundedCornerShape(28.dp))
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.height(36.dp),
            )
            Text(
                text = mascota.nombre,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                ),
            )
            Text(
                text = especieLegible(mascota.especie.value),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                ),
            )
        }
    }
}

@Composable
private fun MascotaInfoCard(mascota: Mascota) {
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
                text = "Ficha básica",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Divider()
            MascotaInfoRow(titulo = "Especie", valor = especieLegible(mascota.especie.value))
            mascota.raza?.takeIf { it.isNotBlank() }?.let {
                MascotaInfoRow(titulo = "Raza", valor = it)
            }
            mascota.sexo?.takeIf { it.isNotBlank() }?.let {
                MascotaInfoRow(titulo = "Sexo", valor = it.replaceFirstChar { c -> c.titlecase(Locale.getDefault()) })
            }
            mascota.fechaNacimiento?.let {
                MascotaInfoRow(
                    titulo = "Fecha de nacimiento",
                    valor = formatoFecha(it),
                )
            }
            mascota.pesoKg?.let {
                MascotaInfoRow(
                    titulo = "Peso",
                    valor = "${"%.1f".format(it)} kg",
                )
            }
        }
    }
}

@Composable
private fun MascotaInfoRow(
    titulo: String,
    valor: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun MascotaTipCard(texto: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Text(
            text = texto,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
        )
    }
}

private fun especieLegible(valor: String): String =
    valor.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }

private fun formatoFecha(fecha: LocalDate): String =
    fecha.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

private fun edadAproximada(fecha: LocalDate): String? {
    val periodo = Period.between(fecha, LocalDate.now())
    return when {
        periodo.years > 0 -> "${periodo.years} ${if (periodo.years == 1) "año" else "años"}"
        periodo.months > 0 -> "${periodo.months} ${if (periodo.months == 1) "mes" else "meses"}"
        periodo.days >= 0 -> "Reciente"
        else -> null
    }
}
