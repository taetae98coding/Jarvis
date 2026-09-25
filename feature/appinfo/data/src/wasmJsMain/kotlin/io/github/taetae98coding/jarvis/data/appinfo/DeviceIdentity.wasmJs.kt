package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.data.PlatformContext
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// 브라우저는 기기 식별자를 주지 않는다. 처음 열 때 만든 UUID 를 보관해서 같은 브라우저 프로필에서 같은 값을 보여준다.
// localStorage 를 쓸 수 없으면(일부 사생활 보호 모드) 매번 새 값이다.
@OptIn(ExperimentalUuidApi::class)
internal actual fun readDeviceIdentity(context: PlatformContext): DeviceIdentity =
    DeviceIdentity(
        name = window.navigator.platform,
        id = runCatching { localStorage.getItem(DeviceIdKey) }.getOrNull()
            ?: Uuid.random().toString().also { id -> runCatching { localStorage.setItem(DeviceIdKey, id) } },
    )

private const val DeviceIdKey = "jarvis.deviceId"
