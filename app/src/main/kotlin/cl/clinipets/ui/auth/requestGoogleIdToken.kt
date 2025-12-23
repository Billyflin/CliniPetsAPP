package cl.clinipets.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import android.util.Log
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

private const val TAG = "GoogleAuth"

suspend fun requestGoogleIdToken(context: Context, serverClientId: String): String? {
    Log.d(TAG, "Requesting Google ID Token with Client ID: $serverClientId")
    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(serverClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val credential = try {
        val result = credentialManager.getCredential(context, request)
        result.credential
    } catch (e: NoCredentialException) {
        Log.w(TAG, "No credential available (cancelled or no accounts): ${e.message}")
        return null
    } catch (e: Exception) {
        Log.e(TAG, "Error requesting credential", e)
        return null
    }

    return try {
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val token = googleCredential.idToken
        Log.d(TAG, "Google ID Token retrieved. Length: ${token.length}, Prefix: ${token.take(10)}...")
        token
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing Google credential", e)
        null
    }
}
