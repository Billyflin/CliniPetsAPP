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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun PaymentMethodButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

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
    var selectedTab by remember { mutableStateOf(0) }

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

    if (state.showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { },
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
                TextButton(onClick = { /* Optional */ }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (state.showResumenDialog) {
        AlertDialog(
            onDismissRequest = { },
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
                    enabled = !state.isLoading && selectedTab == 2 // Enable only on last tab
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
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
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text(text = "Triaje", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text(text = "Examen", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text(text = "Plan", modifier = Modifier.padding(16.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.error != null) {
                    Text(text = state.error ?: "", color = MaterialTheme.colorScheme.error)
                }

                when (selectedTab) {
                    0 -> { // Triaje
                        SectionHeader(title = "Constantes Vitales", icon = Icons.Default.MonitorWeight)
                        
                        VitalSignInput(
                            label = "Peso Registrado",
                            value = state.form.pesoRegistrado?.toString() ?: "",
                            onValueChange = viewModel::onPesoChanged,
                            icon = Icons.Default.MonitorWeight,
                            unit = "kg"
                        )

                        val tempValue = state.form.temperatura
                        val isHighTemp = tempValue != null && tempValue > 39.5
                        
                        VitalSignInput(
                            label = "Temperatura",
                            value = state.form.temperatura?.toString() ?: "",
                            onValueChange = viewModel::onTemperaturaChanged,
                            icon = if (isHighTemp) Icons.Default.Warning else Icons.Default.Thermostat,
                            unit = "°C",
                            isError = isHighTemp
                        )

                        VitalSignInput(
                            label = "Frecuencia Cardíaca",
                            value = state.form.frecuenciaCardiaca?.toString() ?: "",
                            onValueChange = viewModel::onFrecuenciaCardiacaChanged,
                            icon = Icons.Default.Favorite,
                            unit = "lpm"
                        )

                        VitalSignInput(
                            label = "Frecuencia Respiratoria",
                            value = state.form.frecuenciaRespiratoria?.toString() ?: "",
                            onValueChange = viewModel::onFrecuenciaRespiratoriaChanged,
                            icon = Icons.Default.Air,
                            unit = "rpm"
                        )

                        if (state.cita?.estado == cl.clinipets.openapi.models.CitaDetalladaResponse.Estado.CONFIRMADA) {
                            Button(
                                onClick = { viewModel.guardarTriaje(citaId) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Default.Save, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Guardar Triaje e Iniciar Atención")
                                }
                            }
                        }
                    }
                    1 -> { // Examen
                        SectionHeader(title = "Anamnesis", icon = Icons.Default.Info)
                        OutlinedTextField(
                            value = state.form.anamnesis ?: "",
                            onValueChange = viewModel::onAnamnesisChanged,
                            label = { Text("Relato del tutor / Historia") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        SectionHeader(title = "Examen Físico Segmentario", icon = Icons.Default.MedicalServices)
                        
                        val ef = state.form.examenFisico ?: cl.clinipets.openapi.models.ExamenFisicoDto()
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = ef.mucosas ?: "",
                                    onValueChange = { viewModel.updateExamenFisico(ef.copy(mucosas = it)) },
                                    label = { Text("Mucosas") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = ef.tllc ?: "",
                                    onValueChange = { viewModel.updateExamenFisico(ef.copy(tllc = it)) },
                                    label = { Text("TLLC") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            
                            OutlinedTextField(
                                value = ef.hidratacion ?: "",
                                onValueChange = { viewModel.updateExamenFisico(ef.copy(hidratacion = it)) },
                                label = { Text("Grado de Hidratación") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = ef.linfonodos ?: "",
                                onValueChange = { viewModel.updateExamenFisico(ef.copy(linfonodos = it)) },
                                label = { Text("Linfonodos") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = ef.pielAnexos ?: "",
                                onValueChange = { viewModel.updateExamenFisico(ef.copy(pielAnexos = it)) },
                                label = { Text("Piel y Anexos") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            OutlinedTextField(
                                value = ef.sistemaCardiovascular ?: "",
                                onValueChange = { viewModel.updateExamenFisico(ef.copy(sistemaCardiovascular = it)) },
                                label = { Text("Sist. Cardiovascular") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        
                        SectionHeader(title = "Evidencia Gráfica", icon = Icons.Default.AddAPhoto)
                        PhotoSelectionRow(
                            onCameraClick = {
                                val photoFile = runCatching { createTempImageFile(context) }.getOrNull()
                                if (photoFile != null) {
                                    pendingCameraFile = photoFile
                                    takePictureLauncher.launch(FileProvider.getUriForFile(context, fileProviderAuthority, photoFile))
                                }
                            },
                            onGalleryClick = { pickImageLauncher.launch("image/*") }
                        )
                        selectedImageUri?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    2 -> { // Diagnóstico y Plan
                        SectionHeader(title = "Diagnóstico y Plan (A+P)", icon = Icons.Default.Description)
                        OutlinedTextField(
                            value = state.form.avaluoClinico ?: "",
                            onValueChange = viewModel::onAvaluoClinicoChanged,
                            label = { Text("Avalúo Clínico (Análisis)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        OutlinedTextField(
                            value = state.form.planTratamiento ?: "",
                            onValueChange = viewModel::onPlanTratamientoChanged,
                            label = { Text("Plan de Tratamiento (Plan)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4
                        )

                        SectionHeader(title = "Seguimiento", icon = Icons.Default.CalendarToday)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("¿Agendar recordatorio?")
                            Spacer(Modifier.weight(1f))
                            Switch(checked = state.form.fechaProximoControl != null, onCheckedChange = viewModel::onAgendarRecordatorioChanged)
                        }
                        if (state.form.fechaProximoControl != null) {
                            DatePickerButton(
                                date = state.form.fechaProximoControl,
                                onDateSelected = viewModel::onFechaProximoControlChanged
                            )
                        }

                        SectionHeader(title = "Carnet Sanitario", icon = Icons.Default.VerifiedUser)
                        SwitchRow(label = "Test Retroviral Negativo", checked = state.testRetroviralNegativo, onCheckedChange = viewModel::onTestRetroviralChanged)
                        SwitchRow(label = "Esterilizado", checked = state.esterilizado, onCheckedChange = viewModel::onEsterilizadoChanged)
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (selectedTab > 0) {
                        OutlinedButton(onClick = { selectedTab-- }) { Text("Anterior") }
                    } else {
                        Spacer(Modifier.width(8.dp))
                    }
                    if (selectedTab < 2) {
                        Button(onClick = { selectedTab++ }) { Text("Siguiente") }
                    }
                }
                Spacer(Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun PhotoSelectionRow(onCameraClick: () -> Unit, onGalleryClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onCameraClick, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.AddAPhoto, null)
            Spacer(Modifier.width(8.dp))
            Text("Cámara")
        }
        OutlinedButton(onClick = onGalleryClick, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Image, null)
            Spacer(Modifier.width(8.dp))
            Text("Galería")
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun DatePickerButton(date: LocalDate?, onDateSelected: (LocalDate) -> Unit) {
    val context = LocalContext.current
    val dateText = date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Seleccionar Fecha"
    OutlinedButton(onClick = {
        val calendar = Calendar.getInstance()
        DatePickerDialog(context, { _, y, m, d -> onDateSelected(LocalDate.of(y, m + 1, d)) }, 
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.CalendarToday, null)
        Spacer(Modifier.width(8.dp))
        Text(dateText)
    }
}

private fun createTempImageFile(context: Context): File {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    return File.createTempFile("clinipets_atencion_", ".jpg", imagesDir)
}

private fun shareToWhatsApp(context: Context, message: String, imageUri: Uri?) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        if (imageUri != null) {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, message)
            clipData = ClipData.newRawUri(null, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
    }
    val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
    var targetPackage: String? = null
    for (pkg in whatsappPackages) {
        try { context.packageManager.getPackageInfo(pkg, 0); targetPackage = pkg; break } catch (e: Exception) { continue }
    }
    try {
        if (targetPackage != null) { intent.setPackage(targetPackage); context.startActivity(intent) }
        else { context.startActivity(Intent.createChooser(intent, "Enviar vía...")) }
    } catch (e: Exception) { Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show() }
}

@Composable
fun VitalSignInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    unit: String,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(unit) },
        leadingIcon = { Icon(icon, null, tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) },
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else Color.Transparent
        )
    )
}
