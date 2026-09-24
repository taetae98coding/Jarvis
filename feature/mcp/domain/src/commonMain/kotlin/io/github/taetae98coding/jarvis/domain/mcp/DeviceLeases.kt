package io.github.taetae98coding.jarvis.domain.mcp

import io.github.taetae98coding.jarvis.automation.AutomationDevice
import io.github.taetae98coding.jarvis.automation.AutomationPlatform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 기기 할당(docs/common/device-lease.html). 할당 키 → 패널 id 이고 메모리에만 있다(R1, R13).
 * [transaction] 하나 안에서 고르기와 할당을 끝내야 두 패널이 같은 기기를 동시에 잡지 못한다.
 */
internal class DeviceLeases {
    private val owners = mutableMapOf<String, Long>()
    private val mutex = Mutex()

    /** [livePanels] 에 없는 패널의 할당을 먼저 지운 뒤(R12) [block] 을 부른다. */
    suspend fun <T> transaction(livePanels: Collection<Long>, block: (MutableMap<String, Long>) -> T): T =
        mutex.withLock {
            owners.values.removeAll { it !in livePanels }
            block(owners)
        }
}

// R5. 에뮬레이터는 켜지면 시리얼, 꺼지면 `avd:<이름>` 이라 식별자로 가르면 부팅하는 동안 다른 패널이 같은 AVD 를 잡는다.
internal val AutomationDevice.leaseKey: String
    get() = if (platform == AutomationPlatform.ANDROID && !isPhysical) "avd:$name" else id
