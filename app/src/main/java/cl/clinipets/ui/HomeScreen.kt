package cl.clinipets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.clinipets.data.dto.Mascota
import cl.clinipets.data.dto.MeResponse
import cl.clinipets.data.dto.OfertaDto
import cl.clinipets.data.dto.ProcedimientoDto
import cl.clinipets.data.dto.ProductoDto
import cl.clinipets.data.dto.ReservaDto
import cl.clinipets.data.dto.ReservaEstado
import cl.clinipets.data.dto.StockItemDto
import cl.clinipets.data.dto.UpsertOferta
import cl.clinipets.data.dto.UpsertPerfil
import cl.clinipets.data.dto.UpsertProcedimiento
import cl.clinipets.domain.agenda.AgendaRepository
import cl.clinipets.domain.auth.AuthRepository
import cl.clinipets.domain.catalogo.CatalogoRepository
import cl.clinipets.domain.discovery.DiscoveryRepository
import cl.clinipets.domain.inventario.InventarioRepository
import cl.clinipets.domain.mascotas.MascotasRepository
import cl.clinipets.domain.veterinarios.VeterinariosRepository
import cl.clinipets.ui.discovery.MapDiscoverScreen
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

private enum class Role { ADMIN, CLIENTE, VETERINARIO }

private data class TabSpec(val route: String, val label: String)

