package cl.clinipets.ui.staff

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import cl.clinipets.openapi.models.FinalizarCitaRequest
import java.io.File
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
    val fileProviderAuthority = "${context.packageName}.fileprovider"

    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCameraFile?.let { captured ->
                viewModel.onPhotoSelected(captured.absolutePath)
            }
        } else {
            pendingCameraFile = null
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val targetFile = runCatching { createTempImageFile(context) }.getOrNull()
        if (targetFile == null) {
            Toast.makeText(context, "No pudimos preparar el archivo para la imagen", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.onPhotoSelected(targetFile.absolutePath)
        }.onFailure {
            Toast.makeText(context, "No pudimos copiar la imagen seleccionada", Toast.LENGTH_SHORT).show()
            targetFile.delete()
        }
    }

    val selectedImageUri = remember(state.selectedPhotoPath) {
        state.selectedPhotoPath
            ?.let { File(it) }
            ?.takeIf { it.exists() }
            ?.let { FileProvider.getUriForFile(context, fileProviderAuthority, it) }
    }

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

    if (state.showResumenDialog) {
        AlertDialog(
            onDismissRequest = { /* no op to obligate selección */ },
            title = { Text("Revisar mensaje para WhatsApp") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.resumenTexto,
                        onValueChange = viewModel::onResumenTextChanged,
                        label = { Text("Mensaje editable") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5
                    )
                    selectedImageUri?.let { uri ->
                        Text(
                            text = "Foto a compartir",
                            style = MaterialTheme.typography.labelMedium
                        )
                        AsyncImage(
                            model = uri,
                            contentDescription = "Foto seleccionada",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        shareToWhatsApp(
                            context = context,
                            message = state.resumenTexto,
                            imageUri = selectedImageUri
                        )
                        viewModel.onDialogActionConfirmed(citaId, mascotaId)
                    },
                    enabled = !state.isLoading
                ) {
                    Text("Enviar WhatsApp")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onDialogActionConfirmed(citaId, mascotaId) },
                    enabled = !state.isLoading
                ) {
                    Text("Solo cerrar")
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
                    onClick = { viewModel.iniciarFinalizacion(citaId, mascotaId) },
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

            Text(
                text = "Foto de la atención",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val photoFile = runCatching { createTempImageFile(context) }.getOrNull()
                        if (photoFile == null) {
                            Toast.makeText(context, "No pudimos crear el archivo para la cámara", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        pendingCameraFile = photoFile
                        val uri = FileProvider.getUriForFile(context, fileProviderAuthority, photoFile)
                        takePictureLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tomar foto")
                }

                OutlinedButton(
                    onClick = { pickImageLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Elegir galería")
                }
            }

            selectedImageUri?.let { uri ->
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Vista previa de la foto",
                        modifier = Modifier
                            .size(96.dp),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "La foto se adjuntará al historial y al WhatsApp.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

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

private fun createTempImageFile(context: Context): File {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    return File.createTempFile("clinipets_atencion_", ".jpg", imagesDir)
}

private fun shareToWhatsApp(
    context: Context,
    message: String,
    imageUri: Uri?
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        if (imageUri != null) {
            type = "image/jpeg"

            // 1. El estándar: Stream y Texto
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, message)

            // 2. El truco de compatibilidad: Título y Asunto (algunas apps usan esto como caption)
            putExtra(Intent.EXTRA_TITLE, message)
            putExtra(Intent.EXTRA_SUBJECT, message)

            // 3. El truco moderno (ClipData):
            // Esto le dice a Android explícitamente "Este Intent lleva este archivo"
            // Ayuda a que WhatsApp asocie el texto al archivo correctamente.   
            clipData = ClipData.newRawUri(null, imageUri)

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
    }

    // Lógica de detección de paquetes (Business vs Normal)
    val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
    var targetPackage: String? = null

    for (packageName in whatsappPackages) {
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            targetPackage = packageName
            break
        } catch (e: Exception) {
            continue
        }
    }

    try {
        if (targetPackage != null) {
            intent.setPackage(targetPackage)
            context.startActivity(intent)
        } else {
            val chooser = Intent.createChooser(intent, "Enviar resumen vía...")
            context.startActivity(chooser)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
    }
}