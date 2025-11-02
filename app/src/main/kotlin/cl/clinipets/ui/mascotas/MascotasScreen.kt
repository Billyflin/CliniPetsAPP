package cl.clinipets.ui.mascotas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    Column(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        Button(
            onClick = { onNavigateToMascotaForm() },
            modifier = androidx.compose.ui.Modifier
        ) { }
        when {
            cargando -> {
                Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) { CircularProgressIndicator() }
            }
            mascotas.isEmpty() -> {
                Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = displayName?.let { "No hay mascotas registradas para $it." }
                            ?: "Aún no tienes mascotas registradas.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mascotas, key = { it.id ?: it.nombre }) { mascota ->
                        MascotaCard(
                            mascota = mascota,
                            onClick = {
                                mascota.id?.let {
                                    onNavigateToMascotaDetail(mascota.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun MascotaCard(mascota: Mascota, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = androidx.compose.ui.Modifier.clickable(onClick = onClick)
    ) {
        Column {
            Text(
                text = mascota.nombre,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Especie: ${mascota.especie.value}",
                style = MaterialTheme.typography.bodyMedium
            )
            mascota.raza?.takeIf { it.isNotBlank() }?.let { raza ->
                Text(
                    text = "Raza: $raza",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
