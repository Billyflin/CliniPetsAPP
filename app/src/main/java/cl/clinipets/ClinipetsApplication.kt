package cl.clinipets

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ClinipetsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
