package cl.clinipets.feature.veterinario.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.core.Resultado
import cl.clinipets.openapi.models.CrearExcepcion
import cl.clinipets.openapi.models.CrearReglaSemanal
import cl.clinipets.openapi.models.ReglaSemanal
import cl.clinipets.openapi.models.ExcepcionDisponibilidad
import cl.clinipets.openapi.models.BloqueHorario
import java.util.UUID
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            when {
                estado.cargandoPerfil -> Text(text = "Cargando perfil...")
                estado.veterinarioId == null -> ErrorMensaje(estado.error ?: Resultado.Error(Resultado.Tipo.DESCONOCIDO, "No encontramos tu perfil."))
                else -> {
                    if (estado.error != null) {
                        ErrorMensaje(estado.error)
                    }
                    FormReglaSemanal(
                        form = reglaForm,
                        guardando = estado.guardando,
                        onDiaChange = onReglaDiaChange,
                        onHoraInicioChange = onReglaHoraInicioChange,
                        onHoraFinChange = onReglaHoraFinChange,
                        onSubmit = onCrearRegla,
                    )
                    ListaReglas(reglas = estado.reglas, onEliminar = onEliminarRegla)

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
                    ListaExcepciones(excepciones = estado.excepciones)

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

@Composable
private fun ErrorMensaje(error: Resultado.Error) {
    Text(text = error.mensaje ?: "Ocurrió un error inesperado.")
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Crear regla semanal")
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
        form.error?.let { Text(text = it) }
        Button(
            onClick = onSubmit,
            enabled = !guardando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Crear regla")
        }
    }
}

@Composable
private fun ListaReglas(
    reglas: List<ReglaSemanal>,
    onEliminar: (UUID) -> Unit,
) {
    if (reglas.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Reglas creadas")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reglas, key = { it.id ?: UUID.nameUUIDFromBytes((it.diaSemana.value + it.horaInicio + it.horaFin).toByteArray()) }) { regla ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Excepciones")
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
        form.error?.let { Text(text = it) }
        Button(
            onClick = onSubmit,
            enabled = !guardando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Crear excepción")
        }
    }
}

@Composable
private fun ListaExcepciones(excepciones: List<ExcepcionDisponibilidad>) {
    if (excepciones.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Excepciones creadas")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(excepciones, key = { it.id ?: UUID.nameUUIDFromBytes((it.fecha.toString() + it.tipo.value + (it.horaInicio ?: "") + (it.horaFin ?: "")).toByteArray()) }) { excepcion ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = "${excepcion.fecha}: ${excepcion.tipo.value}")
                    excepcion.motivo?.let { Text(text = "Motivo: $it") }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Consultar disponibilidad")
        OutlinedTextField(
            value = fecha,
            onValueChange = onFechaChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "Fecha (YYYY-MM-DD)") },
        )
        Button(
            onClick = onConsultar,
            enabled = !guardando && fecha.isNotBlank(),
        ) {
            Text(text = "Consultar")
        }
        if (guardando) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (disponibilidad.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                disponibilidad.forEach { bloque ->
                    Text(text = "${bloque.inicio} - ${bloque.fin}")
                }
            }
        }
    }
}