@Composable
fun HomeScreen(
    discoveryRepository: DiscoveryRepository,
    mascotasRepository: MascotasRepository,
    agendaRepository: AgendaRepository,
    catalogoRepository: CatalogoRepository,
    onLogout: () -> Unit,
    authRepository: AuthRepository,
    forbiddenSignal: Long = 0L,
    showMessage: (String) -> Unit = {},
    veterinariosRepository: VeterinariosRepository? = null,
    inventarioRepository: InventarioRepository? = null,
) {
    val navController = rememberNavController()

    // Roles y rol activo
    val roles = remember { mutableStateOf(authRepository.getRoles()) }
    val activeRoleState =
        remember { mutableStateOf(authRepository.getActiveRole() ?: roles.value.firstOrNull()) }

    fun tabsFor(role: Role?): List<TabSpec> = when (role) {
        Role.CLIENTE -> listOf(
            TabSpec("cliente/inicio", "Inicio"),
            TabSpec("descubrir", "Descubrir"),
            TabSpec("mascotas", "Mascotas"),
            TabSpec("reservas", "Reservas"),
        )

        Role.VETERINARIO -> listOf(
            TabSpec("vet/inicio", "Inicio"),
            TabSpec("vet/agenda", "Agenda"),
            TabSpec("vet/ofertas", "Ofertas"),
            TabSpec("vet/perfil", "Perfil"),
        )

        Role.ADMIN -> listOf(
            TabSpec("admin/inicio", "Inicio"),
            TabSpec("admin/procedimientos", "Procedimientos"),
            TabSpec("admin/veterinarios", "Veterinarios"),
            TabSpec("admin/inventario", "Inventario"),
        )

        else -> emptyList()
    }

    fun parseRole(r: String?): Role? = when (r) {
        "CLIENTE" -> Role.CLIENTE
        "VETERINARIO" -> Role.VETERINARIO
        "ADMIN" -> Role.ADMIN
        else -> null
    }

    val activeRole = parseRole(activeRoleState.value)
    val tabs = tabsFor(activeRole)
    val selectedIndex = remember(activeRole to tabs) { mutableStateOf(0) }

    // Redirigir a startDestination al cambiar de rol
    LaunchedEffect(activeRole) {
        tabs.firstOrNull()?.let { first ->
            selectedIndex.value = 0
            navController.navigate(first.route) { popUpTo(0) }
        }
    }

    // Manejo de 403: volver a Home del rol
    LaunchedEffect(forbiddenSignal) {
        if (forbiddenSignal > 0) {
            tabs.firstOrNull()?.let { first ->
                navController.navigate(first.route) { launchSingleTop = true }
            }
            showMessage("No tienes permisos para esta sección")
        }
    }

    Column(Modifier.fillMaxSize()) {
        // App bar simple con badge de rol y switcher
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Vista: ${activeRoleState.value ?: "-"}",
                style = MaterialTheme.typography.titleMedium
            )
            if (roles.value.size > 1) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { expanded = true }) { Text("Cambiar vista") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        roles.value.forEach { r ->
                            DropdownMenuItem(text = { Text(r) }, onClick = {
                                expanded = false
                                authRepository.setActiveRole(r)
                                activeRoleState.value = r
                                showMessage("Vista cambiada a $r")
                            })
                        }
                    }
                }
            }
        }

        if (tabs.isNotEmpty()) {
            TabRow(selectedTabIndex = selectedIndex.value) {
                tabs.forEachIndexed { index, t ->
                    Tab(
                        selected = selectedIndex.value == index,
                        onClick = {
                            selectedIndex.value = index
                            navController.navigate(t.route) { launchSingleTop = true }
                        },
                        text = { Text(t.label) }
                    )
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = tabs.firstOrNull()?.route ?: "descubrir"
        ) {
            // CLIENTE
            composable("cliente/inicio") {
                ClienteHomeScreen(authRepository, agendaRepository, mascotasRepository)
            }
            composable("descubrir") {
                MapDiscoverScreen(
                    repo = discoveryRepository,
                    onOpenVet = { vetId -> navController.navigate("vet/$vetId") }
                )
            }
            composable("mascotas") { MascotasScreen(repo = mascotasRepository) }
            composable("reservas") { ReservasScreen(repo = agendaRepository) }

            // VETERINARIO
            composable("vet/inicio") { VetHomeScreen(agendaRepository) }
            composable("vet/agenda") { AgendaVetScreen(agendaRepository, showMessage) }
            composable("vet/ofertas") { OfertasVetScreen(catalogoRepository, showMessage) }
            composable("vet/perfil") { PerfilVetScreen(veterinariosRepository, showMessage) }

            // ADMIN
            composable("admin/inicio") {
                AdminHomeScreen(
                    catalogoRepository,
                    veterinariosRepository,
                    inventarioRepository
                )
            }
            composable("admin/procedimientos") {
                ProcedimientosAdminScreen(
                    catalogoRepository,
                    showMessage
                )
            }
            composable("admin/veterinarios") {
                VeterinariosAdminScreen(
                    veterinariosRepository,
                    showMessage
                )
            }
            composable("admin/inventario") { InventarioAdminScreen(inventarioRepository) }

            // Detalle Vet existente
            composable(
                route = "vet/{vetId}",
                arguments = listOf(navArgument("vetId") { type = NavType.StringType })
            ) { backStackEntry ->
                val vetId = backStackEntry.arguments?.getString("vetId") ?: ""
                VetDetailScreen(
                    vetId = vetId,
                    catalogoRepository = catalogoRepository,
                    agendaRepository = agendaRepository,
                    mascotasRepository = mascotasRepository
                )
            }

            // Perfil existente
            composable("perfil") {
                ProfileScreen(
                    onLogout = onLogout,
                    authRepository = authRepository
                )
            }
        }
    }
}

