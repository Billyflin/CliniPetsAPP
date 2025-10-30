// cl/clinipets/auth/TokenStore.kt — primera implementación (DataStore + Tink)
package cl.clinipets.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.secureDataStore by preferencesDataStore(name = "secure_prefs")

class TokenStore(private val context: Context) {
    private val KEY_ACCESS = stringPreferencesKey("access_token_enc")
    private val KEY_REFRESH = stringPreferencesKey("refresh_token_enc")
    private val aead by lazy { Crypto.aead(context) }
    private val ds get() = context.secureDataStore

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        ds.edit { p ->
            p[KEY_ACCESS] = encrypt(accessToken)
            p[KEY_REFRESH] = encrypt(refreshToken)
        }
    }

    suspend fun updateAccessToken(accessToken: String) {
        ds.edit { it[KEY_ACCESS] = encrypt(accessToken) }
    }

    val accessToken: Flow<String?> = ds.data.map { p -> p[KEY_ACCESS]?.let(::decrypt) }
    val refreshToken: Flow<String?> = ds.data.map { p -> p[KEY_REFRESH]?.let(::decrypt) }

    suspend fun clearAll() {
        ds.edit { it.clear() }
    }

    private fun encrypt(plain: String): String {
        val ct = aead.encrypt(plain.toByteArray(), null)
        return android.util.Base64.encodeToString(
            ct,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
        )
    }

    private fun decrypt(enc: String): String? = runCatching {
        val bytes = android.util.Base64.decode(
            enc,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
        )
        val pt = aead.decrypt(bytes, null)
        String(pt)
    }.getOrNull()
}
