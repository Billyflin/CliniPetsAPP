package cl.clinipets.network

import android.content.Context
import cl.clinipets.BuildConfig
import cl.clinipets.auth.AuthEvents
import okhttp3.Authenticator
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    fun provideApiService(context: Context): ApiService {
        val tokenStore = cl.clinipets.auth.TokenStore(context)

        val logging = HttpLoggingInterceptor()
        logging.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE

        val cookieJar = PersistentCookieJar(context)

        val authInterceptor = Interceptor { chain ->
            val req = chain.request()
            val path = req.url.encodedPath
            if (path.endsWith("/api/auth/refresh") || path.endsWith("/api/auth/google")) {
                return@Interceptor chain.proceed(req)
            }
            val access = tokenStore.getAccessToken() ?: tokenStore.getToken()
            val newReq = if (!access.isNullOrEmpty()) {
                req.newBuilder().addHeader("Authorization", "Bearer $access").build()
            } else req
            chain.proceed(newReq)
        }

        fun newBaseClient(): OkHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val baseClient = newBaseClient()

        val tokenAuthenticator = Authenticator { _, response ->
            if (responseCount(response) >= 1) return@Authenticator null

            val originalRequest = response.request
            val path = originalRequest.url.encodedPath
            if (path.endsWith("/api/auth/refresh") || path.endsWith("/api/auth/google")) {
                return@Authenticator null
            }

            try {
                val refreshToken = tokenStore.getRefreshToken()
                val refreshUrl = BuildConfig.BASE_URL.trimEnd('/') + "/api/auth/refresh"
                val mediaType = "application/json".toMediaTypeOrNull()
                val bodyStr = if (!refreshToken.isNullOrBlank()) {
                    // Nuevo flujo: enviar refreshToken en el body
                    JSONObject().put("refreshToken", refreshToken).toString()
                } else {
                    // Compatibilidad: algunos backends refrescan por cookie
                    "{}"
                }
                val refreshRequest = Request.Builder()
                    .url(refreshUrl)
                    .post(bodyStr.toRequestBody(mediaType))
                    .build()

                val refreshResp = baseClient.newCall(refreshRequest).execute()
                if (!refreshResp.isSuccessful) {
                    tokenStore.clearAll()
                    AuthEvents.notifySessionExpired()
                    return@Authenticator null
                }
                val payload = refreshResp.body?.string() ?: run {
                    tokenStore.clearAll()
                    AuthEvents.notifySessionExpired()
                    return@Authenticator null
                }

                // Intentar parsear ambos formatos: {accessToken, refreshToken} o {token}
                val json = try { JSONObject(payload) } catch (_: Exception) { null }
                val newAccess = json?.optString("accessToken")?.takeIf { it.isNotBlank() }
                    ?: json?.optString("token")?.takeIf { it.isNotBlank() }
                val newRefresh = json?.optString("refreshToken")?.takeIf { it.isNotBlank() }

                if (newAccess.isNullOrBlank()) {
                    tokenStore.clearAll()
                    AuthEvents.notifySessionExpired()
                    return@Authenticator null
                }

                if (!newRefresh.isNullOrBlank()) {
                    tokenStore.saveTokens(newAccess, newRefresh)
                } else {
                    tokenStore.updateAccessToken(newAccess)
                }

                return@Authenticator originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newAccess")
                    .build()
            } catch (_: Exception) {
                tokenStore.clearAll()
                AuthEvents.notifySessionExpired()
                null
            }
        }

        val client = baseClient.newBuilder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(ApiService::class.java)
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }
}

// CookieJar persistente usando SharedPreferences con org.json
private class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs = context.getSharedPreferences("clinipets_cookies", Context.MODE_PRIVATE)
    private val key = "cookies_v1"

    @Volatile private var cache: MutableMap<String, MutableList<Cookie>> = load()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val host = url.host
        val list = (cache[host] ?: mutableListOf()).apply {
            for (c in cookies) removeAll { it.name == c.name && it.matches(url) }
            cookies.filter { !it.hasExpired() }.forEach { add(it) }
        }
        cache[host] = list
        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        pruneExpired()
        val all = cache.flatMap { it.value }
        return all.filter { it.matches(url) }
    }

    private fun Cookie.hasExpired(): Boolean = expiresAt < System.currentTimeMillis()

    @Synchronized
    private fun persist() {
        val root = JSONObject()
        cache.forEach { (host, list) ->
            val arr = JSONArray()
            list.forEach { c -> arr.put(encodeObj(c)) }
            root.put(host, arr)
        }
        prefs.edit().putString(key, root.toString()).apply()
    }

    @Synchronized
    private fun load(): MutableMap<String, MutableList<Cookie>> {
        val json = prefs.getString(key, null) ?: return mutableMapOf()
        return try {
            val root = JSONObject(json)
            val result = mutableMapOf<String, MutableList<Cookie>>()
            val keysIter = root.keys()
            while (keysIter.hasNext()) {
                val hostKey = keysIter.next() as String
                val arr = root.optJSONArray(hostKey) ?: continue
                val list = mutableListOf<Cookie>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    decodeObj(obj)?.let { list.add(it) }
                }
                if (list.isNotEmpty()) result[hostKey] = list
            }
            result
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    @Synchronized
    private fun pruneExpired() {
        var changed = false
        cache.forEach { (_, list) ->
            val it = list.iterator()
            while (it.hasNext()) {
                if (it.next().hasExpired()) { it.remove(); changed = true }
            }
        }
        if (changed) persist()
    }

    private fun encodeObj(cookie: Cookie): JSONObject {
        val obj = JSONObject()
        obj.put("name", cookie.name)
        obj.put("value", cookie.value)
        obj.put("expiresAt", cookie.expiresAt)
        obj.put("domain", cookie.domain)
        obj.put("path", cookie.path)
        obj.put("secure", cookie.secure)
        obj.put("httpOnly", cookie.httpOnly)
        return obj
    }

    private fun decodeObj(obj: JSONObject): Cookie? {
        return try {
            val nameStr = obj.optString("name")
            if (nameStr.isNullOrEmpty()) return null
            val valueStr = obj.optString("value")
            if (valueStr.isNullOrEmpty()) return null
            val expiresAt = obj.optLong("expiresAt", 0L).takeIf { it > 0 } ?: return null
            val domainStr = obj.optString("domain")
            if (domainStr.isNullOrEmpty()) return null
            val path = obj.optString("path", "/")
            val secure = obj.optBoolean("secure", false)
            val httpOnly = obj.optBoolean("httpOnly", false)
            Cookie.Builder()
                .name(nameStr)
                .value(valueStr)
                .domain(domainStr)
                .path(path)
                .expiresAt(expiresAt)
                .apply { if (secure) secure() }
                .apply { if (httpOnly) httpOnly() }
                .build()
        } catch (_: Exception) { null }
    }
}
