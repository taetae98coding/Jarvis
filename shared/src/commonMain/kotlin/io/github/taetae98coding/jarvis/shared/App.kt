package io.github.taetae98coding.jarvis.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import io.github.taetae98coding.jarvis.ui.app.JarvisApp

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    val context = rememberPlatformContext()
    val container = remember(context, scope) { JarvisContainer(context, scope) }

    JarvisApp(container.appState)
}
