package cl.clinipets.appointments.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier,
    appointmentId: String
) {
    Box(modifier) {

        Text(text = "DetailsScreen for appointment: $appointmentId")


    }
}

@Preview(name = "DetailsScreen")
@Composable
private fun PreviewDetailsScreen() {
    DetailsScreen(
        appointmentId = "sample_appointment_id"
    )
}