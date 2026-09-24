package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.PlatformContext

// 터미널 자체가 불가라 화면에 들어갈 수 없다(docs/platform/ios.html#terminal-side-bar).
internal actual fun createFileDataSource(context: PlatformContext): FileDataSource = UnsupportedFileDataSource
