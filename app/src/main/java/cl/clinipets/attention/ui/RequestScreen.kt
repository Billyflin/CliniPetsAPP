package cl.clinipets.attention.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun RequestScreen(
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Text(text = "RequestScreen")
    }
}

@Preview(name = "RequestScreen")
@Composable
private fun PreviewRequestScreen() {
    RequestScreen()
}