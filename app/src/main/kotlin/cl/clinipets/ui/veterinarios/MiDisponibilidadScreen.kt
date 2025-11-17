@file:Suppress("UNUSED_VALUE")
package cl.clinipets.ui.veterinarios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.ExcepcionHorario
import cl.clinipets.openapi.models.HorarioAtencion
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val dayCardShape = RoundedCornerShape(24.dp)
private val intervalShape = RoundedCornerShape(16.dp)

// Estado de Diálogos: Incluye Crear y Editar
private sealed interface DialogState {
    data object None : DialogState

    // Crear
    data class AddInterval(val dia: HorarioAtencion.DiaSemana, val weekly: Boolean) : DialogState
    data object AddExceptionClose : DialogState
    data object AddExceptionRange : DialogState

    // Editar
    data class EditInterval(val horario: HorarioAtencion) : DialogState
    data class EditException(val excepcion: ExcepcionHorario) : DialogState
}

// Target del TimePicker
private enum class TimePickerTarget {
    START, END
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MiDisponibilidadScreen(
    onBack: () -> Unit,
    viewModel: MiDisponibilidadViewModel = hiltViewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var dialogState by remember { mutableStateOf<DialogState>(DialogState.None) }
    val dateFmt =
        remember { DateTimeFormatter.ofPattern("EEEE d 'de' MMM, yyyy").withLocale(Locale("es")) }

    // Estado para las Pestañas
    val pagerState = rememberPagerState { 2 } // 2 pestañas
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("Horario Semanal", "Excepciones")

    LaunchedEffect(Unit) { viewModel.cargarTodo(null) }

    // Observador del Snackbar
    LaunchedEffect(ui.userMessage) {
        ui.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disponibilidad del Veterinario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) { padding ->
        // Superficie principal con bordes redondeados
        Surface(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Pestañas (Tabs)
                TabRow(
                    selectedTabIndex = pagerState.currentPage, containerColor = Color.Transparent
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = pagerState.currentPage == index, onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }, text = { Text(title) })
                    }
                }

