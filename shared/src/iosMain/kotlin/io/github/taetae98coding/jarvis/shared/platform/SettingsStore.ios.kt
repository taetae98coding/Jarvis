package io.github.taetae98coding.jarvis.shared.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDefaultsDidChangeNotification

@Composable
internal actual fun rememberSettingsStore(): SettingsStore = UserDefaultsSettingsStore

private object UserDefaultsSettingsStore : SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        // boolForKey 는 키가 없을 때도 false 를 반환하므로, 기본값은 따로 확인해야 한다.
        if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }

    override val changes: Flow<Unit> = callbackFlow {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            name = NSUserDefaultsDidChangeNotification,
            `object` = defaults,
            queue = null,
        ) { trySend(Unit) }
        awaitClose { center.removeObserver(observer) }
    }
}
