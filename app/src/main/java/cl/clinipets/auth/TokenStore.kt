package cl.clinipets.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(private val context: Context) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "clinipets_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback simple prefs
            context.getSharedPreferences("clinipets_prefs", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_LEGACY_ACCESS = "key_jwt" // compatibilidad anterior
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }

    // Compatibilidad: antes solo se guardaba un token (lo tratamos como access)
    fun saveToken(token: String) {
        prefs.edit()
            .putString(KEY_LEGACY_ACCESS, token)
            .putString(KEY_ACCESS, token)
            .apply()
    }

    fun getToken(): String? = getAccessToken() ?: prefs.getString(KEY_LEGACY_ACCESS, null)

    fun clearToken() {
        prefs.edit()
            .remove(KEY_LEGACY_ACCESS)
            .remove(KEY_ACCESS)
            .apply()
    }

    // Nuevo flujo access/refresh
    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .putString(KEY_LEGACY_ACCESS, accessToken)
            .apply()
    }

    fun updateAccessToken(accessToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_LEGACY_ACCESS, accessToken)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun clearAll() {
        prefs.edit()
            .remove(KEY_LEGACY_ACCESS)
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .apply()
    }
}
