package cl.clinipets.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.preferences.UserPreferences
import cl.clinipets.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUiState(
    val navigationState: SplashNavigationState = SplashNavigationState.LOADING
)

enum class SplashNavigationState {
    LOADING,
    ONBOARDING,
    LOGIN,
    HOME
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkAuthenticationState()
    }

    private fun checkAuthenticationState() {
        viewModelScope.launch {
            // Combinar el estado de onboarding y autenticación
            combine(
                userPreferences.hasCompletedOnboarding,
                authRepository.isAuthenticated
            ) { hasCompletedOnboarding, isAuthenticated ->
                when {
                    !hasCompletedOnboarding -> SplashNavigationState.ONBOARDING
                    !isAuthenticated -> SplashNavigationState.LOGIN
                    else -> SplashNavigationState.HOME
                }
            }.collect { navigationState ->
                _uiState.update { it.copy(navigationState = navigationState) }
            }
        }
    }
}