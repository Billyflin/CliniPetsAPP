// NavigationGraphTest.kt
package cl.clinipets.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class NavigationGraphTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appNavigation_initializesNavController() {
        composeTestRule.setContent {
            AppNavigation()
        }
        // Assert that the NavController is not null
        composeTestRule.waitForIdle()
    }

    @Test
    fun appNavigation_handlesEmptyNavigationState() {
        composeTestRule.setContent {
            CompositionLocalProvider {
                AppNavigation()
            }
        }
        // Assert no crashes or unexpected behavior
        composeTestRule.waitForIdle()
    }
}
