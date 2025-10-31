package cl.clinipets.feature.veterinario.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.CrearExcepcion
import cl.clinipets.openapi.models.CrearReglaSemanal
import cl.clinipets.openapi.models.ReglaSemanal
import cl.clinipets.openapi.models.ExcepcionDisponibilidad
import cl.clinipets.openapi.models.BloqueHorario
import java.util.UUID

@Composable
fun VeterinarioAgendaRoute(
    onBack: () -> Unit,
    viewModel: VeterinarioAgendaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsState()
    val reglaForm by viewModel.reglaForm.collectAsState()
    val excepcionForm by viewModel.excepcionForm.collectAsState()

    VeterinarioAgendaScreen(
        estado = estado,
        reglaForm = reglaForm,
        excepcionForm = excepcionForm,
        onBack = onBack,
        onReglaDiaChange = viewModel::onReglaDiaChange,
        onReglaHoraInicioChange = viewModel::onReglaHoraInicioChange,
        onReglaHoraFinChange = viewModel::onReglaHoraFinChange,
        onCrearRegla = viewModel::crearRegla,
        onEliminarRegla = viewModel::eliminarRegla,
        onExcepcionFechaChange = viewModel::onExcepcionFechaChange,
        onExcepcionTipoChange = viewModel::onExcepcionTipoChange,
        onExcepcionHoraInicioChange = viewModel::onExcepcionHoraInicioChange,
        onExcepcionHoraFinChange = viewModel::onExcepcionHoraFinChange,
        onExcepcionMotivoChange = viewModel::onExcepcionMotivoChange,
        onCrearExcepcion = viewModel::crearExcepcion,
        onFechaDisponibilidadChange = viewModel::onFechaDisponibilidadChange,
        onConsultarDisponibilidad = viewModel::consultarDisponibilidad,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VeterinarioAgendaScreen(
    estado: VeterinarioAgendaUiState,
    reglaForm: ReglaFormState,
    excepcionForm: ExcepcionFormState,
    onBack: () -> Unit,
    onReglaDiaChange: (CrearReglaSemanal.DiaSemana) -> Unit,
    onReglaHoraInicioChange: (String) -> Unit,
    onReglaHoraFinChange: (String) -> Unit,
    onCrearRegla: () -> Unit,
    onEliminarRegla: (UUID) -> Unit,
    onExcepcionFechaChange: (String) -> Unit,
    onExcepcionTipoChange: (CrearExcepcion.Tipo) -> Unit,
    onExcepcionHoraInicioChange: (String) -> Unit,
    onExcepcionHoraFinChange: (String) -> Unit,
    onExcepcionMotivoChange: (String) -> Unit,
    onCrearExcepcion: () -> Unit,
    onFechaDisponibilidadChange: (String) -> Unit,
    onConsultarDisponibilidad: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Agenda veterinaria") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            estado.cargandoPerfil -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LinearProgressIndicator()
                    Text(text = "Cargando tu agenda…", modifier = Modifier.padding(top = 16.dp))
                }
            }

            estado.veterinarioId == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ErrorMensaje(
                        estado.error ?: Resultado.Error(Resultado.Tipo.DESCONOCIDO, "No encontramos tu perfil."),
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    item { AgendaHeader() }
                    estado.error?.let { error ->
                        item { ErrorMensaje(error) }
                    }
                    item {
                        FormReglaSemanal(
                            form = reglaForm,
                            guardando = estado.guardando,
                            onDiaChange = onReglaDiaChange,
                            onHoraInicioChange = onReglaHoraInicioChange,
                            onHoraFinChange = onReglaHoraFinChange,
                            onSubmit = onCrearRegla,
                        )
                    }
                    if (estado.reglas.isNotEmpty()) {
                        item {
                            ListaReglas(reglas = estado.reglas, onEliminar = onEliminarRegla)
                        }
                    }
                    item {
                        FormExcepcion(
                            form = excepcionForm,
                            guardando = estado.guardando,
                            onFechaChange = onExcepcionFechaChange,
                            onTipoChange = onExcepcionTipoChange,
                            onHoraInicioChange = onExcepcionHoraInicioChange,
                            onHoraFinChange = onExcepcionHoraFinChange,
                            onMotivoChange = onExcepcionMotivoChange,
                            onSubmit = onCrearExcepcion,
                        )
                    }
                    if (estado.excepciones.isNotEmpty()) {
                        item { ListaExcepciones(excepciones = estado.excepciones) }
                    }
                    item {
                        DisponibilidadSection(
                            fecha = estado.fechaConsulta,
                            disponibilidad = estado.disponibilidad,
                            guardando = estado.guardando,
                            onFechaChange = onFechaDisponibilidadChange,
                            onConsultar = onConsultarDisponibilidad,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorMensaje(error: Resultado.Error) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Text(
            text = error.mensaje ?: "Ocurrió un error inesperado.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun AgendaHeader() {
    val fondo = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        ),
    )
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(fondo, shape = RoundedCornerShape(28.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Gestiona tu agenda",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                ),
            )
            Text(
                text = "Configura horarios recurrentes, bloquea fechas excepcionales y revisa tu disponibilidad diaria.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                ),
            )
        }
    }
}

@Composable
private fun FormReglaSemanal(
    form: ReglaFormState,
    guardando: Boolean,
    onDiaChange: (CrearReglaSemanal.DiaSemana) -> Unit,
    onHoraInicioChange: (String) -> Unit,
    onHoraFinChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Regla semanal",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = "Define bloques recurrentes para tus días de atención.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CrearReglaSemanal.DiaSemana.values().forEach { dia ->
                    FilterChip(
                        selected = form.diaSemana == dia,
                        onClick = { onDiaChange(dia) },
                        label = { Text(text = dia.name.take(3)) },
                    )
                }
            }
            OutlinedTextField(
                value = form.horaInicio,
                onValueChange = onHoraInicioChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Hora inicio (HH:mm)") },
            )
            OutlinedTextField(
                value = form.horaFin,
                onValueChange = onHoraFinChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Hora fin (HH:mm)") },
            )
            form.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = onSubmit,
                enabled = !guardando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Crear regla")
            }
        }
    }
}

