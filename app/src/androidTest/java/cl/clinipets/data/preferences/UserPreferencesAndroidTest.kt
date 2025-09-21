// app/src/androidTest/java/cl/clinipets/data/preferences/UserPreferencesAndroidTest.kt
package cl.clinipets.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import cl.clinipets.core.data.preferences.UserPreferences
import cl.clinipets.core.ui.theme.Contrast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val DS_NAME = "user_preferences" // fijo, como en producción
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DS_NAME)

class UserPreferencesAndroidTest {

    private lateinit var prefs: UserPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val ctor = UserPreferences::class.java.getDeclaredConstructor(DataStore::class.java)
        ctor.isAccessible = true
        prefs = ctor.newInstance(context.dataStore)
        // limpiar estado antes de cada test
        runBlocking { prefs.clearUserData() }
        runBlocking { prefs.setDarkMode(false); prefs.setDynamicColor(true); prefs.setOnboardingCompleted(false); prefs.setContrast(Contrast.Standard) }
    }

    @After
    fun tearDown() {
        // No se puede cerrar un DataStore creado con preferencesDataStore (singleton).
        // Solo limpia claves para no arrastrar estado entre tests.
        runBlocking {
            prefs.clearUserData()
            prefs.setDarkMode(false)
            prefs.setDynamicColor(true)
            prefs.setOnboardingCompleted(false)
            prefs.setContrast(Contrast.Standard)
        }
    }

    @Test
    fun isDarkMode() = runBlocking {
        assertFalse(prefs.isDarkMode.first())
        prefs.setDarkMode(true)
        assertTrue(prefs.isDarkMode.first())
    }

    @Test
    fun isDynamicColor() = runBlocking {
        assertTrue(prefs.isDynamicColor.first())
        prefs.setDynamicColor(false)
        assertFalse(prefs.isDynamicColor.first())
    }

    @Test
    fun contrast() = runBlocking {
        assertEquals(Contrast.Standard, prefs.contrast.first())
        prefs.setContrast(Contrast.Medium)
        assertEquals(Contrast.Medium, prefs.contrast.first())
    }

    @Test
    fun userData() = runBlocking {
        assertNull(prefs.userId.first())
        prefs.updateUserData("u1","e@x.com","Billy","http://photo")
        assertEquals("u1", prefs.userId.first())
        assertEquals("e@x.com", prefs.userEmail.first())
        assertEquals("Billy", prefs.userDisplayName.first())
        assertEquals("http://photo", prefs.userPhotoUrl.first())
        prefs.clearUserData()
        assertNull(prefs.userId.first())
        assertNull(prefs.userEmail.first())
        assertNull(prefs.userDisplayName.first())
        assertNull(prefs.userPhotoUrl.first())
    }

    @Test
    fun onboarding() = runBlocking {
        assertFalse(prefs.hasCompletedOnboarding.first())
        prefs.setOnboardingCompleted(true)
        assertTrue(prefs.hasCompletedOnboarding.first())
    }
}
