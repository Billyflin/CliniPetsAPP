// ui/screens/profile/ProfileViewModel.kt
package cl.clinipets.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.preferences.UserPreferences
import cl.clinipets.data.repository.ClinipetsRepository
import cl.clinipets.domain.auth.AuthRepository
import cl.clinipets.ui.theme.Contrast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val repository: ClinipetsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeUserData()
        observePreferences()
        loadStats()
    }

    private fun observeUserData() {
        viewModelScope.launch {
            combine(
                userPreferences.userDisplayName,
                userPreferences.userEmail,
                userPreferences.userPhotoUrl
            ) { name, email, photoUrl ->
                Triple(name, email, photoUrl)
            }.collect { (name, email, photoUrl) ->
                _uiState.update {
                    it.copy(
                        userName = name,
                        userEmail = email,
                        userPhotoUrl = photoUrl
                    )
                }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                userPreferences.isDarkMode,
                userPreferences.isDynamicColor,
                userPreferences.contrast
            ) { darkMode, dynamicColor, contrast ->
                Triple(darkMode, dynamicColor, contrast)
            }.collect { (darkMode, dynamicColor, contrast) ->
                _uiState.update {
                    it.copy(
                        isDarkMode = darkMode,
                        isDynamicColor = dynamicColor,
                        contrast = contrast
                    )
                }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            combine(
                repository.pets,
                repository.appointments
            ) { pets, appointments ->
                Pair(pets.size, appointments.size)
            }.collect { (petsCount, appointmentsCount) ->
                _uiState.update {
                    it.copy(
                        petsCount = petsCount,
                        appointmentsCount = appointmentsCount
                    )
                }
            }
        }
    }

    fun updateDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            userPreferences.setDarkMode(isDarkMode)
        }
    }

    fun updateDynamicColor(isDynamicColor: Boolean) {
        viewModelScope.launch {
            userPreferences.setDynamicColor(isDynamicColor)
        }
    }

    fun updateContrast(contrast: Contrast) {
        viewModelScope.launch {
            userPreferences.setContrast(contrast)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            userPreferences.clearUserData()
        }
    }
}

data class ProfileUiState(
    val userName: String? = null,
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
    val petsCount: Int = 0,
    val appointmentsCount: Int = 0,
    val memberSince: String = "2024",
    val isDarkMode: Boolean = false,
    val isDynamicColor: Boolean = true,
    val contrast: Contrast = Contrast.Standard,
    val isLoading: Boolean = false
)