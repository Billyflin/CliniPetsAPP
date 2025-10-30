package cl.clinipets.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import cl.clinipets.openapi.models.Reserva
import cl.clinipets.util.Result

@Composable
fun MyReservationsScreen(navController: NavController, myReservationsViewModel: MyReservationsViewModel = hiltViewModel()) {
    val reservationsState by myReservationsViewModel.reservationsState.collectAsState()
    val updateReservationState by myReservationsViewModel.updateReservationState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Mis Reservas", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        when (reservationsState) {
            is Result.Loading -> CircularProgressIndicator()
            is Result.Error -> Text(text = "Error: ${(reservationsState as Result.Error).exception.message}", color = androidx.compose.ui.graphics.Color.Red)
            is Result.Success -> {
                val reservations = (reservationsState as Result.Success).data
                if (reservations.isEmpty()) {
                    Text(text = "No tienes reservas.")
                } else {
                    LazyColumn {
                        items(reservations) {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(text = "Veterinario: ${it.veterinario.id}") // TODO: Fetch vet name
                                Text(text = "Mascota: ${it.mascota.id}") // TODO: Fetch pet name
                                Text(text = "Procedimiento: ${it.procedimientoSku}")
                                Text(text = "Fecha: ${it.inicio}")
                                Text(text = "Hora: ${it.inicio}")
                                Text(text = "Estado: ${it.estado}")
                                if (it.estado == Reserva.Estado.PENDIENTE || it.estado == Reserva.Estado.CONFIRMADA) {
                                    Button(onClick = {
                                        it.id?.let { reservationId ->
                                            myReservationsViewModel.cancelReservation(reservationId) {
                                                myReservationsViewModel.fetchMyReservations() // Refresh list
                                            }
                                        }
                                    }, enabled = updateReservationState !is Result.Loading) {
                                        if (updateReservationState is Result.Loading) {
                                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                                            Text("Cancelando...")
                                        } else {
                                            Text("Cancelar Reserva")
                                        }
                                    }
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }

        updateReservationState.let { state ->
            if (state is Result.Error) {
                Text(text = "Error al actualizar reserva: ${state.exception.message}", color = androidx.compose.ui.graphics.Color.Red)
            }
        }
    }
}
