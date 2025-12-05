package cl.clinipets

import android.os.Bundle
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cl.clinipets.ui.ClinipetsApp
import cl.clinipets.ui.settings.SettingsViewModel
import cl.clinipets.ui.theme.ClinipetsTheme
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.messaging.FirebaseMessaging

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsViewModel: SettingsViewModel by viewModels()
        
        setContent {
            val isDarkPref by settingsViewModel.isDarkMode.collectAsState()
            val isDark = isDarkPref ?: isSystemInDarkTheme()

            RequestNotificationPermissionAndLogToken()

            ClinipetsTheme(darkTheme = isDark) {
                ClinipetsApp()
            }
        }
    }
}

@Composable
private fun RequestNotificationPermissionAndLogToken() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            Log.d("ClinipetsFCM", "POST_NOTIFICATIONS permission granted? $granted")
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d("ClinipetsFCM", "FCM Token Actual: $token")
            }
            .addOnFailureListener { error ->
                Log.e("ClinipetsFCM", "No se pudo obtener el token FCM", error)
            }
    }
}
