package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.WindowInfo
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfo
import io.github.taetae98coding.jarvis.domain.appinfo.AppInfoRepository
import io.github.taetae98coding.jarvis.domain.appinfo.AppRelease
import io.github.taetae98coding.jarvis.domain.appinfo.AppUpdateRepository
import io.github.taetae98coding.jarvis.domain.emulator.DevicePairingRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorFrame
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorGesture
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorRepository
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorSummary
import io.github.taetae98coding.jarvis.domain.emulator.PairingResult
import io.github.taetae98coding.jarvis.domain.emulator.PairingService
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationNotificationRepository
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationNotificationStatus
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationRepository
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeSettingsRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeNotificationRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeNotificationStatus
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import io.github.taetae98coding.jarvis.domain.appinfo.appInfoDomainModule
import io.github.taetae98coding.jarvis.domain.emulator.emulatorDomainModule
import io.github.taetae98coding.jarvis.domain.rotation.rotationDomainModule
import io.github.taetae98coding.jarvis.domain.screen.screenDomainModule
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.ChromeProfile
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivity
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivityRepository
import io.github.taetae98coding.jarvis.domain.terminal.ClaudeNotification
import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.FileEntry
import io.github.taetae98coding.jarvis.domain.terminal.FileRepository
import io.github.taetae98coding.jarvis.domain.terminal.GitChange
import io.github.taetae98coding.jarvis.domain.terminal.GitChangesRepository
import io.github.taetae98coding.jarvis.domain.terminal.GitCommitFile
import io.github.taetae98coding.jarvis.domain.terminal.GitFileDiff
import io.github.taetae98coding.jarvis.domain.terminal.GitGraphLine
import io.github.taetae98coding.jarvis.domain.terminal.GitPushTarget
import io.github.taetae98coding.jarvis.domain.terminal.GitStatus
import io.github.taetae98coding.jarvis.domain.terminal.GitBranch
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktree
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktreeException
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktreeRepository
import io.github.taetae98coding.jarvis.domain.terminal.ProjectRunRepository
import io.github.taetae98coding.jarvis.domain.terminal.TerminalRepository
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSession
import io.github.taetae98coding.jarvis.domain.terminal.TerminalSize
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspaceChange
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspaceRepository
import io.github.taetae98coding.jarvis.domain.terminal.terminalDomainModule
import io.github.taetae98coding.jarvis.ui.appUiModule
import io.github.taetae98coding.jarvis.ui.appinfo.appInfoUiModule
import io.github.taetae98coding.jarvis.ui.emulator.emulatorUiModule
import io.github.taetae98coding.jarvis.ui.rotation.rotationUiModule
import io.github.taetae98coding.jarvis.ui.screen.screenUiModule
import io.github.taetae98coding.jarvis.ui.terminal.terminalUiModule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

// 화면 테스트는 저장소도 플랫폼도 모른다. 도메인 인터페이스만 가짜로 끼우면 되는 것이 :ui 가
// :data 를 보지 않는다는 증거다.
internal val TestAppInfo = AppInfo(version = "1.2.3-test", platform = "Test Platform", deviceName = "Test Device", deviceId = "test-device-id")

/**
 * 가짜 리포지토리만 담은 Koin 으로 앱 전체를 띄운다.
 *
 * 기능들의 진짜 domain·ui Koin 모듈을 그대로 쓰고 data 모듈만 가짜로 바꿔 끼운다. 손으로 조립하지
 * 않으므로 조립 자체도 이 테스트가 검증한다.
 *
 * 프로덕션과 같은 전역 Koin 을 쓴다. `KoinApplication` 컴포저블로 격리하려 했지만, 그 함수는 전역
 * Koin 이 이미 있으면 새 인스턴스를 만들지 않고 전역 것을 그대로 쓴다(`rememberKoinApplication`).
 * 그러면 먼저 실행된 테스트의 가짜 저장소가 남아 다음 테스트로 새어 든다. 대신 테스트마다 전역
 * Koin 을 세우고, 이전 테스트가 남긴 것이 있으면 먼저 치운다.
 */
