package cl.clinipets.ui.mascotas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.CitaDetalladaResponse
import cl.clinipets.openapi.models.FichaResponse
import cl.clinipets.openapi.models.MascotaResponse
import cl.clinipets.ui.mascotas.gallery.PetGalleryContent
import cl.clinipets.ui.util.toLocalDateStr
import cl.clinipets.ui.util.toLocalHour
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onBookAppointment: (String) -> Unit,
    viewModel: PetDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Mascota") },
            text = { Text("¿Estás seguro? Esta acción es irreversible y eliminará el historial de esta mascota.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.eliminarMascota()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil de mascota") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete, 
                            contentDescription = "Eliminar mascota",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null || uiState.pet == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "No pudimos cargar la mascota")
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            else -> {
                uiState.pet?.let { pet ->
                    val tabTitles = listOf("Citas", "Fichas Médicas", "Galería")
                    var selectedTab by remember { mutableStateOf(0) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PetHeader(pet)
                        TabRow(selectedTabIndex = selectedTab) {
                            tabTitles.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                        ActionButtons(
                            onEdit = { onEdit(pet.id.toString()) },
                            onBook = { onBookAppointment(pet.id.toString()) }
                        )
                        when (selectedTab) {
                            0 -> PetHistory(
                                history = uiState.history,
                                speciesColor = speciesColor(pet.especie)
                            )

                            else -> ClinicalHistory(
                                records = uiState.clinicalRecords
                            )
                        }

                        // Tab Content for Gallery (Index 2)
                        if (selectedTab == 2) {
                            Box(modifier = Modifier.height(500.dp).fillMaxWidth()) {
                                PetGalleryContent(petId = pet.id.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PetHeader(pet: MascotaResponse) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(speciesColor(pet.especie).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EventAvailable,
                    contentDescription = null,
                    tint = speciesColor(pet.especie),
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(
                text = pet.nombre,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Raza y Especie
            Text(
                text = "${pet.especie.name} • ${pet.raza}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Datos básicos (Sexo, Peso)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sexo
                DetailChip(label = pet.sexo.name)
                
                // Peso
                DetailChip(label = "${pet.pesoActual.toPlainString()} kg")
            }
            
            // Estados Clínicos (Nueva sección)
            if (pet.testRetroviralNegativo || pet.esterilizado || !pet.chipIdentificador.isNullOrBlank()) {
                OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Test Viral Negativo
                    if (pet.testRetroviralNegativo) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Test Viral (-)") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Check, 
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50) 
                                ) 
                            },
                            colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                labelColor = Color(0xFF4CAF50)
                            )
                        )
                    }

                    // Esterilizado
                    if (pet.esterilizado) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Esterilizado") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.ContentCut, 
                                    contentDescription = null 
                                ) 
                            },
                            colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            border = null
                        )
                    }

                    // Chip Identificador
                    if (!pet.chipIdentificador.isNullOrBlank()) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Chip: ${pet.chipIdentificador}") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.QrCode, 
                                    contentDescription = null 
                                ) 
                            }
                        )
                    }
                }
            }

            // Temperamento Warning
            if (pet.temperamento == MascotaResponse.Temperamento.NERVIOSO || 
                pet.temperamento == MascotaResponse.Temperamento.AGRESIVO) {
                
                val color = if (pet.temperamento == MascotaResponse.Temperamento.AGRESIVO) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.tertiary

                Card(
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable, // TODO: Use Warning icon if available
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CUIDADO: ${pet.temperamento.name}",
                            color = color,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailChip(label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ActionButtons(
    onEdit: () -> Unit,
    onBook: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Editar perfil")
        }
        Button(
            onClick = onBook,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Agendar cita")
        }
    }
}

@Composable
private fun PetHistory(
    history: List<CitaDetalladaResponse>,
    speciesColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Historial de citas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (history.isEmpty()) {
            Text(
                text = "Aún no hay atenciones registradas.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            history.forEachIndexed { index, cita ->
                TimelineItem(
                    cita = cita,
                    speciesColor = speciesColor,
                    isLast = index == history.lastIndex
                )
            }
        }
    }
}

@Composable
private fun ClinicalHistory(records: List<FichaResponse>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Fichas médicas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (records.isEmpty()) {
            Text(
                text = "Aún no hay fichas clínicas registradas.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            records.forEach { record ->
                ClinicalRecordCard(record = record)
            }
        }
    }
}

@Composable
private fun ClinicalRecordCard(record: FichaResponse) {
    var expanded by remember { mutableStateOf(false) }
    val dateText = record.fechaAtencion.toLocalDateStr()
    val friendlyDate = dateText.split(" ").joinToString(" ") { part ->
        if (part.any { it.isLetter() }) {
            part.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        } else {
            part
        }
    }
    val badgeText = if (record.esVacuna) "Vacuna" else "Consulta"
    val badgeIcon = if (record.esVacuna) Icons.Default.Vaccines else Icons.Default.MedicalServices

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = friendlyDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = record.motivoConsulta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { },
                    label = { Text(badgeText) },
                    leadingIcon = {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = null
                )
            }

            record.diagnostico?.takeIf { it.isNotBlank() }?.let { diagnostico ->
                ClinicalSection(
                    icon = Icons.Default.MedicalServices,
                    label = "Diagnóstico",
                    content = diagnostico,
                    highlight = true
                )
            }

            record.tratamiento?.takeIf { it.isNotBlank() }?.let { tratamiento ->
                ClinicalSection(
                    icon = Icons.Default.Medication,
                    label = "Tratamiento",
                    content = tratamiento,
                    highlight = true
                )
            }

            record.pesoRegistrado?.let { peso ->
                ClinicalSection(
                    icon = Icons.Default.MonitorWeight,
                    label = "Peso",
                    content = "${String.format(Locale.getDefault(), "%.1f", peso)} kg"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    record.anamnesis?.takeIf { it.isNotBlank() }?.let { anamnesis ->
                        ClinicalSection(
                            icon = Icons.Default.Info,
                            label = "Anamnesis",
                            content = anamnesis
                        )
                    }
                    record.examenFisico?.takeIf { it.isNotBlank() }?.let { examen ->
                        ClinicalSection(
                            icon = Icons.Default.Info,
                            label = "Examen Físico",
                            content = examen
                        )
                    }
                    record.observaciones?.takeIf { it.isNotBlank() }?.let { obs ->
                        ClinicalSection(
                            icon = Icons.Default.Info,
                            label = "Observaciones",
                            content = obs
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Ocultar detalles" else "Ver anamnesis y examen",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ClinicalSection(
    icon: ImageVector,
    label: String,
    content: String,
    highlight: Boolean = false
) {
    val shape = MaterialTheme.shapes.small
    val background = if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
    val textColor = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

@Composable
private fun TimelineItem(
    cita: CitaDetalladaResponse,
    speciesColor: Color,
    isLast: Boolean
) {
    val dateText = cita.fechaHoraInicio.toLocalDateStr()
    val hourText = cita.fechaHoraInicio.toLocalHour()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.width(72.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = hourText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(speciesColor)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Display all services in the appointment
                cita.detalles.forEach { detalle ->
                    Text(
                        text = detalle.nombreServicio,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                AssistChip(
                    onClick = { },
                    enabled = false,
                    label = { Text(cita.estado.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = speciesColor
                        )
                    }
                )
            }
        }
    }
}
