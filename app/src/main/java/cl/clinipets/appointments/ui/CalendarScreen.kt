package cl.clinipets.appointments.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Text(text = "CalendarScreen")
    }
}

@Preview(name = "CalendarScreen")
@Composable
private fun PreviewCalendarScreen() {
    CalendarScreen()
}