@Composable
internal fun TestJarvisApp(
    settings: ScreenAwakeSettingsRepository = FakeScreenAwakeSettingsRepository(),
    systemScreenAwake: SystemScreenAwakeRepository = FakeSystemScreenAwakeRepository(),
    systemScreenAwakeNotification: SystemScreenAwakeNotificationRepository = FakeSystemScreenAwakeNotificationRepository(),
    emulator: EmulatorRepository = FakeEmulatorRepository(),
    pairing: DevicePairingRepository = FakeDevicePairingRepository(),
    deviceRotation: DeviceRotationRepository = FakeDeviceRotationRepository(),
    deviceRotationNotification: DeviceRotationNotificationRepository = FakeDeviceRotationNotificationRepository(),
    screenAwake: ScreenAwakeRepository = ScreenAwakeRepository { },
    terminal: TerminalRepository = FakeTerminalRepository(),
    terminalWorkspace: TerminalWorkspaceRepository = FakeTerminalWorkspaceRepository(),
    gitWorktree: GitWorktreeRepository = FakeGitWorktreeRepository(),
    claudeActivity: ClaudeActivityRepository = FakeClaudeActivityRepository(),
    files: FileRepository = FakeFileRepository(),
    gitChanges: GitChangesRepository = FakeGitChangesRepository(),
    projectRun: ProjectRunRepository = FakeProjectRunRepository(),
    // null 이면 테스트 창의 포커스를 그대로 쓴다.
    windowFocused: State<Boolean>? = null,
    appInfo: AppInfo = TestAppInfo,
    appUpdate: AppUpdateRepository = FakeAppUpdateRepository(),
    // 기본 핸들러(DesktopUriHandler)는 테스트 중에 실제 브라우저를 띄운다. 늘 기록만 하는 것으로 바꾼다.
    uriHandler: RecordingUriHandler = RecordingUriHandler(),
) {
    remember {
        installTestMainDispatcher()

        val fakes = module {
            single<AppInfoRepository> { AppInfoRepository { appInfo } }
            single<AppUpdateRepository> { appUpdate }
            single<EmulatorRepository> { emulator }
            single<DevicePairingRepository> { pairing }
            single<ScreenAwakeSettingsRepository> { settings }
            single<ScreenAwakeRepository> { screenAwake }
            single<SystemScreenAwakeRepository> { systemScreenAwake }
            single<SystemScreenAwakeNotificationRepository> { systemScreenAwakeNotification }
            single<DeviceRotationRepository> { deviceRotation }
            single<DeviceRotationNotificationRepository> { deviceRotationNotification }
            single<TerminalRepository> { terminal }
            single<TerminalWorkspaceRepository> { terminalWorkspace }
            single<GitWorktreeRepository> { gitWorktree }
            single<ClaudeActivityRepository> { claudeActivity }
            single<FileRepository> { files }
            single<GitChangesRepository> { gitChanges }
            single<ProjectRunRepository> { projectRun }
        }

        if (KoinPlatformTools.defaultContext().getOrNull() != null) {
            stopKoin()
        }

        startKoin {
            modules(
                fakes,
                appInfoDomainModule, appInfoUiModule,
                emulatorDomainModule, emulatorUiModule,
                screenDomainModule, screenUiModule,
                rotationDomainModule, rotationUiModule,
                terminalDomainModule, terminalUiModule,
                appUiModule,
            )
        }
    }

    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        if (windowFocused == null) {
            JarvisApp()
        } else {
            val window = LocalWindowInfo.current
            val info = remember(window) {
                object : WindowInfo by window {
                    override val isWindowFocused: Boolean get() = windowFocused.value
                }
            }
            CompositionLocalProvider(LocalWindowInfo provides info) { JarvisApp() }
        }
    }
}

/**
 * 기본값은 새 버전이 없는 것이다 — 스스로 업데이트하지 못하는 타깃과 같다. 설치는 요청을 기록하고 [failure] 가 있으면
 * 그것으로 실패한다. [gate] 가 있으면 요청을 기록한 뒤 그것이 끝날 때까지 기다린다.
 */
