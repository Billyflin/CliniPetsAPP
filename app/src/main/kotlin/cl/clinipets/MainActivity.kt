package cl.clinipets

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageManager
import java.security.MessageDigest
import cl.clinipets.ui.ClinipetsApp
import cl.clinipets.ui.settings.SettingsViewModel
import cl.clinipets.ui.theme.ClinipetsTheme
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var deepLinkType by mutableStateOf<String?>(null)
    private var deepLinkTargetId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logAppSignatures()
        val settingsViewModel: SettingsViewModel by viewModels()
        handleDeepLinkIntent(intent)

        setContent {
            val isDarkPref by settingsViewModel.isDarkMode.collectAsState()
            val isDark = isDarkPref ?: isSystemInDarkTheme()

            RequestNotificationPermissionAndLogToken()

            ClinipetsTheme(darkTheme = isDark) {
                ClinipetsApp(
                    deepLinkType = deepLinkType,
                    deepLinkTargetId = deepLinkTargetId,
                    onDeepLinkConsumed = {
                        deepLinkType = null
                        deepLinkTargetId = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val type = intent?.getStringExtra("type")
        val citaId = intent?.getStringExtra("citaId")
        if (!type.isNullOrBlank()) {
            deepLinkType = type
            deepLinkTargetId = citaId
        }
    }

    private fun logAppSignatures() {
        try {
            val packageName = packageName
            val packageManager = packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                info.signatures
            }

            signatures?.forEach { signature ->
                val sha1 = getFingerprint(signature.toByteArray(), "SHA-1")
                val sha256 = getFingerprint(signature.toByteArray(), "SHA-256")
                Log.d("ClinipetsSignatures", "-----------------------------------------")
                Log.d("ClinipetsSignatures", "Package: $packageName")
                Log.d("ClinipetsSignatures", "SHA-1:   $sha1")
                Log.d("ClinipetsSignatures", "SHA-256: $sha256")
                Log.d("ClinipetsSignatures", "-----------------------------------------")
            }
        } catch (e: Exception) {
            Log.e("ClinipetsSignatures", "Error al obtener firmas", e)
        }
    }

    private fun getFingerprint(bytes: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(bytes)
        return digest.joinToString(":") { String.format("%02X", it) }
    }
}

@Composable
private fun RequestNotificationPermissionAndLogToken() {
    LocalContext.current
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
