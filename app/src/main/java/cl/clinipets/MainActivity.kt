package cl.clinipets

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
import cl.clinipets.data.api.AgendaApi
import cl.clinipets.data.api.AuthApi
import cl.clinipets.data.api.CatalogoApi
import cl.clinipets.data.api.DiscoveryApi
import cl.clinipets.data.api.InventarioApi
import cl.clinipets.data.api.MascotasApi
import cl.clinipets.data.api.VeterinariosApi
import cl.clinipets.data.repositories.AgendaRepositoryImpl
import cl.clinipets.data.repositories.AuthRepositoryImpl
import cl.clinipets.data.repositories.CatalogoRepositoryImpl
import cl.clinipets.data.repositories.DiscoveryRepositoryImpl
import cl.clinipets.data.repositories.InventarioRepositoryImpl
import cl.clinipets.data.repositories.MascotasRepositoryImpl
import cl.clinipets.data.repositories.VeterinariosRepositoryImpl
import cl.clinipets.ui.theme.ClinipetsTheme
import retrofit2.create

private const val LOG_TAG = "ClinipetsApp"
private const val TAIL_LEN = 8

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenStorage = TokenStorage(this)
        val unauthorizedTick = mutableStateOf(0L)
        val forbiddenTick = mutableStateOf(0L)
        val mainHandler = Handler(Looper.getMainLooper())

        val okHttp = NetworkModule.okHttp(
            tokenProvider = { tokenStorage.getJwt() },
            onUnauthorized = {
                Log.w(LOG_TAG, "401 detectado. Limpiando JWT y notificando UI…")
                tokenStorage.setJwt(null)
                mainHandler.post { unauthorizedTick.value = unauthorizedTick.value + 1 }
            },
            onForbidden = {
                Log.w(LOG_TAG, "403 detectado. Notificando UI…")
                mainHandler.post { forbiddenTick.value = forbiddenTick.value + 1 }
            }
        )
        val retrofit = NetworkModule.retrofit(BuildConfig.BASE_URL, okHttp)

        // APIs
        val authApi = retrofit.create<AuthApi>()
        val discoveryApi = retrofit.create<DiscoveryApi>()
        val mascotasApi = retrofit.create<MascotasApi>()
        val agendaApi = retrofit.create<AgendaApi>()
        val catalogoApi = retrofit.create<CatalogoApi>()
        val veterinariosApi = retrofit.create<VeterinariosApi>()
        val inventarioApi = retrofit.create<InventarioApi>()

        // Repos
        val authRepo = AuthRepositoryImpl(authApi, tokenStorage)
        val discoveryRepo = DiscoveryRepositoryImpl(discoveryApi)
        val mascotasRepo = MascotasRepositoryImpl(mascotasApi)
        val agendaRepo = AgendaRepositoryImpl(agendaApi)
        val catalogoRepo = CatalogoRepositoryImpl(catalogoApi)
        val veterinariosRepo = VeterinariosRepositoryImpl(veterinariosApi)
        val inventarioRepo = InventarioRepositoryImpl(inventarioApi)

        val webClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID.trim()

        if (webClientId.isBlank()) {
            Log.e(LOG_TAG, "GOOGLE_SERVER_CLIENT_ID vacío. Configúralo en build.gradle.kts")
        } else {
            Log.d(LOG_TAG, "GOOGLE_SERVER CLIENT_ID: ***${webClientId.takeLast(TAIL_LEN)} (long=${webClientId.length})")
        }

        setContent {
            ClinipetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRootEntry(
                        authRepository = authRepo,
                        discoveryRepository = discoveryRepo,
                        webClientId = webClientId,
                        unauthorizedSignal = unauthorizedTick.value,
                        mascotasRepository = mascotasRepo,
                        agendaRepository = agendaRepo,
                        catalogoRepository = catalogoRepo,
                        forbiddenSignal = forbiddenTick.value,
                        veterinariosRepository = veterinariosRepo,
                        inventarioRepository = inventarioRepo,
                    )
                }
            }
        }
    }

}