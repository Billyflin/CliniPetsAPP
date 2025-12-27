package cl.clinipets.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import android.util.Log
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

private const val TAG = "GoogleAuth"

suspend fun requestGoogleIdToken(context: Context): String? {
    // Intentamos obtener el ID generado automáticamente por el plugin de Google
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    val serverClientId = if (resId != 0) context.getString(resId) else null

    Log.d(TAG, ">>> INICIO: requestGoogleIdToken")
    
    if (serverClientId == null) {
        Log.e(TAG, "ERROR CRÍTICO: No se encontró 'default_web_client_id'.")
        Log.e(TAG, "Asegúrate de: 1. Habilitar Google Sign-In en Firebase. 2. Descargar el nuevo google-services.json")
        return null
    }

    Log.d(TAG, "Usando Client ID automático: $serverClientId")
    
    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(serverClientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    Log.d(TAG, "Llamando a credentialManager.getCredential...")
    val credential = try {
        val result = credentialManager.getCredential(context, request)
        Log.d(TAG, "Resultado obtenido de CredentialManager: ${result.credential.type}")
        result.credential
    } catch (e: NoCredentialException) {
        Log.e(TAG, "ERROR: No hay credenciales disponibles (¿Usuario canceló o no hay cuentas Google?)", e)
        return null
    } catch (e: Exception) {
        Log.e(TAG, "ERROR CRÍTICO en CredentialManager: ${e.javaClass.simpleName} - ${e.message}", e)
        return null
    }

    return try {
        Log.d(TAG, "Parseando GoogleIdTokenCredential...")
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val token = googleCredential.idToken
        Log.d(TAG, "¡ÉXITO! Token de Google obtenido (Largo: ${token.length})")
        token
    } catch (e: Exception) {
        Log.e(TAG, "ERROR al parsear la credencial de Google: ${e.message}", e)
        null
    }
}
