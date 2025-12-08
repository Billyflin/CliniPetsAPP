package cl.clinipets

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import cl.clinipets.core.session.SessionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject // <--- CAMBIO IMPORTANTE: usa javax, no jakarta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@HiltAndroidApp
class ClinipetsApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var session: SessionManager

    @Inject
    lateinit var coilOkHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        // Restaurar sesión al inicio
        CoroutineScope(Dispatchers.Default).launch {
            session.restoreIfAny()
        }
    }

    // Configuración Correcta para Coil 3
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(coilOkHttpClient))
            }
            .crossfade(true)
            .build()
    }
}
