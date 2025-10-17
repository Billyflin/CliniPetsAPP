package cl.clinipets.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Construir dependencias aquí: ApiService y TokenStore
            val api = cl.clinipets.network.NetworkModule.provideApiService(context)
            val tokenStore = TokenStore(context)
            val repo = AuthRepository(api, tokenStore)
            return AuthViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
