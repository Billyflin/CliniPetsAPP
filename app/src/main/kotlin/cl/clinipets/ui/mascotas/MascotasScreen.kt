package cl.clinipets.ui.mascotas

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.Mascota
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MascotasScreen(
    displayName: String?,
    onNavigateToMascotaDetail: (UUID) -> Unit,
    onNavigateToMascotaForm: () -> Unit,
    onBack: () -> Unit,
    vm: MascotasViewModel = hiltViewModel()
) {
    val mascotas by vm.items.collectAsState()
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(Unit) { vm.cargar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Mascotas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToMascotaForm) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Mascota")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                // Estado de carga inicial
                cargando && mascotas.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // Estado de error
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { vm.refrescar() }) {
                            Text("Reintentar")
                        }
                    }
                }

                // Estado vacío
                mascotas.isEmpty() -> {
                    Text(
                        text = displayName?.let { "No hay mascotas registradas para $it." }
                            ?: "Aún no tienes mascotas registradas.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Estado con datos
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(mascotas, key = { it.id!! }) { mascota ->
                            MascotaCard(
                                mascota = mascota,
                                onClick = { onNavigateToMascotaDetail(mascota.id!!) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MascotaCard(mascota: Mascota, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mascota.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val razaNombre = mascota.raza?.nombre?.takeIf { it.isNotBlank() }
                Text(
                    text = "${mascota.especie.value}${razaNombre?.let { " - $it" } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}