                // 2. Pager (Contenedor deslizable)
                HorizontalPager(
                    state = pagerState, modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        // --- PÁGINA 0: HORARIO SEMANAL ---
                        0 -> HorarioSemanalPage(
                            ui = ui,
                            viewModel = viewModel,
                            onAddIntervalClick = { dia, isWeekly ->
                                dialogState = DialogState.AddInterval(dia, isWeekly)
                            },
                            onEditIntervalClick = { horario ->
                                dialogState = DialogState.EditInterval(horario)
                            })
                        // --- PÁGINA 1: EXCEPCIONES ---
                        1 -> ExcepcionesPage(
                            ui = ui,
                            viewModel = viewModel,
                            dateFmt = dateFmt,
                            onAddExceptionCloseClick = {
                                dialogState = DialogState.AddExceptionClose
                            },
                            onAddExceptionRangeClick = {
                                dialogState = DialogState.AddExceptionRange
                            },
                            onEditExceptionClick = { excepcion ->
                                dialogState = DialogState.EditException(excepcion)
                            })
                    }
                }
            }
        }

        // --- Diálogos (Sección actualizada con Edit) ---
        if (ui.cargando) {
            Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }

        when (val state = dialogState) {
            is DialogState.None -> {}

            // --- Diálogos de CREACIÓN ---
            is DialogState.AddInterval -> {
                var horaInicio by remember { mutableStateOf(LocalTime.of(9, 0)) }
                var horaFin by remember { mutableStateOf(LocalTime.of(13, 0)) }
                var timePickerTarget by remember { mutableStateOf<TimePickerTarget?>(null) }

                AlertDialog(onDismissRequest = { dialogState = DialogState.None }, confirmButton = {
                    TextButton(onClick = {
                        if (state.weekly) {
                            viewModel.crearIntervaloSemanal(horaInicio, horaFin)
                        } else {
                            // Llama a la función simple (sin fallback raro)
                            viewModel.crearIntervalo(state.dia, horaInicio, horaFin, null)
                        }
                        dialogState = DialogState.None
                    }) { Text("Guardar") }
                }, dismissButton = {
                    TextButton(onClick = {
                        dialogState = DialogState.None
                    }) { Text("Cancelar") }
                }, title = {
                    val title = if (state.weekly) "Nuevo intervalo semanal" else "Nuevo intervalo ${
                        nombreDia(state.dia)
                    }"
                    Text(title)
                }, text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TimePickerRow(
                            label = "Inicio",
                            time = horaInicio,
                            onClick = { timePickerTarget = TimePickerTarget.START })
                        TimePickerRow(
                            label = "Fin",
                            time = horaFin,
                            onClick = { timePickerTarget = TimePickerTarget.END })
                    }
                })

                if (timePickerTarget != null) {
                    val target = timePickerTarget!!
                    val (initialTime, onConfirm) = when (target) {
                        TimePickerTarget.START -> horaInicio to { time: LocalTime ->
                            horaInicio = time
                        }

                        TimePickerTarget.END -> horaFin to { time: LocalTime -> horaFin = time }
                    }
                    SimpleTimePickerDialog(
                        initialTime = initialTime,
                        onDismiss = { timePickerTarget = null },
                        onConfirm = { newTime ->
                            onConfirm(newTime)
                            timePickerTarget = null
                        })
                }
            }

            is DialogState.AddExceptionClose -> {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { dialogState = DialogState.None },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = millisToLocalDate(millis)
                                viewModel.crearExcepcionCierre(date)
                            }
                            dialogState = DialogState.None
                        }) { Text("Crear") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            dialogState = DialogState.None
                        }) { Text("Cancelar") }
                    }) {
                    DatePicker(
                        state = datePickerState, title = { Text("Selecciona el día a cerrar") })
                }
            }

            is DialogState.AddExceptionRange -> {
                val datePickerState = rememberDatePickerState(Instant.now().toEpochMilli())
                var horaInicio by remember { mutableStateOf(LocalTime.of(14, 0)) }
                var horaFin by remember { mutableStateOf(LocalTime.of(16, 0)) }
                var timePickerTarget by remember { mutableStateOf<TimePickerTarget?>(null) }

                AlertDialog(onDismissRequest = { dialogState = DialogState.None }, confirmButton = {
                    TextButton(onClick = {
                        val date = datePickerState.selectedDateMillis?.let { millisToLocalDate(it) }
                        if (date != null) {
                            viewModel.crearExcepcionRango(date, horaInicio, horaFin)
                            dialogState = DialogState.None
                        }
                    }) { Text("Crear") }
                }, dismissButton = {
                    TextButton(onClick = {
                        dialogState = DialogState.None
                    }) { Text("Cancelar") }
                }, title = { Text("Excepción (rango horario)") }, text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DatePicker(
                            state = datePickerState, title = { Text("Selecciona la fecha") })
                        TimePickerRow(
                            label = "Inicio",
                            time = horaInicio,
                            onClick = { timePickerTarget = TimePickerTarget.START })
                        TimePickerRow(
                            label = "Fin",
                            time = horaFin,
                            onClick = { timePickerTarget = TimePickerTarget.END })
                    }
                })

                if (timePickerTarget != null) {
                    val target = timePickerTarget!!
                    val (initialTime, onConfirm) = when (target) {
                        TimePickerTarget.START -> horaInicio to { time: LocalTime ->
                            horaInicio = time
                        }

                        TimePickerTarget.END -> horaFin to { time: LocalTime -> horaFin = time }
                    }
                    SimpleTimePickerDialog(
                        initialTime = initialTime,
                        onDismiss = { timePickerTarget = null },
                        onConfirm = { newTime ->
                            onConfirm(newTime)
                            timePickerTarget = null
                        })
                }
            }

            // --- Diálogos de EDICIÓN ---
            is DialogState.EditInterval -> {
                val horario = state.horario
                var horaInicio by remember { mutableStateOf(LocalTime.parse(horario.horaInicio)) }
                var horaFin by remember { mutableStateOf(LocalTime.parse(horario.horaFin)) }
                var timePickerTarget by remember { mutableStateOf<TimePickerTarget?>(null) }

                AlertDialog(
                    onDismissRequest = { dialogState = DialogState.None },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.actualizarHorario(
                                horario.id!!, horario.diaSemana!!, horaInicio, horaFin
                            )
                            dialogState = DialogState.None
                        }) { Text("Actualizar") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            dialogState = DialogState.None
                        }) { Text("Cancelar") }
                    },
                    title = { Text("Editar intervalo ${nombreDia(horario.diaSemana!!)}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            TimePickerRow(
                                label = "Inicio",
                                time = horaInicio,
                                onClick = { timePickerTarget = TimePickerTarget.START })
                            TimePickerRow(
                                label = "Fin",
                                time = horaFin,
                                onClick = { timePickerTarget = TimePickerTarget.END })
                        }
                    })

                if (timePickerTarget != null) {
                    val target = timePickerTarget!!
                    val (initialTime, onConfirm) = when (target) {
                        TimePickerTarget.START -> horaInicio to { time: LocalTime ->
                            horaInicio = time
                        }

                        TimePickerTarget.END -> horaFin to { time: LocalTime -> horaFin = time }
                    }
                    SimpleTimePickerDialog(
                        initialTime = initialTime,
                        onDismiss = { timePickerTarget = null },
                        onConfirm = { newTime ->
                            onConfirm(newTime)
                            timePickerTarget = null
                        })
                }
            }

            is DialogState.EditException -> {
                val ex = state.excepcion
                // Si es 'cerrado', solo necesitamos un DatePicker
                if (ex.cerrado == true) {
                    val datePickerState = rememberDatePickerState(
                        ex.fecha!!.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                    DatePickerDialog(
                        onDismissRequest = { dialogState = DialogState.None },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val date = millisToLocalDate(millis)
                                    viewModel.actualizarExcepcion(
                                        ex.id!!, date, null, null, true, ex.motivo ?: "Cerrado"
                                    )
                                }
                                dialogState = DialogState.None
                            }) { Text("Actualizar") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                dialogState = DialogState.None
                            }) { Text("Cancelar") }
                        }) {
                        DatePicker(
                            state = datePickerState, title = { Text("Editar fecha de cierre") })
                    }
                }
                // Si es 'rango', necesitamos DatePicker y TimePickers
                else {
                    val datePickerState = rememberDatePickerState(
                        ex.fecha!!.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                    var horaInicio by remember { mutableStateOf(LocalTime.parse(ex.horaInicio)) }
                    var horaFin by remember { mutableStateOf(LocalTime.parse(ex.horaFin)) }
                    var timePickerTarget by remember { mutableStateOf<TimePickerTarget?>(null) }

                    AlertDialog(
                        onDismissRequest = { dialogState = DialogState.None },
                        confirmButton = {
                            TextButton(onClick = {
                                val date =
                                    datePickerState.selectedDateMillis?.let { millisToLocalDate(it) }
                                        ?: ex.fecha!!
                                viewModel.actualizarExcepcion(
                                    ex.id!!,
                                    date,
                                    horaInicio,
                                    horaFin,
                                    false,
                                    ex.motivo ?: "Excepción"
                                )
                                dialogState = DialogState.None
                            }) { Text("Actualizar") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                dialogState = DialogState.None
                            }) { Text("Cancelar") }
                        },
                        title = { Text("Editar excepción") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                DatePicker(
                                    state = datePickerState, title = { Text("Editar fecha") })
                                TimePickerRow(
                                    label = "Inicio",
                                    time = horaInicio,
                                    onClick = { timePickerTarget = TimePickerTarget.START })
                                TimePickerRow(
                                    label = "Fin",
                                    time = horaFin,
                                    onClick = { timePickerTarget = TimePickerTarget.END })
                            }
                        })

                    if (timePickerTarget != null) {
                        val target = timePickerTarget!!
                        val (initialTime, onConfirm) = when (target) {
                            TimePickerTarget.START -> horaInicio to { time: LocalTime ->
                                horaInicio = time
                            }

                            TimePickerTarget.END -> horaFin to { time: LocalTime -> horaFin = time }
                        }
                        SimpleTimePickerDialog(
                            initialTime = initialTime,
                            onDismiss = { timePickerTarget = null },
                            onConfirm = { newTime ->
                                onConfirm(newTime)
                                timePickerTarget = null
                            })
                    }
                }
            }
        }
    }
}

