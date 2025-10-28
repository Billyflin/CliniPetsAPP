package cl.clinipets.reservas

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.clinipets.network.Bloque
import cl.clinipets.network.Mascota
import cl.clinipets.network.Procedimiento
import cl.clinipets.network.VetItem
import com.google.android.gms.location.LocationServices

private enum class Paso { MASCOTA, PROCEDIMIENTO, VETERINARIO, FECHA_BLOQUE, CONFIRMAR }

@Composable
fun ReservaFlowScreen(viewModelFactory: androidx.lifecycle.ViewModelProvider.Factory, initialSku: String? = null, initialEspecie: String? = null) {
    val vm: ReservaFlowViewModel = viewModel(factory = viewModelFactory)

    val mascotas by vm.mascotas.collectAsState()
    val procedimientos by vm.procedimientos.collectAsState()
    val veterinarios by vm.veterinarios.collectAsState()
    val bloques by vm.bloques.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    var paso by remember { mutableStateOf(Paso.MASCOTA) }
    var bootstrapped by remember { mutableStateOf(false) }

    // Bootstrap: si viene initialEspecie, precarga procedimientos para acelerar el paso 2
    LaunchedEffect(Unit) {
        if (!bootstrapped) {
            vm.cargarMascotas()
            if (!initialEspecie.isNullOrBlank()) {
                vm.cargarProcedimientos(initialEspecie)
            }
            bootstrapped = true
        }
    }

    // Si hay initialSku y ya cargamos procedimientos, autoseleccionamos y avanzamos al paso de veterinario
    LaunchedEffect(initialSku, procedimientos) {
        if (!initialSku.isNullOrBlank() && paso == Paso.PROCEDIMIENTO) {
            val match = procedimientos.firstOrNull { it.sku == initialSku }
            if (match != null) {
                vm.selectedProcedimiento = match
                paso = Paso.VETERINARIO
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Nueva reserva", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (!error.isNullOrEmpty()) Text(error ?: "", color = MaterialTheme.colorScheme.error)
        if (isLoading) Text("Cargando...")

        when (paso) {
            Paso.MASCOTA -> PasoMascota(vm, mascotas) { paso = Paso.PROCEDIMIENTO }
            Paso.PROCEDIMIENTO -> PasoProcedimiento(vm, procedimientos, initialSku) {
                paso = Paso.VETERINARIO
            }
            Paso.VETERINARIO -> PasoVeterinario(vm, veterinarios) { paso = Paso.FECHA_BLOQUE }
            Paso.FECHA_BLOQUE -> PasoFechaBloque(vm, bloques) { paso = Paso.CONFIRMAR }
            Paso.CONFIRMAR -> PasoConfirmar(vm) { paso = Paso.MASCOTA }
        }
    }
}

@Composable
private fun PasoMascota(vm: ReservaFlowViewModel, mascotas: List<Mascota>, onNext: () -> Unit) {
    LaunchedEffect(Unit) { vm.cargarMascotas() }
    Text("1) Selecciona tu mascota", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(mascotas) { m ->
            Row(modifier = Modifier.fillMaxWidth().clickable {
                vm.selectedMascota = m
                vm.cargarProcedimientos(m.especie)
                onNext()
            }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(m.nombre)
                Text(m.especie)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PasoProcedimiento(vm: ReservaFlowViewModel, procedimientos: List<Procedimiento>, initialSku: String?, onNext: () -> Unit) {
    Text("2) Selecciona el procedimiento", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    // Si hay initialSku y aún no se ha seleccionado, intentar autoseleccionar
    LaunchedEffect(procedimientos, initialSku) {
        if (!initialSku.isNullOrBlank() && vm.selectedProcedimiento == null) {
            procedimientos.firstOrNull { it.sku == initialSku }?.let {
                vm.selectedProcedimiento = it
                onNext()
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(procedimientos) { p ->
            Column(modifier = Modifier.fillMaxWidth().clickable {
                vm.selectedProcedimiento = p
                onNext()
            }.padding(vertical = 8.dp)) {
                Text(p.nombre)
                Text("Duración: ${p.duracionMinutos} min", style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun PasoVeterinario(vm: ReservaFlowViewModel, veterinarios: List<VetItem>, onNext: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            val fused = LocationServices.getFusedLocationProviderClient(ctx)
            fused.lastLocation.addOnSuccessListener { loc ->
                lat = loc?.latitude
                lng = loc?.longitude
                val especie = vm.selectedMascota?.especie
                val sku = vm.selectedProcedimiento?.sku
                vm.cargarVeterinarios(especie, sku, lat, lng)
            }
        } else {
            val especie = vm.selectedMascota?.especie
            val sku = vm.selectedProcedimiento?.sku
            vm.cargarVeterinarios(especie, sku, null, null)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Text("3) Elige el veterinario", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(veterinarios) { v ->
            Column(modifier = Modifier.fillMaxWidth().clickable {
                vm.selectedVet = v
                onNext()
            }.padding(vertical = 8.dp)) {
                Text(v.nombre)
                if (v.distanciaKm != null) Text("A ${"%.1f".format(v.distanciaKm)} km", style = MaterialTheme.typography.bodySmall)
                Text("Modos: ${v.modos.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PasoFechaBloque(vm: ReservaFlowViewModel, bloques: List<Bloque>, onNext: () -> Unit) {
    var fecha by remember { mutableStateOf("") } // YYYY-MM-DD

    Text("4) Fecha y bloque", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = fecha, onValueChange = { fecha = it }, label = { Text("Fecha (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = {
        val vetId = vm.selectedVet?.id ?: return@Button
        if (fecha.isNotBlank()) vm.cargarBloques(vetId, fecha)
    }, modifier = Modifier.fillMaxWidth()) { Text("Consultar disponibilidad") }

    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(bloques) { b ->
            Row(modifier = Modifier.fillMaxWidth().clickable {
                vm.selectedInicioIso = fecha + "T" + b.inicio + ":00"
                onNext()
            }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${b.inicio}-${b.fin}")
                Text("Seleccionar")
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PasoConfirmar(vm: ReservaFlowViewModel, onDone: () -> Unit) {
    var modo by remember { mutableStateOf(vm.selectedModo) }
    var direccion by remember { mutableStateOf(vm.direccionAtencion ?: "") }
    var notas by remember { mutableStateOf(vm.notas ?: "") }

    Text("5) Confirmar", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Mascota: ${vm.selectedMascota?.nombre}")
    Text("Procedimiento: ${vm.selectedProcedimiento?.nombre}")
    Text("Vet: ${vm.selectedVet?.nombre}")
    Text("Inicio: ${vm.selectedInicioIso}")

    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = modo, onValueChange = {
        modo = it
        vm.selectedModo = it
    }, label = { Text("Modo (CLINICA/DOMICILIO)") }, modifier = Modifier.fillMaxWidth())

    if (modo == "DOMICILIO") {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = direccion, onValueChange = {
            direccion = it
            vm.direccionAtencion = it
        }, label = { Text("Dirección de atención") }, modifier = Modifier.fillMaxWidth())
    }

    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = notas, onValueChange = {
        notas = it
        vm.notas = it
    }, label = { Text("Notas (opcional)") }, modifier = Modifier.fillMaxWidth())

    Spacer(modifier = Modifier.height(12.dp))
    Button(onClick = {
        vm.crearReserva { ok, _ -> if (ok) onDone() }
    }, modifier = Modifier.fillMaxWidth()) { Text("Confirmar reserva") }
}
