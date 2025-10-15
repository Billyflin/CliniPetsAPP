package cl.clinipets.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.clinipets.core.prefs.MapPrefs
import cl.clinipets.domain.discovery.DiscoveryRepository
import cl.clinipets.domain.discovery.VetNearby
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun VetsScreen(
    repo: DiscoveryRepository,
    onOpenVet: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isLoading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }
    val list = remember { mutableStateOf<List<VetNearby>>(emptyList()) }
    val abiertos = remember { mutableStateOf(false) }
    val radio = remember { mutableStateOf(3000) }

    fun refresh(lat: Double, lon: Double) {
        isLoading.value = true
        error.value = null
        scope.launch {
            runCatching { repo.buscarVets(lat, lon, radio.value, abiertoAhora = abiertos.value) }
                .onSuccess { list.value = it }
                .onFailure { error.value = it.message }
            isLoading.value = false
        }
    }

    LaunchedEffect(abiertos.value, radio.value) {
        // cargar usando última posición o fallback Santiago
        val savedLat = MapPrefs.lastLat(context).first()
        val savedLon = MapPrefs.lastLon(context).first()
        val lat = savedLat ?: -33.45
        val lon = savedLon ?: -70.66
        refresh(lat, lon)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Abiertos ahora")
                Spacer(Modifier.width(8.dp))
                Switch(checked = abiertos.value, onCheckedChange = { abiertos.value = it })
            }
            Button(onClick = {
                scope.launch {
                    val savedLat = MapPrefs.lastLat(context).first()
                    val savedLon = MapPrefs.lastLon(context).first()
                    val lat = savedLat ?: -33.45
                    val lon = savedLon ?: -70.66
                    refresh(lat, lon)
                }
            }) { Text("Refrescar") }
        }
        Spacer(Modifier.height(12.dp))

        if (isLoading.value) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        }
        error.value?.let { e ->
            Text("Error: $e", color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(list.value) { vet ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onOpenVet(vet.id) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(vet.nombre, style = MaterialTheme.typography.titleMedium)
                        val estado = if (vet.openNow) "Abierto" else "Cerrado"
                        val color = if (vet.openNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        Text(estado, color = color)
                        vet.ofertaNombre?.let { n ->
                            val precio = vet.ofertaPrecioMin?.let { p -> " - $" + p.toString() } ?: ""
                            Text("Oferta: $n$precio")
                        }
                    }
                }
            }
        }
    }
}
