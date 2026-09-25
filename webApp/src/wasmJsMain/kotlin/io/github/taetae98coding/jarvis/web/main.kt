package io.github.taetae98coding.jarvis.web

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.ComposeViewport
import io.github.taetae98coding.jarvis.shared.App
import io.github.taetae98coding.jarvis.shared.startJarvisKoin
import jarvis.webapp.generated.resources.Res
import jarvis.webapp.generated.resources.noto_sans_kr_regular
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    startJarvisKoin()

    ComposeViewport(document.body!!) {
        // Skiko wasm 은 브라우저 폰트를 쓰지 못하고 기본 폰트에 한글이 없다. preload 한 폰트가 Skia 의 대체 폰트가 되어
        // 한글을 채운다. 올라오기 전에 그리면 □ 가 보였다가 바뀌므로 그때까지 App() 을 그리지 않는다
        // (docs/platform/web.html#release-build).
        val hangul by preloadFont(Res.font.noto_sans_kr_regular)
        val fontFamilyResolver = LocalFontFamilyResolver.current
        var fontReady by remember { mutableStateOf(false) }

        LaunchedEffect(hangul) {
            hangul?.let { font ->
                fontFamilyResolver.preload(FontFamily(font))
                fontReady = true
            }
        }

        if (fontReady) {
            App()
        }
    }
}
