package cl.clinipets

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import cl.clinipets.navigation.AppNavigation
import cl.clinipets.ui.theme.ClinipetsTheme
import cl.clinipets.utils.DataInitializer
import com.facebook.CallbackManager
import com.facebook.FacebookSdk
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FacebookSdk.sdkInitialize(applicationContext)
        callbackManager = CallbackManager.Factory.create()

        lifecycleScope.launch {
            try {
                DataInitializer.initializeBasicServices()
                DataInitializer.initializeCommonMedications()
                DataInitializer.initializeCommonVaccines()
            } catch (e: Exception) {
                Log.e("DataInitializer", "Error al inicializar datos: ${e.message}")
            }
        }
        setContent {
            ClinipetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}