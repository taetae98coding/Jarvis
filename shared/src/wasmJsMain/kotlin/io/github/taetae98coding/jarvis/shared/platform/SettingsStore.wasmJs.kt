package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import org.w3c.dom.events.Event

@Composable
internal actual fun rememberSettingsStore(): SettingsStore = LocalStorageSettingsStore

private object LocalStorageSettingsStore : SettingsStore {
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        localStorage.getItem(key.namespaced())?.toBooleanStrictOrNull() ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        localStorage.setItem(key.namespaced(), value.toString())
        localChanges.tryEmit(Unit)
    }

    // `storage` 이벤트는 같은 오리진의 다른 문서(탭)가 쓴 변경에만 발생한다. 이 문서 자신이 쓴 변경은
    // 이벤트가 없어서 putBoolean 에서 직접 신호를 낸다.
    private val localChanges = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val remoteChanges: Flow<Unit> = callbackFlow {
        val listener: (Event) -> Unit = { trySend(Unit) }
        window.addEventListener("storage", listener)
        awaitClose { window.removeEventListener("storage", listener) }
    }

    override val changes: Flow<Unit> = merge(localChanges, remoteChanges)

    // localStorage 는 같은 오리진의 모든 페이지가 공유한다. 다른 타깃이 쓰는 앱별 저장소와 달라서,
    // 그 저장소들이 기본으로 갖는 네임스페이스를 키에 직접 붙인다.
    private fun String.namespaced(): String = "jarvis.settings.$this"
}