// --- PÁGINA 0 (Horario Semanal) ---
@Composable
private fun HorarioSemanalPage(
    ui: MiDisponibilidadUiState,
    viewModel: MiDisponibilidadViewModel,
    onAddIntervalClick: (dia: HorarioAtencion.DiaSemana, isWeekly: Boolean) -> Unit,
    onEditIntervalClick: (horario: HorarioAtencion) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item("section_horario_title") {
            Text("Horario semanal", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Define tus horarios regulares por día. Estos intervalos se repiten semanalmente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Usamos estos horarios para agendar citas. Puedes editarlos en cualquier momento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }


        val days = HorarioAtencion.DiaSemana.entries
        days.forEach { dia ->
            val intervals = ui.horarios.filter { it.diaSemana == dia }
            item(key = "day_${dia.name}") {
                DayScheduleCard(
                    dia = dia,
                    intervals = intervals,
                    onDelete = { h -> viewModel.eliminarIntervalo(h, null) },
                    onAddClick = { onAddIntervalClick(dia, false) },
                    onAddMorning = { viewModel.plantillaMananaDia(dia) },
                    onAddAfternoon = { viewModel.plantillaTardeDia(dia) },
                    onEditClick = onEditIntervalClick // Pasa el evento
                )
            }
        }
        item("section_horario_actions") {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.plantillaMananaSemana() },
                    shape = RoundedCornerShape(24.dp)
                ) { Text("Mañana (9-13) toda la semana") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.plantillaTardeSemana() },
                    shape = RoundedCornerShape(24.dp)
                ) { Text("Tarde (15-19) toda la semana") }
            }

        }
        item("section_horario_add_global") {
            OutlinedButton(
                onClick = { onAddIntervalClick(HorarioAtencion.DiaSemana.MONDAY, true) },
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar intervalo semanal (todos los días)")
            }
        }
        item("bottom_spacer") { Spacer(Modifier.height(40.dp)) }
    }
}

