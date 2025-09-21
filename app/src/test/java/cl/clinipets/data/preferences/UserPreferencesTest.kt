// Kotlin
package cl.clinipets.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import cl.clinipets.core.data.preferences.UserPreferences
import cl.clinipets.core.ui.theme.Contrast
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserPreferencesTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: UserPreferences
    private lateinit var file: File
    private lateinit var scope: TestScope

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        val tempDir = Files.createTempDirectory("datastore_test").toFile()
        file = File(tempDir, "user_prefs.preferences_pb")
        scope = TestScope(StandardTestDispatcher())
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file }
        )
        val ctor = UserPreferences::class.java.getDeclaredConstructor(DataStore::class.java)
        ctor.isAccessible = true
        prefs = ctor.newInstance(dataStore)
    }

    @AfterTest
    fun tearDown() {
        runCatching { (dataStore as? Closeable)?.close() }
        runCatching { scope.cancel() }
        runCatching { file.delete() }
        runCatching { file.parentFile?.delete() }
    }

    @Test
    fun isDarkMode() = scope.runTest {
        assertFalse(prefs.isDarkMode.first())
        prefs.setDarkMode(true)
        assertTrue(prefs.isDarkMode.first())
    }

    @Test
    fun isDynamicColor() = scope.runTest {
        assertTrue(prefs.isDynamicColor.first())
        prefs.setDynamicColor(false)
        assertFalse(prefs.isDynamicColor.first())
    }

    @Test
    fun setContrast() = scope.runTest {
        prefs.setContrast(Contrast.High)
        assertEquals(Contrast.High, prefs.contrast.first())
    }

    @Test
    fun updateUserData() = scope.runTest {
        prefs.updateUserData("123", "email", "name", "url")
        assertEquals("123", prefs.userId.first())
        assertEquals("email", prefs.userEmail.first())
        assertEquals("name", prefs.userDisplayName.first())
        assertEquals("url", prefs.userPhotoUrl.first())
    }

    @Test
    fun getContrast() = scope.runTest {
        assertEquals(Contrast.Standard, prefs.contrast.first())
        prefs.setContrast(Contrast.Medium)
        assertEquals(Contrast.Medium, prefs.contrast.first())
    }

    @Test
    fun getHasCompletedOnboarding() = scope.runTest {
        assertFalse(prefs.hasCompletedOnboarding.first())
        prefs.setOnboardingCompleted(true)
        assertTrue(prefs.hasCompletedOnboarding.first())
    }

    @Test
    fun getUserId() = scope.runTest {
        assertNull(prefs.userId.first())
        prefs.updateUserData("u1", null, null, null)
        assertEquals("u1", prefs.userId.first())
    }

    @Test
    fun getUserEmail() = scope.runTest {
        assertNull(prefs.userEmail.first())
        prefs.updateUserData(null, "e@x.com", null, null)
        assertEquals("e@x.com", prefs.userEmail.first())
    }

    @Test
    fun getUserDisplayName() = scope.runTest {
        assertNull(prefs.userDisplayName.first())
        prefs.updateUserData(null, null, "Billy", null)
        assertEquals("Billy", prefs.userDisplayName.first())
    }

    @Test
    fun getUserPhotoUrl() = scope.runTest {
        assertNull(prefs.userPhotoUrl.first())
        prefs.updateUserData(null, null, null, "http://photo")
        assertEquals("http://photo", prefs.userPhotoUrl.first())
    }

    @Test
    fun setDarkMode() = scope.runTest {
        prefs.setDarkMode(true)
        assertTrue(prefs.isDarkMode.first())
    }

    @Test
    fun setDynamicColor() = scope.runTest {
        prefs.setDynamicColor(true)
        assertTrue(prefs.isDynamicColor.first(),
            prefs.isDynamicColor.first().toString())
    }

    @Test
    fun setOnboardingCompleted() = scope.runTest {
        prefs.setOnboardingCompleted(true)
        assertTrue(prefs.hasCompletedOnboarding.first())
    }

}
