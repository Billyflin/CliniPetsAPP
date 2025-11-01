package cl.clinipets.feature.auth.data

import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import cl.clinipets.core.Resultado
import javax.inject.Inject
import javax.inject.Singleton

interface GoogleAuthProvider {
    fun getSignInIntent(): Intent
    fun extractIdTokenFromIntent(data: Intent?): Resultado<String>
    fun signOut()
}

@Singleton
class GoogleAuthProviderImpl @Inject constructor(
    private val client: GoogleSignInClient,
) : GoogleAuthProvider {

    override fun getSignInIntent(): Intent = client.signInIntent

    override fun extractIdTokenFromIntent(data: Intent?): Resultado<String> {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val token = account?.idToken
            if (token.isNullOrBlank()) {
                Resultado.Error(
                    tipo = Resultado.Tipo.CLIENTE,
                    mensaje = "Google no entregó un ID token válido.",
                )
            } else {
                Resultado.Exito(token)
            }
        } catch (api: ApiException) {
            Resultado.Error(
                tipo = Resultado.Tipo.CLIENTE,
                mensaje = api.localizedMessage ?: "Error al autenticar con Google.",
                causa = api,
                codigoHttp = api.statusCode,
            )
        } catch (throwable: Throwable) {
            Resultado.Error(
                tipo = Resultado.Tipo.DESCONOCIDO,
                mensaje = throwable.localizedMessage ?: "Error inesperado al autenticar con Google.",
                causa = throwable,
            )
        }
    }

    override fun signOut() {
        client.signOut()
    }
}