@Composable
private fun ClienteHomeScreen(
    authRepository: AuthRepository,
    agendaRepository: AgendaRepository,
    mascotasRepository: MascotasRepository,
) {
    val scope = rememberCoroutineScope()
    val me = remember { mutableStateOf<MeResponse?>(null) }
    val proxima = remember { mutableStateOf<ReservaDto?>(null) }
    val mascotas = remember { mutableStateOf<List<Mascota>>(emptyList()) }
    val loading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Inicio (Cliente)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    loading.value = true; error.value = null
                    runCatching { authRepository.me() }
                        .onSuccess { me.value = it }
                        .onFailure { error.value = it.message }
                    loading.value = false
                }
            }) { Text("Cargar perfil") }
            Button(onClick = {
                scope.launch {
                    loading.value = true; error.value = null
                    runCatching {
                        val list = agendaRepository.reservasMias()
                        val now = Instant.now()
                        proxima.value = list
                            .filter { it.estado in listOf(ReservaEstado.PENDIENTE, ReservaEstado.ACEPTADA) }
                            .minByOrNull {
                                try {
                                    Instant.parse(it.inicio)
                                } catch (_: DateTimeParseException) {
                                    now.plusSeconds(10_000_000)
                                }
                            }
                    }.onFailure { error.value = it.message }
                    loading.value = false
                }
            }) { Text("Próxima reserva") }
            Button(onClick = {
                scope.launch {
                    loading.value = true; error.value = null
                    runCatching { mascotasRepository.listarMias() }
                        .onSuccess { mascotas.value = it }
                        .onFailure { error.value = it.message }
                    loading.value = false
                }
            }) { Text("Mis mascotas") }
        }
        Spacer(Modifier.height(8.dp))
        me.value?.let { Text("Hola, ${it.email}") }
        proxima.value?.let { r -> Text("Próxima: ${r.inicio} (${r.estado})") }
        if (mascotas.value.isNotEmpty()) {
            Text("Mascotas:")
            mascotas.value.forEach { m -> Text("• ${m.nombre} (${m.especie})") }
        }
        error.value?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        if (loading.value) CircularProgressIndicator()
    }
}

