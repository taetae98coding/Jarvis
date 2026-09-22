package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import kotlinx.browser.localStorage

@Composable
internal actual fun rememberSettingsStore(): SettingsStore = LocalStorageSettingsStore

private object LocalStorageSettingsStore : SettingsStore {
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        localStorage.getItem(key.namespaced())?.toBooleanStrictOrNull() ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        localStorage.setItem(key.namespaced(), value.toString())
    }

    // localStorage 는 같은 오리진의 모든 페이지가 공유한다. 다른 타깃이 쓰는 앱별 저장소와 달라서,
    // 그 저장소들이 기본으로 갖는 네임스페이스를 키에 직접 붙인다.
    private fun String.namespaced(): String = "jarvis.settings.$this"
}
