package io.github.taetae98coding.jarvis.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.github.taetae98coding.jarvis.shared.App
import io.github.taetae98coding.jarvis.shared.startJarvisKoin
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startJarvisKoin()

    ComposeViewport(document.body!!) {
        App()
    }
}
