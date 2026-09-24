package io.github.taetae98coding.jarvis.automation

/**
 * Android·iOS 기기를 조작한다(docs/common/mcp-server.html R13–R18). 좌표는 [screenshot] 이 준 이미지의 픽셀이다.
 * 구현은 기기 도구를 직접 부를 수 있는 타깃만 Koin 에 등록한다.
 */
interface DeviceAutomation {
    suspend fun devices(): List<AutomationDevice>

    /** 꺼져 있으면 켜고 조작할 수 있을 때까지 기다린 뒤 켜진 기기의 식별자를 준다. 꺼진 AVD 는 켜지면 시리얼로 바뀐다. */
    suspend fun boot(deviceId: String): String

    /** 새 에뮬레이터·시뮬레이터를 만들어 꺼진 채로 준다(docs/common/device-lease.html R8). [devices] 에는 다음 번 셀 때 나온다. */
    suspend fun create(platform: AutomationPlatform): AutomationDevice

    suspend fun screenshot(deviceId: String): AutomationImage

    suspend fun tap(deviceId: String, x: Int, y: Int, durationMs: Long)

    suspend fun swipe(deviceId: String, fromX: Int, fromY: Int, toX: Int, toY: Int, durationMs: Long)

    suspend fun type(deviceId: String, text: String)

    suspend fun press(deviceId: String, key: DeviceKey)

    /** 요소마다 한 줄. 좌표는 [screenshot] 과 같은 이미지 픽셀이다. */
    suspend fun uiTree(deviceId: String): String

    suspend fun launchApp(deviceId: String, appId: String)
}

data class AutomationDevice(
    val id: String,
    val name: String,
    val platform: AutomationPlatform,
    val isPhysical: Boolean,
    val isRunning: Boolean,
    val canControl: Boolean,
)

enum class AutomationPlatform { ANDROID, IOS }

enum class DeviceKey(val wireName: String) {
    BACK("back"),
    HOME("home"),
    APP_SWITCH("app_switch"),
    ENTER("enter"),
    DELETE("delete"),
    POWER("power"),
    VOLUME_UP("volume_up"),
    VOLUME_DOWN("volume_down"),
    ;

    companion object {
        fun fromWireName(name: String): DeviceKey? = entries.firstOrNull { it.wireName == name }
    }
}
