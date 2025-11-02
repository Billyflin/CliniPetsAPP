package cl.clinipets

import android.app.Application
import cl.clinipets.core.session.SessionManager
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class ClinipetsApplication : Application() {
    @Inject
    lateinit var session: SessionManager
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.Default).launch {
            session.restoreIfAny()
        }
    }

}
