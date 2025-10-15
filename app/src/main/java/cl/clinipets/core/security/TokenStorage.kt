package cl.clinipets.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStorage(context: Context) {
    private val prefs = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getJwt(): String? = prefs.getString(KEY_JWT, null)
    fun setJwt(token: String?) {
        prefs.edit().apply {
            if (token.isNullOrBlank()) remove(KEY_JWT) else putString(KEY_JWT, token)
        }.apply()
    }
    fun clear() { prefs.edit().clear().apply() }

    private companion object {
        const val PREFS_NAME = "clinipets.secure.prefs"
        const val KEY_JWT = "jwt"
    }
}

