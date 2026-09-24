package io.github.taetae98coding.jarvis.data.terminal

// 터미널 자체가 불가라 화면에 들어갈 수 없다(docs/platform/ios.html#terminal-claude-status).
internal actual fun createClaudeActivityDataSource(): ClaudeActivityDataSource = UnsupportedClaudeActivityDataSource
