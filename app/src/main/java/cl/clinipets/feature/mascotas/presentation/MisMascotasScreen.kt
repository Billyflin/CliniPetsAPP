package cl.clinipets.feature.mascotas.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.Mascota

@Composable
fun MisMascotasRoute(
    viewModel: MisMascotasViewModel = hiltViewModel(),
    onCerrarSesion: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val estado by viewModel.estado.collectAsState()
    MisMascotasScreen(
        estado = estado,
        onReintentar = viewModel::recargar,
        onCerrarSesion = onCerrarSesion,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisMascotasScreen(
    estado: MisMascotasUiState,
    onReintentar: () -> Unit,
    onCerrarSesion: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Mis Mascotas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onCerrarSesion) {
                        Text(text = "Cerrar sesión")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (estado.cargando) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            when {
                estado.requiereSesion -> SesionRequeridaContenido()
                estado.error != null -> ErrorContenido(
                    mensaje = estado.error.mensaje ?: "No pudimos cargar tus mascotas.",
                    onReintentar = onReintentar,
                )
                estado.mascotas.isEmpty() -> VacioContenido(onReintentar = onReintentar)
                else -> ListaMascotasContenido(mascotas = estado.mascotas)
            }
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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = mensaje)
        Button(onClick = onReintentar) {
            Text(text = "Reintentar")
        }
    }
}

@Composable
private fun VacioContenido(onReintentar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Aún no registras mascotas.")
        Button(onClick = onReintentar) {
            Text(text = "Actualizar")
        }
    }
}

@Composable
private fun ListaMascotasContenido(mascotas: List<Mascota>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(mascotas, key = { it.id ?: it.nombre }) { mascota ->
            MascotaItem(mascota = mascota)
        }
    }
}

@Composable
private fun SesionRequeridaContenido() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Inicia sesión para ver tus mascotas.")
    }
}

@Composable
private fun MascotaItem(mascota: Mascota) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = mascota.nombre)
            Text(
                text = "Especie: ${mascota.especie.value}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            mascota.raza?.let {
                Text(text = "Raza: $it")
            }
        }
    }
}
