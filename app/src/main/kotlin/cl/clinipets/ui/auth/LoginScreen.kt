package cl.clinipets.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource // <-- ¡IMPORTE CLAVE!
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.clinipets.R // <-- ¡IMPORTE CLAVE!
import cl.clinipets.core.di.ApiModule.resolveBaseUrl

// Forma expresiva para el botón
private val buttonShape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)

@Composable
fun LoginScreen(
    busy: Boolean,
    error: String?,
    onLoginClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. Sección de Bienvenida (Logo y Título) ---
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo de Clinipets",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(32.dp))
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Bienvenido a Clinipets",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tu app de gestión veterinaria.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- 2. Sección de Acción (Error y Botón) ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mensaje de Error
                AnimatedVisibility(visible = error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error ?: "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // [CAMBIO] Botón actualizado para usar painterResource
                OutlinedButton(
                    onClick = onLoginClick,
                    enabled = !busy,
                    shape = buttonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        // Se usa el nuevo XML de drawable
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Logo de Google",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified // ¡Importante para que no se tiña de gris!
                        )
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Iniciar sesión con Google")
                    }
                }

                // Texto de debug (base URL)
                Text(
                    text = resolveBaseUrl(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}