package cl.clinipets.feature.mascotas.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.R
import cl.clinipets.openapi.models.Mascota
import java.time.LocalDate
import java.time.Period
import java.util.Locale
import java.util.UUID

@Composable
fun MisMascotasRoute(
    viewModel: MisMascotasViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAgregarMascota: () -> Unit,
    onMascotaSeleccionada: (UUID) -> Unit,
) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.recargar()
    }
    MisMascotasScreen(
        estado = estado,
        onReintentar = viewModel::recargar,
        onNavigateBack = onNavigateBack,
        onAgregarMascota = onAgregarMascota,
        onMascotaSeleccionada = onMascotaSeleccionada,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisMascotasScreen(
    estado: MisMascotasUiState,
    onReintentar: () -> Unit,
    onNavigateBack: () -> Unit,
    onAgregarMascota: () -> Unit,
    onMascotaSeleccionada: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Mis mascotas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAgregarMascota) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Agregar mascota",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAgregarMascota,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                    )
                },
                text = { Text(text = "Agregar mascota") },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (estado.cargando) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
            }

            when {
                estado.requiereSesion -> SesionRequeridaContenido()
                estado.error != null -> ErrorContenido(
                    mensaje = estado.error.mensaje ?: "No pudimos cargar tus mascotas.",
                    onReintentar = onReintentar,
                )
                estado.mascotas.isEmpty() -> VacioContenido(onAgregarMascota = onAgregarMascota)
                else -> ListaMascotasContenido(
                    mascotas = estado.mascotas,
                    onSeleccionar = onMascotaSeleccionada,
                )
            }
        }
    }
}

@Composable
private fun ListaMascotasContenido(
    mascotas: List<Mascota>,
    onSeleccionar: (UUID) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
    ) {
        item {
            HeroMascotas(total = mascotas.size)
        }
        items(mascotas, key = { it.id ?: it.nombre }) { mascota ->
            MascotaCard(
                mascota = mascota,
                onSeleccionar = onSeleccionar,
            )
        }
    }
}

@Composable
private fun HeroMascotas(total: Int) {
    val fondo = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
        ),
    )
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .background(fondo, shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Tienes $total ${if (total == 1) "mascota registrada" else "mascotas registradas"}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondary,
                    ),
                )
                Text(
                    text = "Mantén actualizados sus datos para que las citas y recordatorios funcionen mejor.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.9f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun MascotaCard(
    mascota: Mascota,
    onSeleccionar: (UUID) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { mascota.id?.let(onSeleccionar) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(18.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = mascota.nombre,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = especieLegible(mascota.especie.value),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val detalles = buildList {
                    mascota.raza?.takeIf { it.isNotBlank() }?.let { add(it) }
                    edadAproximada(mascota.fechaNacimiento)?.let { add(it) }
                    mascota.pesoKg?.let { add("${"%.1f".format(it)} kg") }
                }
                if (detalles.isNotEmpty()) {
                    Text(
                        text = detalles.joinToString(separator = " · "),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun VacioContenido(onAgregarMascota: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.logopastel),
            contentDescription = null,
            modifier = Modifier.height(120.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Aún no registras mascotas",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Agrega la primera para guardar sus vacunas, peso y próximas atenciones.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAgregarMascota) {
            Text(text = "Registrar mascota")
        }
    }
}

@Composable
private fun ErrorContenido(
    mensaje: String,
    onReintentar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.height(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onReintentar) {
            Text(text = "Reintentar")
        }
    }
}

@Composable
private fun SesionRequeridaContenido() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Inicia sesión para ver tus mascotas.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

private fun especieLegible(valor: String): String =
    valor.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }

private fun edadAproximada(fechaNacimiento: LocalDate?): String? {
    fechaNacimiento ?: return null
    val hoy = LocalDate.now()
    val periodo = Period.between(fechaNacimiento, hoy)
    return when {
        periodo.years > 0 -> "${periodo.years} ${if (periodo.years == 1) "año" else "años"}"
        periodo.months > 0 -> "${periodo.months} ${if (periodo.months == 1) "mes" else "meses"}"
        periodo.days >= 0 -> "Reciente"
        else -> null
    }
}
