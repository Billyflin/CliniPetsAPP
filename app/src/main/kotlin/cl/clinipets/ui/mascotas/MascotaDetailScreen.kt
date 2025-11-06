package cl.clinipets.ui.mascotas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MascotaDetailScreen(
    mascotaId: UUID,
    onBack: () -> Unit,
    onEdit: (UUID) -> Unit,
    vm: MascotasViewModel = hiltViewModel()
) {
    val cargando by vm.cargando.collectAsState()
    val mascota by vm.seleccionada.collectAsState()
    val error by vm.error.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    val handleBack = {
        vm.limpiarSeleccion()
        onBack()
    }

    LaunchedEffect(mascotaId) {
        vm.detalle(id = mascotaId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar a ${mascota?.nombre}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        vm.eliminar(mascotaId)
                        handleBack() // Volver después de eliminar
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mascota?.nombre ?: "Detalle de Mascota") },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                cargando && mascota == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                mascota == null -> {
                    Text(
                        "No se encontró información de la mascota.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                else -> {
                    val m = mascota!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Tarjeta Principal
                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = m.nombre,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    InfoItem("Especie", m.especie.value)
                                    InfoItem("Raza", m.raza?.nombre ?: "Mestizo")
                                    m.sexo?.let { InfoItem("Sexo", it.value) }
                                }
                            }
                        }

                        // Tarjeta de Atributos
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Atributos", style = MaterialTheme.typography.titleMedium)

                                    // --- INICIO CORRECCIÓN ---
                                    // m.fechaNacimiento ahora es LocalDate?, no String?
                                    m.fechaNacimiento?.let { fecha ->
                                        val edad = Period.between(fecha, LocalDate.now())
                                        val aprox = if (m.esFechaAproximada) " (aprox.)" else ""
                                        val edadStr = "${edad.years} años, ${edad.months} meses$aprox"
                                        val fechaStr = fecha.format(DateTimeFormatter.ISO_LOCAL_DATE)

                                        InfoItem("Edad", edadStr)
                                        InfoItem("F. Nacimiento", "$fechaStr$aprox")
                                    }
                                    // --- FIN CORRECCIÓN ---

                                    m.pesoKg?.let { InfoItem("Peso", "$it kg") }
                                    m.pelaje?.let { InfoItem("Pelaje", it.value) }
                                    m.patron?.let { InfoItem("Patrón", it.value) }

                                    if (m.colores.isNotEmpty()) {
                                        Text(
                                            "Colores",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            m.colores.forEach { color ->
                                                AssistChip(onClick = { }, label = { Text(color.value) })
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Acciones
                        item {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { onEdit(mascotaId) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Editar") }

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Eliminar Mascota") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}