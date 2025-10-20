package cl.clinipets.core.security

import android.content.Context
import android.content.SharedPreferences

class TokenStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveAccess(token: String) = prefs.edit().putString(KEY_ACCESS, token).apply()
    fun getAccess(): String? = prefs.getString(KEY_ACCESS, null)
    fun clear() = prefs.edit().clear().apply()

    private companion object { const val KEY_ACCESS = "access_token" }
}

