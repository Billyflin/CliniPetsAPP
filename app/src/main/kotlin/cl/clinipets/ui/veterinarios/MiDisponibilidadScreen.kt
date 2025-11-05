package cl.clinipets.ui.veterinarios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.DisponibilidadSemanal
import cl.clinipets.openapi.models.BloqueHorario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiDisponibilidadScreen(
    onBack: () -> Unit,
    vm: VeterinariosViewModel = hiltViewModel()
) {
    val cargando by vm.cargando.collectAsState()
    val error by vm.error.collectAsState()
    val disponibilidad: DisponibilidadSemanal? by vm.disponibilidad.collectAsState()

    LaunchedEffect(Unit) { vm.cargarDisponibilidad() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi disponibilidad") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (error != null) {
                AssistChip(onClick = { vm.limpiarError() }, label = { Text(error ?: "") })
            }
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.cargarDisponibilidad() }, enabled = !cargando) { Text("Refrescar") }
            }
            if (cargando && disponibilidad == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            ElevatedCard(Modifier.padding(12.dp).fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bloques (${disponibilidad?.bloques?.size ?: 0})")
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(disponibilidad?.bloques ?: emptyList()) { bloque: BloqueHorario ->
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(8.dp)) {
                                    Text("Día: ${bloque.dia}")
                                    Text("Inicio: ${bloque.inicio} - Fin: ${bloque.fin}")
                                    Text("Habilitado: ${bloque.habilitado}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