@Composable
private fun VetHomeScreen(agendaRepository: AgendaRepository) {
    val scope = rememberCoroutineScope()
    val hoy = remember { mutableStateOf<List<ReservaDto>>(emptyList()) }
    val error = remember { mutableStateOf<String?>(null) }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Inicio (Veterinario)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                error.value = null
                runCatching {
                    val list = agendaRepository.reservasMias()
                    val today = LocalDate.now(ZoneId.systemDefault())
                    hoy.value = list.filter { r ->
                        runCatching {
                            Instant.parse(r.inicio).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                            .getOrNull() == today
                    }
                }.onFailure { error.value = it.message }
            }
        }) { Text("Cargar agenda de hoy") }
        hoy.value.forEach { r -> Text("• ${r.inicio} - ${r.estado}") }
        error.value?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun AgendaVetScreen(agendaRepository: AgendaRepository, showMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val reservas = remember { mutableStateOf<List<ReservaDto>>(emptyList()) }
    val targetId = remember { mutableStateOf("") }
    val error = remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            error.value = null
            runCatching { agendaRepository.reservasMias() }
                .onSuccess { reservas.value = it }
                .onFailure { error.value = it.message }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Agenda", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = targetId.value,
            onValueChange = { targetId.value = it },
            label = { Text("reservaId") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { refresh() }) { Text("Refrescar") }
            Button(onClick = {
                if (targetId.value.isBlank()) return@Button
                scope.launch {
                    runCatching { agendaRepository.aceptar(targetId.value) }
                        .onSuccess { showMessage("Aceptada"); refresh() }
                        .onFailure { showMessage(it.message ?: "Error") }
                }
            }) { Text("Aceptar") }
            Button(onClick = {
                if (targetId.value.isBlank()) return@Button
                scope.launch {
                    runCatching { agendaRepository.rechazar(targetId.value) }
                        .onSuccess { showMessage("Rechazada"); refresh() }
                        .onFailure { showMessage(it.message ?: "Error") }
                }
            }) { Text("Rechazar") }
            Button(onClick = {
                if (targetId.value.isBlank()) return@Button
                scope.launch {
                    runCatching { agendaRepository.completar(targetId.value) }
                        .onSuccess { showMessage("Completada"); refresh() }
                        .onFailure { showMessage(it.message ?: "Error") }
                }
            }) { Text("Completar") }
        }
        Spacer(Modifier.height(8.dp))
        reservas.value.forEach { r -> Text("• ${r.reservaId} - ${r.inicio} - ${r.estado}") }
        error.value?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun OfertasVetScreen(
    catalogoRepository: CatalogoRepository,
    showMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val ofertas = remember { mutableStateOf<List<OfertaDto>>(emptyList()) }
    val procId = remember { mutableStateOf("") }
    val precio = remember { mutableStateOf(0) }
    val duracion = remember { mutableStateOf(30) }
    val activo = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            error.value = null
            runCatching { catalogoRepository.ofertas(vetId = "YO", activo = true) }
                .onSuccess { ofertas.value = it }
                .onFailure { error.value = it.message }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Mis ofertas", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = procId.value,
                onValueChange = { procId.value = it },
                label = { Text("procedimientoId") })
            OutlinedTextField(
                value = if (precio.value == 0) "" else precio.value.toString(),
                onValueChange = { precio.value = it.toIntOrNull() ?: 0 },
                label = { Text("precioCents") })
            OutlinedTextField(
                value = duracion.value.toString(),
                onValueChange = { duracion.value = it.toIntOrNull() ?: 0 },
                label = { Text("min") })
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Activo")
                Switch(checked = activo.value, onCheckedChange = { activo.value = it })
            }
            Button(onClick = {
                scope.launch {
                    runCatching {
                        catalogoRepository.crearOferta(
                            UpsertOferta(
                                procId.value,
                                precio.value,
                                duracion.value,
                                activo.value
                            )
                        )
                    }
                        .onSuccess { showMessage("Oferta creada"); refresh() }
                        .onFailure { showMessage(it.message ?: "Error") }
                }
            }) { Text("Crear") }
        }
        Spacer(Modifier.height(8.dp))
        ofertas.value.forEach { of ->
            Text("• ${of.ofertaId ?: "(sin id)"} - ${of.nombre} - $${of.precioCents / 100.0}")
        }
        error.value?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun PerfilVetScreen(
    veterinariosRepository: VeterinariosRepository?,
    showMessage: (String) -> Unit
) {
    if (veterinariosRepository == null) {
        Text("Repo no injectado"); return
    }
    val scope = rememberCoroutineScope()
    val nombre = remember { mutableStateOf("") }
    val bio = remember { mutableStateOf("") }
    val lat = remember { mutableStateOf("") }
    val lon = remember { mutableStateOf("") }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Mi perfil (Vet)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = nombre.value,
            onValueChange = { nombre.value = it },
            label = { Text("Nombre") })
        OutlinedTextField(
            value = bio.value,
            onValueChange = { bio.value = it },
            label = { Text("Bio") })
        OutlinedTextField(
            value = lat.value,
            onValueChange = { lat.value = it },
            label = { Text("Lat") })
        OutlinedTextField(
            value = lon.value,
            onValueChange = { lon.value = it },
            label = { Text("Lon") })
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                runCatching {
                    veterinariosRepository.upsertMiPerfil(
                        UpsertPerfil(
                            nombre = nombre.value.ifBlank { null },
                            bio = bio.value.ifBlank { null },
                            latitud = lat.value.toDoubleOrNull(),
                            longitud = lon.value.toDoubleOrNull(),
                        )
                    )
                }.onSuccess { showMessage("Perfil actualizado") }
                    .onFailure { showMessage(it.message ?: "Error") }
            }
        }) { Text("Guardar") }
    }
}

@Composable
private fun AdminHomeScreen(
    catalogoRepository: CatalogoRepository,
    veterinariosRepository: VeterinariosRepository?,
    inventarioRepository: InventarioRepository?,
) {
    val scope = rememberCoroutineScope()
    val kpiOfertas = remember { mutableStateOf<Int?>(null) }
    val kpiProced = remember { mutableStateOf<Int?>(null) }
    val vetsJson = remember { mutableStateOf<String?>(null) }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Inicio (Admin)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    runCatching { catalogoRepository.ofertas().size }.onSuccess {
                        kpiOfertas.value = it
                    }
                }
            }) { Text("KPI Ofertas") }
            Button(onClick = {
                scope.launch {
                    runCatching { catalogoRepository.procedimientos().size }.onSuccess {
                        kpiProced.value = it
                    }
                }
            }) { Text("KPI Proced.") }
            Button(onClick = {
                if (veterinariosRepository != null) {
                    scope.launch {
                        runCatching { veterinariosRepository.publicos() }.onSuccess {
                            vetsJson.value = it
                        }
                    }
                }
            }) { Text("Veterinarios públicos") }
        }
        kpiOfertas.value?.let { Text("Ofertas activas: $it") }
        kpiProced.value?.let { Text("Procedimientos: $it") }
        vetsJson.value?.let { Text("Vets JSON: ${it.take(500)}...") }
    }
}