internal class FakeAppUpdateRepository(
    release: AppRelease? = null,
    var failure: String? = null,
) : AppUpdateRepository {
    val release = MutableStateFlow(release)

    val installed = mutableListOf<AppRelease>()

    var gate: CompletableDeferred<Unit>? = null

    override fun observeAvailableUpdate(): Flow<AppRelease?> = release

    override suspend fun install(release: AppRelease): Result<Unit> {
        installed += release
        gate?.await()
        return failure?.let { Result.failure(IllegalStateException(it)) } ?: Result.success(Unit)
    }
}

/** 시스템 브라우저로 열라고 한 주소를 기록한다(docs/common/terminal-link.html R5). */
internal class RecordingUriHandler : UriHandler {
    val opened = mutableListOf<String>()

    override fun openUri(uri: String) {
        opened += uri
    }
}

internal class FakeScreenAwakeSettingsRepository(
    keepScreenAwake: Boolean = false,
    keepSystemScreenAwake: Boolean = false,
) : ScreenAwakeSettingsRepository {
    val keepScreenAwake = MutableStateFlow(keepScreenAwake)
    val keepSystemScreenAwake = MutableStateFlow(keepSystemScreenAwake)

    override fun observeKeepScreenAwake() = keepScreenAwake

    override fun readKeepScreenAwake() = keepScreenAwake.value

    override fun observeKeepSystemScreenAwake() = keepSystemScreenAwake

    override fun readKeepSystemScreenAwake() = keepSystemScreenAwake.value

    override fun setKeepScreenAwake(value: Boolean) {
        this.keepScreenAwake.value = value
    }

    override fun setKeepSystemScreenAwake(value: Boolean) {
        this.keepSystemScreenAwake.value = value
    }
}

// 기본값은 Skiko 로 렌더링하는 세 타깃의 실제 상태와 같다. 셋 다 시스템 전역 화면 유지를 지원하지 않는다.
internal class FakeSystemScreenAwakeRepository(
    initial: SystemScreenAwakeStatus = SystemScreenAwakeStatus(),
) : SystemScreenAwakeRepository {
    val status = MutableStateFlow(initial)

    override fun observeStatus() = status

    override fun readStatus() = status.value

    override fun setEnabled(enabled: Boolean) = Unit

    override fun requestPermission() = Unit
}

// 기본값은 "셀 수 없음" 과 빈 목록이다. 개수를 세지 못하는 타깃이 답하는 값과 같다.
internal class FakeEmulatorRepository(
    android: EmulatorSummary? = null,
    ios: EmulatorSummary? = null,
    devices: List<EmulatorDevice> = emptyList(),
    private val frames: Flow<EmulatorFrame?> = emptyFlow(),
) : EmulatorRepository {
    val status = MutableStateFlow(EmulatorStatus(android = android, ios = ios))

    val devices = MutableStateFlow(devices)

    val gestures = mutableListOf<Pair<String, EmulatorGesture>>()

    val launched = mutableListOf<String>()

    val woken = mutableListOf<String>()

    override fun observeStatus() = status

    override fun observeDevices() = devices

    override fun observeScreen(deviceId: String) = frames

    override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) {
        gestures += deviceId to gesture
    }

    override suspend fun launch(deviceId: String) {
        launched += deviceId
    }

    override suspend fun wake(deviceId: String) {
        woken += deviceId
    }
}

// 아직 아무 답도 하지 않은 저장소. 화면은 "확인 중…" 과 빈 목록을 보여줘야 한다.
internal object SilentEmulatorRepository : EmulatorRepository {
    override fun observeStatus() = emptyFlow<EmulatorStatus>()

    override fun observeDevices() = emptyFlow<List<EmulatorDevice>>()

    override fun observeScreen(deviceId: String) = emptyFlow<EmulatorFrame?>()

    override suspend fun sendGesture(deviceId: String, gesture: EmulatorGesture) = Unit

    override suspend fun launch(deviceId: String) = Unit

    override suspend fun wake(deviceId: String) = Unit
}

