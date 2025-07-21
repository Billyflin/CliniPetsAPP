// ui/viewmodels/SettingsViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.preferences.UserPreferences
import cl.clinipets.ui.theme.Contrast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState

    init {
        loadSettings()
        observePreferences()
    }

    private fun loadSettings() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()


            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferences.isDarkMode.collect { isDark ->
                _settingsState.value = _settingsState.value.copy(isDarkMode = isDark)
            }
        }

        viewModelScope.launch {
            userPreferences.isDynamicColor.collect { isDynamic ->
                _settingsState.value = _settingsState.value.copy(isDynamicColor = isDynamic)
            }
        }

        viewModelScope.launch {
            userPreferences.contrast.collect { contrast ->
                _settingsState.value = _settingsState.value.copy(contrast = contrast)
            }
        }
    }

    fun setDarkMode(value: Boolean) {
        viewModelScope.launch {
            userPreferences.setDarkMode(value)
            updateFirestoreSettings()
        }
    }

    fun setDynamicColor(value: Boolean) {
        viewModelScope.launch {
            userPreferences.setDynamicColor(value)
            updateFirestoreSettings()
        }
    }

    fun setContrast(value: Contrast) {
        viewModelScope.launch {
            userPreferences.setContrast(value)
            updateFirestoreSettings()
        }
    }

    fun setNotifications(value: Boolean) {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(notificationsEnabled = value)
            updateFirestoreSettings()
        }
    }

    fun setReminderHours(hours: Int) {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(reminderHours = hours)
            updateFirestoreSettings()
        }
    }

    private suspend fun updateFirestoreSettings() {
        val userId = auth.currentUser?.uid ?: return

        try {
            val settings = hashMapOf(
                "isDarkMode" to userPreferences.isDarkMode.first(),
                "isDynamicColor" to userPreferences.isDynamicColor.first(),
                "contrast" to userPreferences.contrast.first().name,
                "notificationsEnabled" to _settingsState.value.notificationsEnabled,
                "reminderHours" to _settingsState.value.reminderHours
            )

            firestore.collection("users")
                .document(userId)
                .update("settings", settings)
                .await()
        } catch (e: Exception) {
            // Error handling
        }
    }
}

data class SettingsState(
    val isDarkMode: Boolean = false,
    val isDynamicColor: Boolean = true,
    val contrast: Contrast = Contrast.Standard,
    val notificationsEnabled: Boolean = true,
    val reminderHours: Int = 24,
    val userRole: String = "CLIENT",
    val isLoading: Boolean = false,
    val error: String? = null
)