@Composable
private fun ProcedimientosAdminScreen(
    catalogoRepository: CatalogoRepository,
    showMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val lista = remember { mutableStateOf<List<ProcedimientoDto>>(emptyList()) }
    val nombre = remember { mutableStateOf("") }
    val categoria = remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            runCatching { catalogoRepository.procedimientos() }.onSuccess {
                lista.value = it
            }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Procedimientos", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = nombre.value,
                onValueChange = { nombre.value = it },
                label = { Text("Nombre") })
            OutlinedTextField(
                value = categoria.value,
                onValueChange = { categoria.value = it },
                label = { Text("Categoría") })
            Button(onClick = {
                scope.launch {
                    runCatching {
                        catalogoRepository.crearProcedimiento(
                            UpsertProcedimiento(
                                nombre.value,
                                categoria.value
                            )
                        )
                    }
                        .onSuccess { showMessage("Creado"); refresh() }
                        .onFailure { showMessage(it.message ?: "Error") }
                }
            }) { Text("Crear") }
        }
        Spacer(Modifier.height(8.dp))
        lista.value.forEach { p -> Text("• ${p.id ?: "(sin id)"} - ${p.nombre} [${p.categoria}]") }
    }
}

@Composable
private fun VeterinariosAdminScreen(
    veterinariosRepository: VeterinariosRepository?,
    showMessage: (String) -> Unit
) {
    if (veterinariosRepository == null) {
        Text("Repo no injectado"); return
    }
    val scope = rememberCoroutineScope()
    val json = remember { mutableStateOf<String?>(null) }
    val vetId = remember { mutableStateOf("") }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Veterinarios (Admin)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    runCatching { veterinariosRepository.publicos() }.onSuccess {
                        json.value = it
                    }
                }
            }) { Text("Listar públicos") }
            OutlinedTextField(
                value = vetId.value,
                onValueChange = { vetId.value = it },
                label = { Text("vetId") })
            Button(onClick = {
                if (vetId.value.isBlank()) return@Button
                scope.launch {
                    runCatching { veterinariosRepository.verificar(vetId.value) }
                        .onSuccess { showMessage("Verificado") }
                        .onFailure { showMessage(it.message ?: "Error") }
                }
            }) { Text("Verificar") }
        }
        json.value?.let { Text(it.take(1200) + if (it.length > 1200) "..." else "") }
    }
}

@Composable
private fun InventarioAdminScreen(inventarioRepository: InventarioRepository?) {
    if (inventarioRepository == null) {
        Text("Repo no injectado"); return
    }
    val scope = rememberCoroutineScope()
    val productos = remember { mutableStateOf<List<ProductoDto>>(emptyList()) }
    val stock = remember { mutableStateOf<List<StockItemDto>>(emptyList()) }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Inventario (solo lectura)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    runCatching { inventarioRepository.productos() }.onSuccess {
                        productos.value = it
                    }
                }
            }) { Text("Productos") }
            Button(onClick = {
                scope.launch {
                    runCatching { inventarioRepository.stock() }.onSuccess {
                        stock.value = it
                    }
                }
            }) { Text("Stock") }
        }
        Spacer(Modifier.height(8.dp))
        if (productos.value.isNotEmpty()) {
            Text("Productos:")
            productos.value.take(20).forEach { p -> Text("• ${p.sku} - ${p.nombre}") }
        }
        if (stock.value.isNotEmpty()) {
            Text("Stock:")
            stock.value.take(20).forEach { s -> Text("• ${s.ubicacionId} - ${s.sku}: ${s.qty}") }
        }
    }
}
