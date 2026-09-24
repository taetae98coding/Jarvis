package io.github.taetae98coding.jarvis.data.terminal

// Claude Code CLI 가 Android 용으로 없어 Claude 탭 자체가 불가다(docs/platform/android.html#terminal-claude-status).
internal actual fun createClaudeActivityDataSource(): ClaudeActivityDataSource = UnsupportedClaudeActivityDataSource
