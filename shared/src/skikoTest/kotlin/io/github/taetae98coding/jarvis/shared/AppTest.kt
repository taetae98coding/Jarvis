package io.github.taetae98coding.jarvis.shared

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppTest {
    @Test
    fun showsHelloWorld() = runComposeUiTest {
        setContent { App() }

        onNodeWithText("Hello World").assertIsDisplayed()
    }

    @Test
    fun showsCurrentPlatformName() = runComposeUiTest {
        setContent { App() }

        onNodeWithText("Running on $platformName").assertIsDisplayed()
    }
}
