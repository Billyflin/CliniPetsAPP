package cl.clinipets.chat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ThreadListScreen(
    modifier: Modifier = Modifier,
    onThreadSelected: ((String) -> Unit)? = null
) {

    Box(modifier) {
        Text(text = "ThreadListScreen")
    }
}

@Preview(name = "ThreadListScreen")
@Composable
private fun PreviewThreadListScreen() {
    ThreadListScreen()
}