// 기본값은 페어링을 기다리는 기기가 없는 개발자 머신이다.
internal class FakeDevicePairingRepository(
    services: List<PairingService>? = emptyList(),
    private val result: PairingResult = PairingResult.Paired(isConnected = true),
) : DevicePairingRepository {
    val services = MutableStateFlow(services)

    val paired = mutableListOf<Pair<PairingService, String>>()

    override fun observePairingServices() = services

    override suspend fun pair(service: PairingService, code: String): PairingResult {
        paired += service to code
        return result
    }
}

// 기본값은 Skiko 로 렌더링하는 세 타깃 중 JVM 의 실제 상태와 같다. 돌릴 화면이 없다.
internal class FakeDeviceRotationRepository(
    initial: DeviceRotationStatus = DeviceRotationStatus(),
) : DeviceRotationRepository {
    val status = MutableStateFlow(initial)

    override fun observeStatus() = status

    override fun readStatus() = status.value

    override fun setAngle(angle: RotationAngle) {
        status.value = status.value.copy(angle = angle)
    }

    override fun setLocked(locked: Boolean) {
        status.value = status.value.copy(locked = locked)
    }

    override fun requestPermission() = Unit
}

// 기본값은 Skiko 로 렌더링하는 세 타깃의 실제 상태와 같다. 셋 다 알림 컨트롤을 만들 수 없다.
internal class FakeDeviceRotationNotificationRepository(
    initial: DeviceRotationNotificationStatus = DeviceRotationNotificationStatus(),
) : DeviceRotationNotificationRepository {
    val status = MutableStateFlow(initial)

    override fun observeStatus() = status

    override fun readStatus() = status.value

    override fun setPinned(pinned: Boolean) {
        status.value = status.value.copy(pinned = pinned)
    }

    override fun requestPermission() = Unit
}

internal class FakeSystemScreenAwakeNotificationRepository(
    initial: SystemScreenAwakeNotificationStatus = SystemScreenAwakeNotificationStatus(),
) : SystemScreenAwakeNotificationRepository {
    val status = MutableStateFlow(initial)

    override fun observeStatus() = status

    override fun readStatus() = status.value

    override fun setPinned(pinned: Boolean) {
        status.value = status.value.copy(pinned = pinned)
    }

    override fun requestPermission() = Unit
}

// 셸 대신 테스트가 출력을 흘려 넣고 종료를 정한다. 기본값은 JVM·Android 처럼 셸을 띄울 수 있는 타깃이다.
internal class FakeTerminalRepository(
    override val isSupported: Boolean = true,
    override val isClaudeSupported: Boolean = true,
    // 켜면 브라우저 탭이 네이티브 웹뷰를 띄우려 한다. 테스트 화면에는 붙지 않으니 메뉴를 보는 테스트만 켠다.
    override val isBrowserSupported: Boolean = false,
    override val isChromeImportSupported: Boolean = false,
    private val chromeProfiles: List<ChromeProfile> = emptyList(),
) : TerminalRepository {
    val sessions = mutableListOf<FakeTerminalSession>()

    val stoppedClaudeSessions = mutableListOf<String>()

    val notifications = mutableListOf<ClaudeNotification>()

    override suspend fun open(size: TerminalSize, tab: TerminalTab): TerminalSession =
        FakeTerminalSession(size, tab).also { sessions += it }

    override suspend fun stopClaude(sessionId: String) {
        stoppedClaudeSessions += sessionId
    }

    override suspend fun showNotification(notification: ClaudeNotification) {
        notifications += notification
    }

    override fun observeChromeProfiles(): Flow<List<ChromeProfile>> = flowOf(chromeProfiles)

    override suspend fun importChromeCookies(profileDirectory: String): List<BrowserCookie> = emptyList()
}

/**
 * 파일 대신 메모리에 둔다. 같은 인스턴스를 다음 [TestJarvisApp] 에 넘기면 앱을 다시 켠 것과 같다 —
 * 세션은 새로 열리고 배치는 남는다.
 */
internal class FakeTerminalWorkspaceRepository(
    initial: TerminalWorkspace = TerminalWorkspace.initial(),
) : TerminalWorkspaceRepository {
    val workspace = MutableStateFlow(initial)

    override fun observeWorkspace(): Flow<TerminalWorkspace> = workspace

    override suspend fun updateWorkspace(transform: (TerminalWorkspace) -> TerminalWorkspace): TerminalWorkspaceChange {
        val before = workspace.value
        val after = transform(before)
        workspace.value = after

        return TerminalWorkspaceChange(before, after)
    }
}