// --- PÁGINA 1 (Excepciones) ---
@Composable
private fun ExcepcionesPage(
    ui: MiDisponibilidadUiState,
    viewModel: MiDisponibilidadViewModel,
    dateFmt: DateTimeFormatter,
    onAddExceptionCloseClick: () -> Unit,
    onAddExceptionRangeClick: () -> Unit,
    onEditExceptionClick: (excepcion: ExcepcionHorario) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item("section_ex_title") {
            Text("Excepciones de agenda", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Usa excepciones para días específicos: marca un día completo como cerrado o bloquea un rango horario.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item("section_ex_actions") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAddExceptionCloseClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) { Text("Marcar día como CERRADO…") }
                OutlinedButton(
                    onClick = onAddExceptionRangeClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) { Text("Agregar excepción (rango horario)…") }
            }
        }

        item("divider_1") { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        val excepcionesOrdenadas =
            ui.excepciones.sortedWith(compareBy({ it.fecha }, { it.horaInicio ?: "" }))
        if (excepcionesOrdenadas.isEmpty()) {
            item("ex_empty") {
                Text(
                    "Aún no has creado excepciones",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(excepcionesOrdenadas, key = { it.id ?: UUID.randomUUID() }) { ex ->
                ExcepcionCard(
                    ex = ex,
                    fechaTexto = ex.fecha?.format(dateFmt) ?: "",
                    onDelete = { ex.id?.let { id -> viewModel.eliminarExcepcion(id, null) } },
                    onClick = { onEditExceptionClick(ex) } // Pasa el evento
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        item("bottom_spacer") { Spacer(Modifier.height(40.dp)) }
    }
}


// --- Composables Privados (Actualizados con Click) ---

private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun DayScheduleCard(
    dia: HorarioAtencion.DiaSemana,
    intervals: List<HorarioAtencion>,
    onDelete: (HorarioAtencion) -> Unit,
    onAddClick: () -> Unit,
    onAddMorning: () -> Unit,
    onAddAfternoon: () -> Unit,
    onEditClick: (HorarioAtencion) -> Unit
) {
    Card(
        shape = dayCardShape, colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                2.dp
            )
        ), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = nombreDia(dia),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onAddMorning, shape = RoundedCornerShape(20.dp)
                    ) { Text("Mañana") }
                    OutlinedButton(
                        onClick = onAddAfternoon, shape = RoundedCornerShape(20.dp)
                    ) { Text("Tarde") }
                    IconButton(onClick = onAddClick) {
                        Icon(
                            Icons.Default.Add, contentDescription = "Agregar intervalo"
                        )
                    }
                }
            }
            if (intervals.isEmpty()) {
                Text(
                    "Sin intervalos",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                intervals.sortedBy { it.horaInicio }.forEach { h ->
                    IntervalRow(
                        h = h,
                        onDelete = onDelete,
                        onClick = { onEditClick(h) } // Pasa el click
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalRow(
    h: HorarioAtencion, onDelete: (HorarioAtencion) -> Unit, onClick: () -> Unit
) {
    Card(
        shape = intervalShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                1.dp
            )
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable(onClick = onClick) // Fila clicable
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${h.horaInicio} - ${h.horaFin}", style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = { onDelete(h) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ExcepcionCard(
    ex: ExcepcionHorario, fechaTexto: String, onDelete: () -> Unit, onClick: () -> Unit
) {
    val texto = if (ex.cerrado == true) {
        "CERRADO — $fechaTexto"
    } else {
        val rango = "${ex.horaInicio} - ${ex.horaFin}"
        "$fechaTexto — $rango (${ex.motivo ?: "Excepción"})"
    }
    Card(
        shape = intervalShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                2.dp
            )
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Fila clicable
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(texto, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// --- Composables de TimePicker (sin cambios) ---

@Composable
private fun TimePickerRow(
    label: String, time: LocalTime, onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(8.dp)) {
            Text(
                time.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTimePickerDialog(
    initialTime: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour, initialMinute = initialTime.minute, is24Hour = true
    )
    TimePickerDialog(onDismissRequest = onDismiss, confirmButton = {
        TextButton(onClick = {
            val newTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
            onConfirm(newTime)
        }) { Text("OK") }
    }, dismissButton = {
        TextButton(onClick = onDismiss) { Text("Cancelar") }
    }, title = { Text("Selecciona la hora") }) {
        TimePicker(state = timePickerState)
    }
}


private fun nombreDia(dia: HorarioAtencion.DiaSemana): String = when (dia) {
    HorarioAtencion.DiaSemana.MONDAY -> "Lunes"
    HorarioAtencion.DiaSemana.TUESDAY -> "Martes"
    HorarioAtencion.DiaSemana.WEDNESDAY -> "Miércoles"
    HorarioAtencion.DiaSemana.THURSDAY -> "Jueves"
    HorarioAtencion.DiaSemana.FRIDAY -> "Viernes"
    HorarioAtencion.DiaSemana.SATURDAY -> "Sábado"
    HorarioAtencion.DiaSemana.SUNDAY -> "Domingo"
}