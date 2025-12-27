package cl.clinipets.ui.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.BloqueoAgenda
import cl.clinipets.openapi.models.CitaDetalladaResponse
import cl.clinipets.openapi.models.ResumenDiarioResponse
import cl.clinipets.ui.util.toLocalHour
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAgendaScreen(
    onProfileClick: () -> Unit = {},
    onCitaClick: (String, String) -> Unit = { _, _ -> },
    onServiciosClick: () -> Unit = {},
    onInventarioClick: () -> Unit = {},
    viewModel: StaffAgendaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var showBlockDialog by remember { mutableStateOf(false) }
    val lastUpdatedFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "CliniPets Admin",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                NavigationDrawerItem(
                    label = { Text("Agenda") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.MedicalServices, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Servicios") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onServiciosClick() 
                    },
                    icon = { Icon(Icons.Default.MedicalServices, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Inventario") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onInventarioClick() 
                    },
                    icon = { Icon(Icons.Default.Inventory, null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Staff Agenda", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    },
                    actions = {
                        IconButton(onClick = onProfileClick) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Perfil")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showBlockDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo bloqueo")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Selector de Fecha
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.cambiarFecha(state.date.minusDays(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Anterior")
                    }
                    Text(
                        text = state.date.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.cambiarFecha(state.date.plusDays(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Siguiente")
                    }
                }

                val lastUpdatedText = state.lastUpdated?.format(lastUpdatedFormatter) ?: "--:--:--"
                Text(
                    text = "Última actualización: $lastUpdatedText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.refresh() },
                    state = pullRefreshState,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.agendaItems.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Agenda libre", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val enAtencion = state.agendaItems.filterIsInstance<ItemCita>().filter {
                                it.data.estado == CitaDetalladaResponse.Estado.EN_ATENCION
                            }
                            
                            if (enAtencion.isNotEmpty()) {
                                item {
                                    Text("EN ATENCIÓN", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                                items(enAtencion) { item ->
                                    AppointmentItem(
                                        cita = item.data,
                                        onClick = { 
                                            val mId = item.data.detalles.firstOrNull()?.mascotaId?.toString() ?: ""
                                            onCitaClick(item.data.id.toString(), mId) 
                                        },
                                        onTriageClick = { viewModel.cambiarEstadoCita(item.data.id, CitaDetalladaResponse.Estado.EN_ATENCION) }
                                    )
                                }
                            }

                            state.resumen?.let { item { DailySummaryCard(resumen = it) } }
                            
                            item {
                                Text("AGENDA DEL DÍA", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }

                            items(state.agendaItems) { item ->
                                when (item) {
                                    is ItemCita -> {
                                        if (item.data.estado != CitaDetalladaResponse.Estado.EN_ATENCION) {
                                            AppointmentItem(
                                                cita = item.data, 
                                                onClick = { 
                                                    val mId = item.data.detalles.firstOrNull()?.mascotaId?.toString() ?: ""
                                                    onCitaClick(item.data.id.toString(), mId) 
                                                },
                                                onTriageClick = { viewModel.cambiarEstadoCita(item.data.id, CitaDetalladaResponse.Estado.EN_ATENCION) }
                                            )
                                        }
                                    }
                                    is ItemBloqueo -> BlockItem(
                                        bloqueo = item.data,
                                        onDelete = { item.data.id?.let { viewModel.eliminarBloqueo(it) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBlockDialog) {
        NuevoBloqueoDialog(
            onDismiss = { showBlockDialog = false },
            onConfirm = { inicio, fin, motivo ->
                viewModel.crearBloqueo(inicio, fin, motivo)
                showBlockDialog = false
            }
        )
    }
}

@Composable
fun AppointmentItem(
    cita: CitaDetalladaResponse, 
    onClick: () -> Unit,
    onTriageClick: () -> Unit = {}
) {
    val isEnAtencion = cita.estado == CitaDetalladaResponse.Estado.EN_ATENCION
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEnAtencion) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnAtencion) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = cita.fechaHoraInicio.toLocalHour(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isEnAtencion) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
                Text("hrs", style = MaterialTheme.typography.labelSmall)
            }

            Box(Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant))

            Column(modifier = Modifier.weight(1f)) {
                val detalle = cita.detalles.firstOrNull()
                Text(detalle?.nombreMascota ?: "Mascota", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(detalle?.nombreServicio ?: "Servicio", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                
                if (cita.estado == CitaDetalladaResponse.Estado.CONFIRMADA) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onTriageClick,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Atender", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            StatusChip(cita.estado)
        }
    }
}

@Composable
fun StatusChip(estado: CitaDetalladaResponse.Estado) {
    val color = when (estado) {
        CitaDetalladaResponse.Estado.EN_ATENCION -> Color(0xFFFF9800)
        CitaDetalladaResponse.Estado.CONFIRMADA -> Color(0xFF2196F3)
        CitaDetalladaResponse.Estado.FINALIZADA -> Color(0xFF4CAF50)
        CitaDetalladaResponse.Estado.CANCELADA -> Color.Gray
        CitaDetalladaResponse.Estado.NO_ASISTIO -> Color.Red
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = estado.name,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DailySummaryCard(resumen: ResumenDiarioResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Resumen de Caja", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("Citas Total", resumen.totalCitas.toString())
                SummaryItem("Finalizadas", resumen.citasFinalizadas.toString())
                SummaryItem("Recaudado", "$${resumen.recaudacionTotalRealizada}")
                SummaryItem("Pendiente", "$${resumen.proyeccionPendiente}")
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BlockItem(bloqueo: BloqueoAgenda, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("BLOQUEO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(bloqueo.motivo ?: "Sin motivo", style = MaterialTheme.typography.bodyMedium)
                Text("${bloqueo.fechaHoraInicio.toLocalHour()} - ${bloqueo.fechaHoraFin.toLocalHour()}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoBloqueoDialog(onDismiss: () -> Unit, onConfirm: (LocalTime, LocalTime, String?) -> Unit) {
    var motivo by remember { mutableStateOf("") }
    val now = LocalTime.now()
    val startState = remember { TimePickerState(now.hour, now.minute, true) }
    val endState = remember { TimePickerState(now.plusHours(1).hour % 24, now.minute, true) }
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(startState.hour, startState.minute), LocalTime.of(endState.hour, endState.minute), motivo) }) { Text("Bloquear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Nuevo Bloqueo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showStart = true }) { Text("Inicio: ${startState.hour}:${startState.minute}") }
                Button(onClick = { showEnd = true }) { Text("Fin: ${endState.hour}:${endState.minute}") }
                OutlinedTextField(value = motivo, onValueChange = { motivo = it }, label = { Text("Motivo") })
            }
        }
    )
    if (showStart) AlertDialog(onDismissRequest = { showStart = false }, confirmButton = { TextButton(onClick = { showStart = false }) { Text("OK") } }, text = { TimePicker(state = startState) })
    if (showEnd) AlertDialog(onDismissRequest = { showEnd = false }, confirmButton = { TextButton(onClick = { showEnd = false }) { Text("OK") } }, text = { TimePicker(state = endState) })
}
