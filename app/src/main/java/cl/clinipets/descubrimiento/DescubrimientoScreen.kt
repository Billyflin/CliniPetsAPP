package cl.clinipets.descubrimiento

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.clinipets.network.Oferta
import cl.clinipets.network.Procedimiento
import cl.clinipets.network.VetItem
import java.util.Locale

@Composable
fun DescubrimientoScreen(viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory) {
    val vm: DescubrimientoViewModel = viewModel(factory = viewModelFactory)
    val vets by vm.veterinarios.collectAsState()
    val procedimientos by vm.procedimientos.collectAsState()
    val ofertas by vm.ofertas.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    var query by remember { mutableStateOf("") }
    var especie by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Descubrimiento", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Buscar procedimiento") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { vm.cargarProcedimientos(especie = if (especie.isBlank()) null else especie, q = if (query.isBlank()) null else query) }, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar procedimientos")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Procedimientos", style = MaterialTheme.typography.titleMedium)
        LazyColumn { items(procedimientos) { p: Procedimiento -> ProcedimientoRow(p) } }

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { vm.buscarVeterinarios(null, null, 10, especie = if (especie.isBlank()) null else especie) }, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar veterinarios cercanos")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Veterinarios", style = MaterialTheme.typography.titleMedium)
        LazyColumn { items(vets) { v: VetItem -> VetRow(v) } }

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { vm.buscarOfertas(especie = if (especie.isBlank()) null else especie) }, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar ofertas")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Ofertas", style = MaterialTheme.typography.titleMedium)
        LazyColumn { items(ofertas) { o: Oferta -> OfertaRow(o) } }

        if (!error.isNullOrEmpty()) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
        }
        if (isLoading) {
            Text("Cargando...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ProcedimientoRow(p: Procedimiento) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(p.nombre, style = MaterialTheme.typography.titleMedium)
        Text("Duración: ${p.duracionMinutos} min", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun VetRow(v: VetItem) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Text(v.nombre, style = MaterialTheme.typography.titleMedium)
        Text("Modos: ${v.modos.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
        if (v.distanciaKm != null) Text("A ${String.format(Locale.getDefault(), "%.1f", v.distanciaKm)} km", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun OfertaRow(o: Oferta) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(o.procedimientoNombre + " - $${o.precio}", style = MaterialTheme.typography.titleMedium)
        Text("Duración: ${o.duracionMinutos} min", style = MaterialTheme.typography.bodySmall)
    }
}
