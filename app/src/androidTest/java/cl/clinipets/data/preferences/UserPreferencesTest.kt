// Kotlin
package cl.clinipets.data.preferences

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.clinipets.ui.theme.Contrast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferencesAndroidTest {

    private fun prefs(): UserPreferences {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        return UserPreferences(ctx)
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
