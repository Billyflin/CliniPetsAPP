// cl/clinipets/attention/ui/ConfirmScreen.kt
package cl.clinipets.attention.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.clinipets.attention.model.VetLite
import cl.clinipets.core.data.model.common.GeoPoint
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun ConfirmScreen(
    modifier: Modifier = Modifier,
    vet: VetLite,
    onConfirm: (VetLite) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    Surface(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Confirmar solicitud",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LabeledValue(label = "Profesional", value = vet.name)
                    LabeledValue(label = "ID", value = vet.id)
                    vet.rating?.let { LabeledValue(label = "Rating", value = String.format("%.1f ★", it)) }
                    vet.distanceMeters?.let { meters ->
                        val km = meters / 1000.0
                        val pretty = if (km >= 1) "${String.format("%.1f", km)} km" else "${meters} m"
                        LabeledValue(label = "Distancia", value = pretty)
                    }
                    vet.location?.let { loc ->
                        LabeledValue(
                            label = "Ubicación",
                            value = "lat=${loc.lat.format(6)}, lng=${loc.lng.format(6)}"
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Revisa los datos antes de continuar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                ActionRow(
                    onCancel = onCancel,
                    onConfirm = { onConfirm(vet) }
                )
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActionRow(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text("Confirmar")
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text("Volver")
        }
    }
}

private fun Double.format(digits: Int): String {
    val factor = 10.0.pow(digits)
    val rounded = (this * factor).roundToInt() / factor
    return "%.${digits}f".format(rounded)
}

private fun Double.pow(exp: Int): Double = this.pow(exp.toDouble())

@Preview(name = "ConfirmScreen")
@Composable
private fun PreviewConfirmScreen() {
    ConfirmScreen(
        vet = VetLite(
            id = "vet-1",
            name = "Dra. Paula",
            rating = 4.6,
            distanceMeters = 680,
            location = GeoPoint(lat = -38.74241702002798, lng = -72.62570115890024),
        )
    )
}
