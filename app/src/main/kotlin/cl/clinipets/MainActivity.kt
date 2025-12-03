package cl.clinipets

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cl.clinipets.ui.ClinipetsApp
import cl.clinipets.ui.theme.ClinipetsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClinipetsTheme { ClinipetsApp() }
        }
        handleDeepLinkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            Log.d("PaymentDeepLink", "URI recibida: $uri")

            if (uri?.scheme == "clinipets") {
                val status = uri.getQueryParameter("status")
                val message = "Estado del pago: ${status ?: "desconocido"}"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
