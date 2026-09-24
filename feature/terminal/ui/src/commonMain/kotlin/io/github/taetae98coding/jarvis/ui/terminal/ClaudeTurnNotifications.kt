package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taetae98coding.jarvis.domain.terminal.NotifyClaudeTurnEndsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Claude 탭의 턴이 끝나면 운영체제 알림을 보낸다. 앱 루트가 한 번 부른다 — 창을 최소화하거나 다른 화면에 있어도
 * 알려야 해서 터미널 화면이 아니라 앱 루트의 수명을 쓴다(docs/common/claude-notification.html#implementation).
 */
@Composable
fun ClaudeTurnNotifications() {
    koinViewModel<ClaudeNotificationEffectViewModel>()
}

internal class ClaudeNotificationEffectViewModel(
    notifyClaudeTurnEnds: NotifyClaudeTurnEndsUseCase,
    attention: ClaudeAttention,
) : ViewModel() {
    init {
        viewModelScope.launch { notifyClaudeTurnEnds(attention::isWatching) }
    }
}

/** 사용자가 지금 보고 있는 Claude 탭의 sessionId. 터미널 화면이 채우고, 화면이 없으면 비어 있다. */
internal class ClaudeAttention {
    private val watched = MutableStateFlow(emptySet<String>())

    fun watch(sessionIds: Set<String>) {
        watched.value = sessionIds
    }

    fun isWatching(sessionId: String): Boolean = sessionId in watched.value
}