/**
 * 폴더마다 정해 둔 워크트리를 답한다. 기본값은 어느 폴더도 저장소가 아닌 것이다. 만들기는 요청을 기록하고
 * 새 경로도 그 브랜치를 체크아웃한 저장소로 등록해서, 진짜 git 처럼 워크트리 패널에도 + 가 붙고 현재 브랜치가 보인다.
 * 지우기는 요청을 기록하고 그 경로를 저장소가 아닌 것으로 되돌린다. [failure] 가 있으면 둘 다 그것으로 실패한다.
 * [gate] 가 있으면 둘 다 요청을 기록한 뒤 그것이 끝날 때까지 기다린다 — 뒤에서 도는 동안의 화면을 볼 수 있게.
 */
internal class FakeGitWorktreeRepository(
    worktrees: Map<String, GitWorktree> = emptyMap(),
    var failure: String? = null,
) : GitWorktreeRepository {
    var gate: CompletableDeferred<Unit>? = null

    class Added(val repositoryDirectory: String, val branch: String, val path: String, val baseBranch: String?)

    val worktrees = MutableStateFlow(worktrees)

    /** main 워크트리 경로마다 기준 브랜치 후보. */
    val branches = MutableStateFlow<Map<String, List<GitBranch>>>(emptyMap())

    class Removed(val directory: String, val deleteDirectory: Boolean)

    val added = mutableListOf<Added>()

    val removed = mutableListOf<Removed>()

    override fun observeWorktree(directory: String): Flow<GitWorktree?> = worktrees.map { it[directory] }

    override fun observeBranches(directory: String): Flow<List<GitBranch>> = branches.map { it[directory].orEmpty() }

    override suspend fun addWorktree(repositoryDirectory: String, branch: String, path: String, baseBranch: String?): Result<GitWorktree> {
        added += Added(repositoryDirectory, branch, path, baseBranch)
        gate?.await()
        failure?.let { return Result.failure(GitWorktreeException(it)) }

        val worktree = GitWorktree(
            path = path,
            mainPath = worktrees.value[repositoryDirectory]?.mainPath ?: repositoryDirectory,
            branch = branch,
        )
        worktrees.update { it + (path to worktree) }

        return Result.success(worktree)
    }

    override suspend fun removeWorktree(directory: String, deleteDirectory: Boolean): Result<Unit> {
        removed += Removed(directory, deleteDirectory)
        gate?.await()
        failure?.let { return Result.failure(GitWorktreeException(it)) }

        worktrees.update { it - directory }
        return Result.success(Unit)
    }
}

/** 폴더·파일마다 정해 둔 값을 답한다. 기본값은 어느 폴더도 읽을 수 없는 것이다. 값을 바꾸면 디스크가 바뀐 것처럼 따라간다. */
internal class FakeFileRepository(
    directories: Map<String, List<FileEntry>> = emptyMap(),
    files: Map<String, FileContent> = emptyMap(),
) : FileRepository {
    val directories = MutableStateFlow(directories)

    val files = MutableStateFlow(files)

    override fun observeDirectory(directory: String): Flow<List<FileEntry>?> = directories.map { it[directory] }

    override fun observeFile(path: String): Flow<FileContent> = files.map { it[path] ?: FileContent.Unreadable }
}

/**
 * 폴더마다 정해 둔 git 상태와 그래프, 파일마다 정해 둔 diff 를 답한다. 기본값은 어느 폴더도 저장소가 아닌 것이다. stage·unstage·push 는 요청을 기록하고
 * [failure] 가 있으면 그것으로 실패한다. 상태는 바꾸지 않는다 — 결과는 테스트가 [statuses] 로 정한다.
 * [gate] 가 있으면 push 는 요청을 기록한 뒤 그것이 끝날 때까지 기다린다.
 */
