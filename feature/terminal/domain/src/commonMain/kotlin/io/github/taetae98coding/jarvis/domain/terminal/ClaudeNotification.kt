package io.github.taetae98coding.jarvis.domain.terminal

data class ClaudeNotification(
    val title: String,
    val message: String,
)

/** 제목은 마침·입력 대기를 가르고, 본문은 `패널[ · 탭][ — 요약]` 이다(docs/common/claude-notification.html R2). */
fun claudeNotification(end: ClaudeTurnEnd, panel: TerminalPanel, tab: TerminalTab): ClaudeNotification {
    val title = if (end.activity == ClaudeActivity.WaitingForInput) "Claude 입력 대기" else "Claude 작업 완료"
    val place = listOfNotNull(panel.name, tab.name).joinToString(" · ")
    val summary = end.summary?.trim()?.take(ClaudeSummaryMaxLength)?.takeIf { it.isNotEmpty() }

    return ClaudeNotification(title, if (summary != null) "$place — $summary" else place)
}

private const val ClaudeSummaryMaxLength = 200
