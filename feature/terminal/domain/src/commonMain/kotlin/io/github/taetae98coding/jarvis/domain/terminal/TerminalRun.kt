package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 실행 메뉴의 사용자 명령 하나(docs/common/terminal-run.html R14–R16). [title] 이 null 이면 [command] 가 이름이다.
 * [id] 는 [TerminalWorkspace.nextId] 에서 나온다.
 */
data class TerminalCommand(
    val id: Long,
    val title: String?,
    val command: String,
) {
    val label: String
        get() = title ?: command
}

/** 마지막으로 실행한 Android 선택(R17). */
data class AndroidRunChoice(
    val modulePath: String,
    val variant: String,
    val deviceId: String,
)

/** 마지막으로 실행한 iOS 선택(R17). */
data class IosRunChoice(
    val scheme: String,
    val configuration: String,
    val deviceId: String,
)

enum class ProjectKind {
    Android,
    IOS,
}

/** Gradle·xcodebuild 에 묻는 동안의 상태. */
sealed interface ProjectLoad<out T> {
    data object Loading : ProjectLoad<Nothing>

    data class Loaded<T>(val value: T) : ProjectLoad<T>

    data class Failed(val message: String? = null) : ProjectLoad<Nothing>
}

data class AndroidVariant(
    val name: String,
    val applicationId: String?,
)

/** `com.android.application` 을 쓰는 모듈. [modulePath] 는 `:androidApp` 같은 Gradle 경로다. */
data class AndroidApp(
    val modulePath: String,
    val variants: List<AndroidVariant>,
)

data class AndroidProject(
    val directory: String,
    val apps: List<AndroidApp>,
)

/** [container] 는 `.xcworkspace` 나 `.xcodeproj` 의 절대 경로다. 워크스페이스면 [isWorkspace]. */
data class IosProject(
    val directory: String,
    val container: String,
    val isWorkspace: Boolean,
    val schemes: List<String>,
    val configurations: List<String>,
)

/**
 * 실행할 기기. [id] 는 emulator 기능의 식별자 모양 그대로다(adb 시리얼, `avd:<이름>`, 시뮬레이터 UDID, `ios:<UDID>`).
 * [canMirror] 면 실행할 때 기기 탭을 함께 연다(R10).
 */
data class RunDevice(
    val id: String,
    val name: String,
    val platform: DevicePlatform,
    val isPhysical: Boolean = false,
    val isRunning: Boolean = true,
    val canMirror: Boolean = true,
)

data class AndroidRunRequest(
    val directory: String,
    val modulePath: String,
    val variant: AndroidVariant,
    val device: RunDevice,
)

data class IosRunRequest(
    val project: IosProject,
    val scheme: String,
    val configuration: String,
    val device: RunDevice,
)

/** 실행할 때 함께 여는 기기 탭(R10). */
data class RunMirror(
    val deviceId: String,
    val deviceName: String,
    val platform: DevicePlatform,
)

internal fun RunDevice.mirror(): RunMirror? = if (canMirror) RunMirror(id, name, platform) else null
