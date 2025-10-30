package cl.clinipets.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import cl.clinipets.openapi.models.CrearReserva
import cl.clinipets.util.Result
import java.time.LocalDate
import java.util.UUID

@Composable
fun VetDetailScreen(navController: NavController, vetId: String, vetDetailViewModel: VetDetailViewModel = hiltViewModel()) {
    val vetDetailsState by vetDetailViewModel.vetDetailsState.collectAsState()
    val availabilityState by vetDetailViewModel.availabilityState.collectAsState()
    val createReservationState by vetDetailViewModel.createReservationState.collectAsState()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vetId) {
        vetDetailViewModel.fetchVetDetails(UUID.fromString(vetId))
        vetDetailViewModel.fetchAvailability(UUID.fromString(vetId), selectedDate)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (vetDetailsState) {
            is Result.Loading -> CircularProgressIndicator()
            is Result.Error -> Text(text = "Error: ${(vetDetailsState as Result.Error).exception.message}", color = androidx.compose.ui.graphics.Color.Red)
            is Result.Success -> {
                val vet = (vetDetailsState as Result.Success).data
                Text(text = "Veterinario: ${vet.nombre} ${vet.apellido}", style = MaterialTheme.typography.headlineMedium)
                Text(text = "Especialidad: ${vet.especialidad}")
                Text(text = "Modo de atención: ${vet.modo}")
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        Text(text = "Disponibilidad para ${selectedDate}:")
        when (availabilityState) {
            is Result.Loading -> CircularProgressIndicator()
            is Result.Error -> Text(text = "Error: ${(availabilityState as Result.Error).exception.message}", color = androidx.compose.ui.graphics.Color.Red)
            is Result.Success -> {
                val availability = (availabilityState as Result.Success).data
                if (availability.isEmpty()) {
                    Text(text = "No hay disponibilidad para esta fecha.")
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(availability) {
                            Text(text = "${it.horaInicio} - ${it.horaFin}",
                                modifier = Modifier.fillMaxWidth().clickable { selectedTimeSlot = it.horaInicio }
                                    .padding(vertical = 4.dp),
                                color = if (selectedTimeSlot == it.horaInicio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // TODO: Implement date picker for selectedDate
            // For now, just re-fetch for current date
            vetDetailViewModel.fetchAvailability(UUID.fromString(vetId), selectedDate)
        }) {
            Text(text = "Actualizar Disponibilidad")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            selectedTimeSlot?.let {
                // TODO: Get petId and procedureId from user selection
                val dummyPetId = UUID.randomUUID() // Replace with actual pet selection
                val dummyProcedureId = "PROC001" // Replace with actual procedure selection
                val crearReserva = CrearReserva(
                    veterinarioId = UUID.fromString(vetId),
                    mascotaId = dummyPetId,
                    procedimientoSku = dummyProcedureId,
                    fecha = selectedDate,
                    horaInicio = it,
                    modoAtencion = CrearReserva.ModoAtencion.DOMICILIO // TODO: Allow user to select
                )
                vetDetailViewModel.createReservation(crearReserva) { navController.popBackStack() }
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = createReservationState !is Result.Loading && selectedTimeSlot != null) {
            if (createReservationState is Result.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text("Creando Reserva...")
            } else {
                Text(text = "Reservar Cita")
            }
        }

        createReservationState.let { state ->
            if (state is Result.Error) {
                Text(text = "Error al crear reserva: ${state.exception.message}", color = androidx.compose.ui.graphics.Color.Red)
            }
        }
    }
}
