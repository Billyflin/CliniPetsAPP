package cl.clinipets.network

import android.content.Context
import cl.clinipets.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    fun provideApiService(context: Context): ApiService {
        // use fully-qualified TokenStore to avoid resolution issues
        val tokenStore = cl.clinipets.auth.TokenStore(context)

        val logging = HttpLoggingInterceptor()
        logging.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE

        val authInterceptor = Interceptor { chain ->
            val reqBuilder = chain.request().newBuilder()
            val token = tokenStore.getToken()
            if (token != null && token.isNotEmpty()) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(reqBuilder.build())
        }

        val responseInterceptor = Interceptor { chain ->
            val response: Response = chain.proceed(chain.request())
            if (response.code == 401 || response.code == 403) {
                // clear token on unauthorized/forbidden globally
                tokenStore.clearToken()
            }
            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(responseInterceptor)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
