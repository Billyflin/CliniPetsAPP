package cl.clinipets.attention.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import cl.clinipets.attention.presentation.AttentionViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    vm: AttentionViewModel = hiltViewModel(), onRequestCreated: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()

    // 1) Inicializa el SDK de Maps una vez
    LaunchedEffect(Unit) {
        MapsInitializer.initialize(ctx)  // <- clave para evitar "CameraUpdateFactory is not initialized"
    }

    // 2) Permisos de ubicación
    val permission = Manifest.permission.ACCESS_FINE_LOCATION
    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission.value = granted
        if (granted) vm.loadLocation()
    }

    LaunchedEffect(Unit) {
        if (hasPermission.value) vm.loadLocation() else launcher.launch(permission)
    }

    // 3) Cámara
    val camPosState = rememberCameraPositionState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Solicitar atención " + state.location) }) },
    ) { pads ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pads)
        ) {
            when {
                state.location != null -> {
                    val loc = state.location!!
                    // Animar SOLO después de que MapsInitializer se ejecutó (arriba)
                    LaunchedEffect(loc) {
                        camPosState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(loc.lat, loc.lng), 17f
                            )
                        )
                    }
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = camPosState,
                        uiSettings = MapUiSettings(myLocationButtonEnabled = true),
                        properties = MapProperties(isMyLocationEnabled = hasPermission.value)
                    ) {
                        // markers/overlays si quieres
                    }
                }

                state.loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                else -> {
                    Text(
                        state.error ?: "Permite la ubicación para continuar",
                        Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
