package cl.clinipets.auth

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class EncryptedCookieJar(private val context: Context) : CookieJar {

    // This is a placeholder implementation. 
    // A real implementation would use AndroidX Security Crypto to encrypt and store cookies securely.
    // For now, it's an in-memory cookie jar.
    private val allCookies = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        allCookies.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return allCookies.filter { it.matches(url) }
    }
}
