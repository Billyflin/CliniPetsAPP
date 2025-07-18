package cl.clinipets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.preferences.UserPreferences
import cl.clinipets.domain.auth.AuthRepository
import cl.clinipets.ui.theme.Contrast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        // Observe authentication state
        viewModelScope.launch {
            authRepository.isAuthenticated
                .collect { isAuthenticated ->
                    _appState.update { it.copy(isAuthenticated = isAuthenticated) }
                }
        }

        // Observe user preferences
        viewModelScope.launch {
            combine(
                userPreferences.isDarkMode,
                userPreferences.isDynamicColor,
                userPreferences.contrast,
                userPreferences.hasCompletedOnboarding
            ) { isDarkMode, isDynamicColor, contrast, hasCompletedOnboarding ->
                AppState(
                    isAuthenticated = _appState.value.isAuthenticated,
                    isDarkMode = isDarkMode,
                    isDynamicColor = isDynamicColor,
                    contrast = contrast,
                    hasCompletedOnboarding = hasCompletedOnboarding
                )
            }.collect { state ->
                _appState.value = state
            }
        }
    }

    fun updateAuthState(isAuthenticated: Boolean) {
        viewModelScope.launch {
            _appState.update { it.copy(isAuthenticated = isAuthenticated) }
        }
    }

    fun updateThemeSettings(
        isDarkMode: Boolean? = null,
        isDynamicColor: Boolean? = null,
        contrast: Contrast? = null
    ) {
        viewModelScope.launch {
            isDarkMode?.let { userPreferences.setDarkMode(it) }
            isDynamicColor?.let { userPreferences.setDynamicColor(it) }
            contrast?.let { userPreferences.setContrast(it) }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferences.setOnboardingCompleted(true)
        }
    }
}

data class AppState(
    val isAuthenticated: Boolean = false,
    val isDarkMode: Boolean = false,
    val isDynamicColor: Boolean = true,
    val contrast: Contrast = Contrast.Standard,
    val hasCompletedOnboarding: Boolean = false
)
