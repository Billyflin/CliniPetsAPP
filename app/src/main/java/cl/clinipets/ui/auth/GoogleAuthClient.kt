package cl.clinipets.ui.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

private const val TAG = "GoogleAuthClient"

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data class Cancelled(val reason: String? = null) : GoogleSignInResult()
    data class NoCredentials(val message: String? = null) : GoogleSignInResult()
    data class ConfigurationError(val message: String) : GoogleSignInResult()
    data class Unexpected(val message: String, val cause: Throwable? = null) : GoogleSignInResult()
}

class GoogleAuthClient {

    suspend fun signIn(activity: Activity, webClientId: String): GoogleSignInResult {
        val serverClientId = webClientId.trim()
        if (serverClientId.isBlank()) {
            val msg = "webClientId vacío; define google_web_client_id con un OAuth Client ID de tipo Web."
            Log.e(TAG, msg)
            return GoogleSignInResult.ConfigurationError(msg)
        }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val cm = CredentialManager.create(activity)
            Log.d(TAG, "Invocando CredentialManager.getCredential con GoogleIdOption…")
            val result = cm.getCredential(activity, request)
            val credential = result.credential
            Log.d(TAG, "Credencial recibida de tipo=${credential.type}")

            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleCred.idToken
            Log.d(TAG, "Token obtenido: present=${idToken.isNotEmpty()} len=${idToken.length}")
            if (idToken.isEmpty()) {
                GoogleSignInResult.ConfigurationError(
                    "No se obtuvo idToken. Revisa que uses un Client ID de tipo Web y que la SHA-256 esté configurada."
                )
            } else {
                GoogleSignInResult.Success(idToken)
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Flujo cancelado por el sistema/usuario: ${e.message}", e)
            GoogleSignInResult.Cancelled(e.message)
        } catch (e: NoCredentialException) {
            Log.w(TAG, "Sin credenciales elegibles en el dispositivo: ${e.message}", e)
            GoogleSignInResult.NoCredentials(e.message)
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Fallo parseando GoogleIdTokenCredential: ${e.message}", e)
            GoogleSignInResult.ConfigurationError("Token inválido o respuesta malformada de Google.")
        } catch (e: GetCredentialException) {
            val msg = e.errorMessage ?: e.javaClass.simpleName
            Log.e(TAG, "Error al obtener credencial: $msg", e)
            GoogleSignInResult.ConfigurationError("No se pudo obtener credencial: $msg")
        } catch (e: Exception) {
            Log.e(TAG, "Excepción inesperada en signIn: ${e.message}", e)
            GoogleSignInResult.Unexpected("Error inesperado: ${e.message}", e)
        }
    }
}
