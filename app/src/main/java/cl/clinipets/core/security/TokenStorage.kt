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

    fun getRoles(): List<String> {
        val csv = prefs.getString(KEY_ROLES_CSV, null) ?: return emptyList()
        return csv.split(',').mapNotNull { it.trim().ifBlank { null } }
    }

    fun setRoles(roles: List<String>) {
        val csv = roles.joinToString(",")
        prefs.edit().putString(KEY_ROLES_CSV, csv).apply()
        // Si el rol activo ya no pertenece a la lista, limpiarlo
        val active = getActiveRole()
        if (active != null && !roles.contains(active)) {
            setActiveRole(null)
        }
    }

    fun getActiveRole(): String? = prefs.getString(KEY_ACTIVE_ROLE, null)

    fun setActiveRole(role: String?) {
        prefs.edit().apply {
            if (role.isNullOrBlank()) remove(KEY_ACTIVE_ROLE) else putString(KEY_ACTIVE_ROLE, role)
        }.apply()
    }

    fun clear() { prefs.edit().clear().apply() }

    private companion object {
        const val PREFS_NAME = "clinipets.secure.prefs"
        const val KEY_JWT = "jwt"
        const val KEY_ROLES_CSV = "roles_csv"
        const val KEY_ACTIVE_ROLE = "active_role"
    }
}
