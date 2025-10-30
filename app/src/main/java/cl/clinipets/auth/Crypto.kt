// cl/clinipets/auth/Crypto.kt
package cl.clinipets.auth

import android.content.Context
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

object Crypto {
    private const val KEYSET_NAME = "clinipets_aead_keyset"
    private const val PREF_FILE = "clinipets_aead_keyset_prefs"
    private const val MASTER_KEY_URI = "android-keystore://clinipets_master_key"

    fun aead(context: Context): Aead {
        // Inicializa Tink y el MasterKey
        AeadConfig.register()

        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        return keysetHandle.getPrimitive(Aead::class.java)
    }
}
