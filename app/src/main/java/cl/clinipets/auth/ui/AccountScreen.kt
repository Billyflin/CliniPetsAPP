package cl.clinipets.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun AccountScreen(
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Text(text = "AccountScreen")
    }
}

@Preview(name = "AccountScreen")
@Composable
private fun PreviewAccountScreen() {
    AccountScreen()
}