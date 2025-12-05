package cl.clinipets.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.core.settings.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    // Exposes the raw preference (nullable)
    val isDarkMode: StateFlow<Boolean?> = settingsManager.isDarkModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun toggleDarkMode(currentValue: Boolean) {
        viewModelScope.launch {
            settingsManager.setDarkMode(!currentValue)
        }
    }
    
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setDarkMode(enabled)
        }
    }
}
