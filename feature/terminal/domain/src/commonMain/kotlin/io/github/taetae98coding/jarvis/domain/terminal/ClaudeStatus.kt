package io.github.taetae98coding.jarvis.domain.terminal

/** Claude Code 백그라운드 세션이 지금 하는 일. Claude Code 가 세션마다 쓰는 기록에서 읽는다. */
sealed interface ClaudeActivity {
    /** 차례를 진행하고 있다. */
    data object Working : ClaudeActivity

    /** 차례를 끝냈지만 걸어 둔 백그라운드 작업(Monitor, 백그라운드 명령, 예약된 깨우기)이 남아 있다. */
    data object Monitoring : ClaudeActivity

    /**
     * 차례를 끝내고 사용자를 기다린다(답을 마침, 질문함, 세션이 멈춤). [at] 은 Claude Code 가 이 상태를
     * 마지막으로 기록한 시각(epoch ms)으로, 사용자가 본 결과인지 가리는 열쇠다.
     */
    data class Finished(val at: Long) : ClaudeActivity
}

/** 패널 줄에 보이는 Claude 상태. 선언 순서가 한 패널에 탭이 여럿일 때의 우선순위다. */
enum class ClaudeStatus {
    AwaitingReply,
    Working,
    Monitoring,
    Checked,
}

fun ClaudeActivity.status(checkedAt: Long?): ClaudeStatus =
    when (this) {
        ClaudeActivity.Working -> ClaudeStatus.Working
        ClaudeActivity.Monitoring -> ClaudeStatus.Monitoring
        is ClaudeActivity.Finished -> if (checkedAt != null && checkedAt >= at) ClaudeStatus.Checked else ClaudeStatus.AwaitingReply
    }

/** 이 패널 Claude 탭들의 상태 중 가장 앞선 것. 상태를 아는 Claude 탭이 없으면 null 이다. 하위 워크트리 패널은 세지 않는다. */
fun TerminalPanel.claudeStatus(activities: Map<String, ClaudeActivity>): ClaudeStatus? =
    tabs.mapNotNull { tab -> tab.claudeSessionId?.let(activities::get)?.status(tab.claudeCheckedAt) }.minOrNull()

/** 모든 패널의 Claude 탭이 붙은 세션. 상태를 조회할 대상이다. */
val TerminalWorkspace.claudeSessionIds: Set<String>
    get() = tabs.mapNotNullTo(mutableSetOf()) { tab -> tab.claudeSessionId.takeIf { tab.program == TerminalProgram.Claude } }
