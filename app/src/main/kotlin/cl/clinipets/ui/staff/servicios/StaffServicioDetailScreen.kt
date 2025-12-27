package cl.clinipets.ui.staff.servicios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.ServicioMedicoDto
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffServicioDetailScreen(
    servicioId: String,
    onBack: () -> Unit,
    viewModel: StaffServicioDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedIds by remember { mutableStateOf(setOf<UUID>()) }

    LaunchedEffect(servicioId) {
        viewModel.cargarServicio(servicioId)
    }

    LaunchedEffect(uiState) {
        (uiState as? DetailUiState.Success)?.let {
            selectedIds = it.servicio.serviciosRequeridosIds
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    (uiState as? DetailUiState.Success)?.let { Text(it.servicio.nombre) } ?: Text("Detalle de Servicio")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (uiState is DetailUiState.Success) {
                        IconButton(onClick = { viewModel.guardarDependencias(servicioId, selectedIds) }) {
                            Icon(Icons.Default.Save, contentDescription = "Guardar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is DetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DetailUiState.Error -> {
                    Text(state.mensaje, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
                is DetailUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Dependencias del Servicio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Selecciona los servicios que deben realizarse antes de este.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.disponibles) { disponible ->
                                val isChecked = selectedIds.contains(disponible.id)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked) selectedIds + disponible.id else selectedIds - disponible.id
                                        }
                                    )
                                    Text(disponible.nombre, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
