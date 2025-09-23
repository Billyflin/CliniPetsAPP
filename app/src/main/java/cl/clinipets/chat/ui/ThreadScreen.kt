package cl.clinipets.chat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ThreadScreen(
    modifier: Modifier = Modifier,
    conversationId: String = "test_conversation_id",
    onBack: () -> Boolean
) {
    Box(modifier) {
        Text(text = "ThreadScreen")
    }
}

@Preview(name = "ThreadScreen")
@Composable
private fun PreviewThreadScreen() {
    ThreadScreen( onBack = { true })
}