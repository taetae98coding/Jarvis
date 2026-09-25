package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.CodeIntelRepository

// 터미널 자체가 불가라 화면에 들어갈 수 없다(docs/platform/ios.html#terminal-code-navigation).
internal actual fun createCodeIntelRepository(): CodeIntelRepository = UnsupportedCodeIntelRepository
