package cl.clinipets.ui.staff

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.DatePicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.clinipets.openapi.models.FinalizarCitaRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAtencionScreen(
    citaId: String,
    mascotaId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: StaffAtencionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(citaId) {
        viewModel.cargarCita(citaId)
    }

    LaunchedEffect(state.success) {
        if (state.success) {
            onSuccess()
        }
    }

    LaunchedEffect(state.paymentLinkToShare) {
        state.paymentLinkToShare?.let { link ->
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Hola! Aquí tienes el link para pagar el saldo de tu atención en Clinipets: $link")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Enviar link de pago")
            context.startActivity(shareIntent)
            // Finalizamos el flujo tras lanzar el intent
            onSuccess()
        }
    }

    if (state.showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { /* No dismissal to force selection or back */ },
            title = { Text("Cobrar Saldo Pendiente: $${state.cita?.saldoPendiente}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethodButton(
                        text = "Efectivo",
                        icon = Icons.Default.Money,
                        onClick = { viewModel.onPaymentMethodSelected(FinalizarCitaRequest.MetodoPago.EFECTIVO, citaId, mascotaId) }
                    )
                    PaymentMethodButton(
                        text = "Transferencia",
                        icon = Icons.Default.PhoneAndroid,
                        onClick = { viewModel.onPaymentMethodSelected(FinalizarCitaRequest.MetodoPago.TRANSFERENCIA, citaId, mascotaId) }
                    )
                    PaymentMethodButton(
                        text = "Tarjeta (POS)",
                        icon = Icons.Default.CreditCard,
                        onClick = { viewModel.onPaymentMethodSelected(FinalizarCitaRequest.MetodoPago.TARJETA_POS, citaId, mascotaId) }
                    )
                    PaymentMethodButton(
                        text = "Generar Link de Pago",
                        icon = Icons.Default.Link,
                        onClick = { viewModel.onPaymentMethodSelected(FinalizarCitaRequest.MetodoPago.MERCADO_PAGO_LINK, citaId, mascotaId) }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { /* Optional: Close dialog logic if needed */ }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ficha Clínica") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                Button(
                    onClick = { viewModel.iniciarFinalizacion() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(20.dp)
                                .height(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Finalizar Atención")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Signos Vitales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = state.peso,
                onValueChange = { viewModel.onPesoChanged(it) },
                label = { Text("Peso Actual (kg)") },
                leadingIcon = { Icon(Icons.Default.MonitorWeight, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Evolución Clínica (SOAP)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = state.anamnesis,
                onValueChange = { viewModel.onAnamnesisChanged(it) },
                label = { Text("Anamnesis (Subjetivo)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = state.examenFisico,
                onValueChange = { viewModel.onExamenFisicoChanged(it) },
                label = { Text("Examen Físico (Objetivo)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = state.diagnostico,
                onValueChange = { viewModel.onDiagnosticoChanged(it) },
                label = { Text("Diagnóstico (Análisis)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = state.tratamiento,
                onValueChange = { viewModel.onTratamientoChanged(it) },
                label = { Text("Tratamiento (Plan)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Seguimiento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("¿Agendar recordatorio?")
                Switch(
                    checked = state.agendarRecordatorio,
                    onCheckedChange = { viewModel.onAgendarRecordatorioChanged(it) }
                )
            }

            if (state.agendarRecordatorio) {
                val dateText = state.fechaProximoControl?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Seleccionar Fecha"
                
                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        val dpd = DatePickerDialog(
                            context,
                            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                                viewModel.onFechaProximoControlChanged(LocalDate.of(year, month + 1, dayOfMonth))
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        dpd.datePicker.minDate = System.currentTimeMillis()
                        dpd.show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, null)
                    Spacer(Modifier.width(8.dp))
                    Text(dateText)
                }
            }
            
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Carnet Sanitario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Test Retroviral Negativo\n(Habilita Vacunas Leucemia)",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state.testRetroviralNegativo,
                    onCheckedChange = { viewModel.onTestRetroviralChanged(it) }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Esterilizado")
                Switch(
                    checked = state.esterilizado,
                    onCheckedChange = { viewModel.onEsterilizadoChanged(it) }
                )
            }
            
            Spacer(Modifier.height(64.dp)) // Espacio extra para que el botón no tape contenido
        }
    }
}

@Composable
fun PaymentMethodButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}
