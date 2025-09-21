// app/src/androidTest/java/cl/clinipets/data/preferences/UserPreferencesAndroidTest.kt
package cl.clinipets.data.preferences

import androidx.test.platform.app.InstrumentationRegistry
import cl.clinipets.ui.theme.Contrast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserPreferencesAndroidTest {

    private fun prefs(): UserPreferences {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        return UserPreferences(ctx)
    }

    @BeforeTest
    fun reset() = runBlocking {
        val p = prefs()
        p.setDarkMode(false)
        p.setDynamicColor(true)
        p.setContrast(Contrast.Standard)
        p.setOnboardingCompleted(false)
        p.clearUserData()
    }

    @Test
    fun defaults_whenUnset() = runBlocking {
        val p = prefs()
        assertEquals(false, p.isDarkMode.first())
        assertEquals(true, p.isDynamicColor.first())
        assertEquals(Contrast.Standard, p.contrast.first())
        assertEquals(false, p.hasCompletedOnboarding.first())
        assertNull(p.userId.first())
        assertNull(p.userEmail.first())
        assertNull(p.userDisplayName.first())
        assertNull(p.userPhotoUrl.first())
    }

    @Test
    fun darkMode_changesArePersisted() = runBlocking {
        val p = prefs()
        p.setDarkMode(true)
        assertEquals(true, p.isDarkMode.first())
        p.setDarkMode(false)
        assertEquals(false, p.isDarkMode.first())
    }

    @Test
    fun dynamicColor_changesArePersisted() = runBlocking {
        val p = prefs()
        p.setDynamicColor(false)
        assertEquals(false, p.isDynamicColor.first())
        p.setDynamicColor(true)
        assertEquals(true, p.isDynamicColor.first())
    }

    @Test
    fun contrast_changesArePersisted() = runBlocking {
        val p = prefs()
        p.setContrast(Contrast.Medium)
        assertEquals(Contrast.Medium, p.contrast.first())
        p.setContrast(Contrast.High)
        assertEquals(Contrast.High, p.contrast.first())
        p.setContrast(Contrast.Standard)
        assertEquals(Contrast.Standard, p.contrast.first())
    }

    @Test
    fun onboardingFlag_changesArePersisted() = runBlocking {
        val p = prefs()
        p.setOnboardingCompleted(true)
        assertEquals(true, p.hasCompletedOnboarding.first())
        p.setOnboardingCompleted(false)
        assertEquals(false, p.hasCompletedOnboarding.first())
    }

    @Test
    fun userData_updateAndClear_emitsExpectedValues() = runBlocking {
        val p = prefs()
        p.updateUserData(
            userId = "u1",
            email = "a@b.c",
            displayName = "Alice",
            photoUrl = "http://x/y.png"
        )
        assertEquals("u1", p.userId.first())
        assertEquals("a@b.c", p.userEmail.first())
        assertEquals("Alice", p.userDisplayName.first())
        assertEquals("http://x/y.png", p.userPhotoUrl.first())

        p.clearUserData()
        assertNull(p.userId.first())
        assertNull(p.userEmail.first())
        assertNull(p.userDisplayName.first())
        assertNull(p.userPhotoUrl.first())
    }
}