@Composable
private fun ListaReglas(
    reglas: List<ReglaSemanal>,
    onEliminar: (UUID) -> Unit,
) {
    if (reglas.isEmpty()) return
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Reglas creadas",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                reglas.forEach { regla ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(text = "${regla.diaSemana.value}: ${regla.horaInicio} - ${regla.horaFin}")
                            regla.id?.let { id ->
                                TextButton(onClick = { onEliminar(id) }) {
                                    Text(text = "Eliminar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormExcepcion(
    form: ExcepcionFormState,
    guardando: Boolean,
    onFechaChange: (String) -> Unit,
    onTipoChange: (CrearExcepcion.Tipo) -> Unit,
    onHoraInicioChange: (String) -> Unit,
    onHoraFinChange: (String) -> Unit,
    onMotivoChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Excepciones",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = "Bloquea fechas específicas por vacaciones, reuniones u otras contingencias.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = form.fecha ?: "",
                onValueChange = onFechaChange,
                label = { Text(text = "Fecha (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CrearExcepcion.Tipo.values().forEach { tipo ->
                    FilterChip(
                        selected = form.tipo == tipo,
                        onClick = { onTipoChange(tipo) },
                        label = { Text(text = tipo.value) },
                    )
                }
            }
            OutlinedTextField(
                value = form.horaInicio,
                onValueChange = onHoraInicioChange,
                label = { Text(text = "Hora inicio (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.horaFin,
                onValueChange = onHoraFinChange,
                label = { Text(text = "Hora fin (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.motivo,
                onValueChange = onMotivoChange,
                label = { Text(text = "Motivo (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            form.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = onSubmit,
                enabled = !guardando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Crear excepción")
            }
        }
    }
}

@Composable
private fun ListaExcepciones(excepciones: List<ExcepcionDisponibilidad>) {
    if (excepciones.isEmpty()) return
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Excepciones creadas",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                excepciones.forEach { excepcion ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(text = "${excepcion.fecha}: ${excepcion.tipo.value}")
                            excepcion.motivo?.let { Text(text = "Motivo: $it") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisponibilidadSection(
    fecha: String,
    disponibilidad: List<BloqueHorario>,
    guardando: Boolean,
    onFechaChange: (String) -> Unit,
    onConsultar: () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Consultar disponibilidad",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            OutlinedTextField(
                value = fecha,
                onValueChange = onFechaChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Fecha (YYYY-MM-DD)") },
            )
            Button(
                onClick = onConsultar,
                enabled = !guardando && fecha.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Consultar")
            }
            if (guardando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (disponibilidad.isEmpty() && !guardando) {
                Text(
                    text = "No tienes disponibilidad para la fecha seleccionada.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    disponibilidad.forEach { bloque ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            ),
                        ) {
                            Text(
                                text = "${bloque.inicio} - ${bloque.fin}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
