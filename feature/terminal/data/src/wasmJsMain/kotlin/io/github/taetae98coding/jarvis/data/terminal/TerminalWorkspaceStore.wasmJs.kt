package io.github.taetae98coding.jarvis.data.terminal

import androidx.datastore.core.DataStore
import io.github.taetae98coding.jarvis.data.PlatformContext

// 터미널을 띄울 수 없어 저장할 배치가 생기지 않는다(docs/common/terminal-panels.html#platforms).
internal actual fun createTerminalWorkspaceStore(context: PlatformContext): DataStore<TerminalWorkspaceDto>? = null
