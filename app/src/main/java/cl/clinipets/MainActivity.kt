package cl.clinipets

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import cl.clinipets.core.security.TokenStorage
import cl.clinipets.data.NetworkModule
import cl.clinipets.data.api.AuthApi
import cl.clinipets.data.api.DiscoveryApi
import cl.clinipets.data.repositories.AuthRepositoryImpl
import cl.clinipets.data.repositories.DiscoveryRepositoryImpl
import cl.clinipets.ui.theme.ClinipetsTheme
import cl.clinipets.ui.AppRoot
import retrofit2.create
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

private const val LOG_TAG = "ClinipetsApp"
private const val TAIL_LEN = 8

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logAppIdAndSha256()
        logAppIdAndSha1()

        val tokenStorage = TokenStorage(this)
        val unauthorizedTick = mutableStateOf(0L)
        val mainHandler = Handler(Looper.getMainLooper())

        val okHttp = NetworkModule.okHttp(
            tokenProvider = { tokenStorage.getJwt() },
            onUnauthorized = {
                Log.w(LOG_TAG, "401 detectado. Limpiando JWT y notificando UI…")
                tokenStorage.setJwt(null)
                mainHandler.post { unauthorizedTick.value = unauthorizedTick.value + 1 }
            }
        )
        val retrofit = NetworkModule.retrofit(BuildConfig.BASE_URL, okHttp)
        val authApi = retrofit.create<AuthApi>()
        val discoveryApi = retrofit.create<DiscoveryApi>()
        val authRepo = AuthRepositoryImpl(authApi, tokenStorage)
        val discoveryRepo = DiscoveryRepositoryImpl(discoveryApi)
        val webClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID.trim()

        if (webClientId.isBlank()) {
            Log.e(LOG_TAG, "GOOGLE_SERVER_CLIENT_ID vacío. Configúralo en build.gradle.kts")
        } else {
            Log.d(LOG_TAG, "GOOGLE_SERVER_CLIENT_ID: ***${webClientId.takeLast(TAIL_LEN)} (long=${webClientId.length})")
        }

        setContent {
            ClinipetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        authRepository = authRepo,
                        discoveryRepository = discoveryRepo,
                        webClientId = webClientId,
                        unauthorizedSignal = unauthorizedTick.value
                    )
                }
            }
        }
    }

    private fun logAppIdAndSha1() {
        try {
            val pkg = packageName
            val pm = packageManager
            val pi = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            val signatures = pi.signingInfo?.apkContentsSigners
            val md = MessageDigest.getInstance("SHA-1")
            signatures?.forEachIndexed { index, sig ->
                val digest = md.digest(sig.toByteArray())
                val hex = digest.joinToString(":") { b -> "%02X".format(b) }
                Log.d(LOG_TAG, "Package=$pkg signingCert[$index] SHA-1=$hex")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(LOG_TAG, "Paquete no encontrado para SHA-1: ${e.message}")
        } catch (e: NoSuchAlgorithmException) {
            Log.w(LOG_TAG, "Algoritmo SHA-1 no disponible: ${e.message}")
        }
    }

    private fun logAppIdAndSha256() {
        try {
            val pkg = packageName
            val pm = packageManager
            val pi = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            val signatures = pi.signingInfo?.apkContentsSigners
            val md = MessageDigest.getInstance("SHA-256")
            signatures?.forEachIndexed { index, sig ->
                val digest = md.digest(sig.toByteArray())
                val hex = digest.joinToString(":") { b -> "%02X".format(b) }
                Log.d(LOG_TAG, "Package=$pkg signingCert[$index] SHA-256=$hex")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(LOG_TAG, "Paquete no encontrado para SHA-256: ${e.message}")
        } catch (e: NoSuchAlgorithmException) {
            Log.w(LOG_TAG, "Algoritmo SHA-256 no disponible: ${e.message}")
        }
    }
}