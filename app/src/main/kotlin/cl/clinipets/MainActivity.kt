package cl.clinipets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cl.clinipets.ui.ClinipetsApp
import cl.clinipets.ui.settings.SettingsViewModel
import cl.clinipets.ui.theme.ClinipetsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsViewModel: SettingsViewModel by viewModels()
        
        setContent {
            val isDarkPref by settingsViewModel.isDarkMode.collectAsState()
            val isDark = isDarkPref ?: isSystemInDarkTheme()
            
            ClinipetsTheme(darkTheme = isDark) { 
                ClinipetsApp() 
            }
        }
    }
}