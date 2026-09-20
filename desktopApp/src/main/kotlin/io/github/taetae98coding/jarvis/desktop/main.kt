package io.github.taetae98coding.jarvis.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.taetae98coding.jarvis.shared.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Jarvis",
        state = rememberWindowState(size = DpSize(900.dp, 640.dp)),
    ) {
        App()
    }
}
