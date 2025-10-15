package cl.clinipets

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
        val okHttp = NetworkModule.okHttp { tokenStorage.getJwt() }
        val retrofit = NetworkModule.retrofit(BuildConfig.BASE_URL, okHttp)
        val authApi = retrofit.create<AuthApi>()
        val discoveryApi = retrofit.create<DiscoveryApi>()
        val authRepo = AuthRepositoryImpl(authApi, tokenStorage)
        val discoveryRepo = DiscoveryRepositoryImpl(discoveryApi)
        val webClientId = getString(R.string.google_web_client_id).trim()

        if (webClientId.isBlank()) {
            Log.e(LOG_TAG, "google_web_client_id vacío. Revisa res/values/auth.xml")
        } else {
            Log.d(LOG_TAG, "google_web_client_id: ***${webClientId.takeLast(TAIL_LEN)} (long=${webClientId.length})")
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
                        webClientId = webClientId
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