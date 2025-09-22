package cl.clinipets.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRequestAttention: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenAccount: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Clinipets") }) }
    ) { pads ->
        Column(
            modifier = Modifier
                .padding(pads)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onRequestAttention, modifier = Modifier.fillMaxWidth()) {
                Text("Solicitar atención (Mapa)")
            }
            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                Text("Historial de mascotas")
            }
            OutlinedButton(onClick = onOpenChat, modifier = Modifier.fillMaxWidth()) {
                Text("Chat")
            }
            TextButton(onClick = onOpenAccount) {
                Text("Cuenta")
            }
        }
    }
}
