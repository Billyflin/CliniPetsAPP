package cl.clinipets.auth.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
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
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }

    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
    return googleCredential.idToken
}
