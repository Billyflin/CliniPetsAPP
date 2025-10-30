package cl.clinipets.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Repositorio en memoria para tokens, sincronizado con TokenStore (DataStore cifrado).
 */
class TokenRepository(
    private val store: TokenStore,
    private val scope: CoroutineScope
) {
    private val _access = MutableStateFlow<String?>(null)
    val access: StateFlow<String?> = _access

    private val _refresh = MutableStateFlow<String?>(null)
    val refresh: StateFlow<String?> = _refresh

    init {
        scope.launch(Dispatchers.IO) {
            combine(store.accessToken, store.refreshToken) { a, r -> a to r }
                .collect { (a, r) ->
                    _access.value = a
                    _refresh.value = r
                }
        }
    }

    fun setAccess(token: String?) {
        _access.value = token
        if (token != null) {
            scope.launch(Dispatchers.IO) { store.updateAccessToken(token) }
        }
    }

    fun setTokens(access: String, refresh: String? = null) {
        _access.value = access
        _refresh.value = refresh
        scope.launch(Dispatchers.IO) {
            store.saveTokens(access, refresh ?: "")
        }
    }

    fun clear() {
        _access.value = null
        _refresh.value = null
        scope.launch(Dispatchers.IO) { store.clearAll() }
    }
}