internal class FakeGitChangesRepository(
    statuses: Map<String, GitStatus> = emptyMap(),
    graphs: Map<String, List<GitGraphLine>> = emptyMap(),
    diffs: Map<String, GitFileDiff> = emptyMap(),
    commitFiles: Map<String, List<GitChange>> = emptyMap(),
    commitFileContents: Map<Pair<String, String>, GitCommitFile> = emptyMap(),
    var failure: String? = null,
) : GitChangesRepository {
    /** (절대 경로, 해시)마다 커밋 시점의 파일. 없으면 읽을 수 없는 커밋이다. */
    val commitFileContents = MutableStateFlow(commitFileContents)

    val statuses = MutableStateFlow(statuses)

    val graphs = MutableStateFlow(graphs)

    val diffs = MutableStateFlow(diffs)

    /** 해시마다 커밋의 파일. 없는 해시는 읽을 수 없는 커밋이다. */
    val commitFiles = MutableStateFlow(commitFiles)

    val staged = mutableListOf<Pair<String, List<GitChange>>>()

    val unstaged = mutableListOf<Pair<String, List<GitChange>>>()

    val pushed = mutableListOf<Pair<String, GitPushTarget>>()

    var gate: CompletableDeferred<Unit>? = null

    override fun observeStatus(directory: String): Flow<GitStatus?> = statuses.map { it[directory] }

    override fun observeGraph(directory: String): Flow<List<GitGraphLine>> = graphs.map { it[directory].orEmpty() }

    override fun observeCommitFiles(directory: String, hash: String): Flow<List<GitChange>?> = commitFiles.map { it[hash] }

    override fun observeCommitFile(path: String, hash: String): Flow<GitCommitFile?> = commitFileContents.map { it[path to hash] }

    override fun observeFileDiff(path: String): Flow<GitFileDiff?> = diffs.map { it[path] }

    override suspend fun stage(root: String, changes: List<GitChange>): Result<Unit> {
        staged += root to changes
        return failure?.let { Result.failure(GitWorktreeException(it)) } ?: Result.success(Unit)
    }

    override suspend fun unstage(root: String, changes: List<GitChange>): Result<Unit> {
        unstaged += root to changes
        return failure?.let { Result.failure(GitWorktreeException(it)) } ?: Result.success(Unit)
    }

    override suspend fun push(root: String, target: GitPushTarget): Result<Unit> {
        pushed += root to target
        gate?.await()
        return failure?.let { Result.failure(GitWorktreeException(it)) } ?: Result.success(Unit)
    }
}

/** 창 sessionId 마다 테스트가 정한 활동을 답한다. 기본값은 어떤 세션도 찾지 못한 것이다. */
internal class FakeClaudeActivityRepository(
    activities: Map<String, ClaudeActivity> = emptyMap(),
) : ClaudeActivityRepository {
    val activities = MutableStateFlow(activities)

    override fun observeActivities(sessionIds: Set<String>): Flow<Map<String, ClaudeActivity>> =
        this.activities.map { all -> all.filterKeys { it in sessionIds } }
}

internal class FakeTerminalSession(
    var size: TerminalSize,
    val tab: TerminalTab,
) : TerminalSession {
    val program: TerminalProgram get() = tab.program

    private val currentDirectory = MutableStateFlow<String?>(null)

    override val directory: Flow<String> = currentDirectory.filterNotNull()

    /** 셸이 `cd` 한 것처럼 작업 디렉터리를 알린다. */
    fun changeDirectory(path: String) {
        currentDirectory.value = path
    }

    private val channel = Channel<ByteArray>(Channel.UNLIMITED)

    val written = mutableListOf<ByteArray>()

    var closed = false
        private set

    override val isPty: Boolean = true

    override val output: Flow<ByteArray> = channel.consumeAsFlow()

    fun emit(text: String) {
        channel.trySend(text.encodeToByteArray())
    }

    /** 셸이 스스로 끝난 것처럼 출력을 닫는다. */
    fun exit() {
        channel.close()
    }

    override suspend fun write(bytes: ByteArray) {
        written += bytes
    }

    override fun resize(size: TerminalSize) {
        this.size = size
    }

    override fun close() {
        closed = true
        channel.close()
    }
}
