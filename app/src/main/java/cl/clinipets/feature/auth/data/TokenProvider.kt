package cl.clinipets.feature.auth.data

import cl.clinipets.feature.auth.domain.Sesion
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Singleton
class TokenProvider @Inject constructor(
    private val sesionLocal: SesionLocalDataSource,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sesion: Flow<Sesion?> = sesionLocal.sesion

    @Volatile
    private var tokenActual: String? = null

    init {
        scope.launch {
            sesionLocal.sesion.collect { sesion ->
                tokenActual = sesion?.token
            }
        }
    }

    fun obtenerTokenActual(): String? = tokenActual

    fun invalidarSesionPorError() {
        tokenActual = null
        scope.launch {
            sesionLocal.limpiarSesion()
        }
    }
}
