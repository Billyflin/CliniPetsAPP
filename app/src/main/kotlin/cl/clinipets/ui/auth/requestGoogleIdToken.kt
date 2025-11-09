package cl.clinipets.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

suspend fun requestGoogleIdToken(context: Context, serverClientId: String): String? {
    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(serverClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val credential = try {
        credentialManager.getCredential(context, request).credential
    } catch (e: NoCredentialException) {
        // Sin credenciales disponibles (usuario canceló o no hay cuentas válidas)
        return null
    } catch (e: Exception) {
        // Cualquier otro error: no imprimir stacktrace para mantener la UI limpia
        return null
    }

    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
    return googleCredential.idToken
}
