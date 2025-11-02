package cl.clinipets.ui.mascotas

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.UUID



@Composable
fun MascotaFormScreen(
    mascotaId: UUID,
    onBack: () -> Unit,
    vm: MascotasViewModel = hiltViewModel()
) {

    Text(
        text = "MascotaFormScreen",
    )

    Text(
        text = "mascotaId: $mascotaId",
    )
}