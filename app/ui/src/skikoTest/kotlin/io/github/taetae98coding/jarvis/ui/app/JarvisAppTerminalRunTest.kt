package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.taetae98coding.jarvis.domain.terminal.AndroidApp
import io.github.taetae98coding.jarvis.domain.terminal.AndroidProject
import io.github.taetae98coding.jarvis.domain.terminal.AndroidRunChoice
import io.github.taetae98coding.jarvis.domain.terminal.AndroidRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.AndroidVariant
import io.github.taetae98coding.jarvis.domain.terminal.DevicePlatform
import io.github.taetae98coding.jarvis.domain.terminal.IosProject
import io.github.taetae98coding.jarvis.domain.terminal.IosRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.PaneNode
import io.github.taetae98coding.jarvis.domain.terminal.ProjectKind
import io.github.taetae98coding.jarvis.domain.terminal.ProjectLoad
import io.github.taetae98coding.jarvis.domain.terminal.ProjectRunRepository
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import io.github.taetae98coding.jarvis.ui.terminal.TerminalCommandDialogCommandTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalCommandDialogConfirmTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalCommandDialogTitleTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalRunDialogConfirmTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalRunDialogDeviceTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalRunDialogErrorTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalRunDialogLoadingTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalRunDialogRetryTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalRunDialogVariantTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalRunMenuAddCommandTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalRunMenuAndroidTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalRunMenuIosTestTag
import io.github.taetae98coding.jarvis.ui.terminal.TerminalTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalRunCommandDeleteTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalRunCommandEditTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalRunCommandTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalRunDialogOptionTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalRunTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabKindTestTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** docs/common/terminal-run.html */
@OptIn(ExperimentalTestApi::class)
class JarvisAppTerminalRunTest {
    // 폴더가 /repo 인 패널 하나.
    private fun repoWorkspace() = FakeTerminalWorkspaceRepository(
        TerminalWorkspace.initial().let { TerminalWorkspace(it.panels.map { panel -> panel.copy(directory = "/repo") }, it.selectedPanelId, it.nextId) },
    )

    private fun ComposeUiTest.openTerminal(
        workspace: FakeTerminalWorkspaceRepository,
        projectRun: FakeProjectRunRepository = FakeProjectRunRepository(),
        emulator: FakeEmulatorRepository = FakeEmulatorRepository(devices = listOf(RunningAndroidDevice, StoppedAndroidDevice, RunningSimulator, PhysicalIosDevice)),
        terminal: FakeTerminalRepository = FakeTerminalRepository(),
    ) {
        setContent { TestJarvisApp(emulator = emulator, terminal = terminal, terminalWorkspace = workspace, projectRun = projectRun) }
        onNodeWithTag(TerminalTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { terminal.sessions.size == 1 }
    }

    // 처음 켠 작업 공간의 그룹 id 다(패널 1, 탭 2, 그룹 3).
    private val groupId: Long = 3

    private fun ComposeUiTest.openRunMenu() {
        onNodeWithTag(terminalRunTestTag(groupId)).performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.waitForTag(tag: String) {
        waitUntil(timeoutMillis = FrameTimeoutMillis) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun menuShowsDetectedProjectsAndTheCommandSection() = runComposeUiTest {
        openTerminal(repoWorkspace(), FakeProjectRunRepository(kinds = setOf(ProjectKind.Android, ProjectKind.IOS)))

        openRunMenu()

        waitForTag(TerminalRunMenuAndroidTestTag)
        onNodeWithTag(TerminalRunMenuIosTestTag).assertIsDisplayed()
        onNodeWithText("명령").assertIsDisplayed()
        onNodeWithTag(TerminalRunMenuAddCommandTestTag).assertIsDisplayed()
    }

    @Test
    fun notAProjectShowsOnlyCommands() = runComposeUiTest {
        openTerminal(repoWorkspace(), FakeProjectRunRepository(kinds = emptySet()))

        openRunMenu()

        onNodeWithTag(TerminalRunMenuAddCommandTestTag).assertIsDisplayed()
        assertEquals(0, onAllNodesWithTag(TerminalRunMenuAndroidTestTag).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithTag(TerminalRunMenuIosTestTag).fetchSemanticsNodes().size)
    }

    @Test
    fun commandsAreAddedRunEditedAndDeleted() = runComposeUiTest {
        val workspace = repoWorkspace()
        openTerminal(workspace)

        openRunMenu()
        onNodeWithTag(TerminalRunMenuAddCommandTestTag).performClick()
        onNodeWithTag(TerminalCommandDialogConfirmTestTag).assertIsNotEnabled()
        onNodeWithTag(TerminalCommandDialogTitleTestTag).performTextInput("테스트")
        onNodeWithTag(TerminalCommandDialogCommandTestTag).performTextInput("./gradlew test")
        onNodeWithTag(TerminalCommandDialogConfirmTestTag).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.commandsOf(1).isNotEmpty() }
        val command = workspace.workspace.value.commandsOf(1).single()
        assertEquals("테스트", command.title)
        assertEquals("./gradlew test", command.command)

        openRunMenu()
        onNodeWithText("./gradlew test").assertIsDisplayed()
        onNodeWithTag(terminalRunCommandTestTag(command.id)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.command != null }
        val tab = workspace.workspace.value.focusedTab!!
        assertEquals("./gradlew test", tab.command)
        assertEquals("/repo", tab.directory)
        onNodeWithText("테스트").assertIsDisplayed()
        onNodeWithTag(terminalTabKindTestTag(tab.id)).assertContentDescriptionEquals("실행")

        openRunMenu()
        onNodeWithTag(terminalRunCommandEditTestTag(command.id)).performClick()
        onNodeWithTag(TerminalCommandDialogCommandTestTag).performTextReplacement("make")
        onNodeWithTag(TerminalCommandDialogConfirmTestTag).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.commandsOf(1).single().command == "make" }

        openRunMenu()
        onNodeWithTag(terminalRunCommandDeleteTestTag(command.id)).performClick()
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.commandsOf(1).isEmpty() }
    }

    @Test
    fun androidRunOpensTheRunTabAndTheDeviceTab() = runComposeUiTest {
        val workspace = repoWorkspace()
        val projectRun = FakeProjectRunRepository(kinds = setOf(ProjectKind.Android))
        openTerminal(workspace, projectRun)

        openRunMenu()
        waitForTag(TerminalRunMenuAndroidTestTag)
        onNodeWithTag(TerminalRunMenuAndroidTestTag).performClick()

        waitForTag(TerminalRunDialogVariantTestTag)
        // debug 가 기본이고 켜진 첫 기기가 기본이다(R17).
        onNodeWithText("debug").assertIsDisplayed()
        waitForTag(TerminalRunDialogDeviceTestTag)
        onNodeWithText(RunningAndroidDevice.name).assertIsDisplayed()

        onNodeWithTag(TerminalRunDialogVariantTestTag).performClick()
        onNodeWithTag(terminalRunDialogOptionTestTag("release")).performClick()
        onNodeWithTag(TerminalRunDialogDeviceTestTag).performClick()
        // 꺼진 AVD 도 고를 수 있다. iOS 기기는 나오지 않는다.
        onNodeWithText("Android 에뮬레이터 · 꺼짐").assertIsDisplayed()
        assertEquals(0, onAllNodesWithTag(terminalRunDialogOptionTestTag(RunningSimulator.id)).fetchSemanticsNodes().size)
        onNodeWithTag(terminalRunDialogOptionTestTag(StoppedAndroidDevice.id)).performClick()
        onNodeWithTag(TerminalRunDialogConfirmTestTag).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.tabs.any { it.program == TerminalProgram.Device } }
        val request = projectRun.androidRequests.single()
        assertEquals(":androidApp", request.modulePath)
        assertEquals("release", request.variant.name)
        assertEquals(StoppedAndroidDevice.id, request.device.id)

        val after = workspace.workspace.value
        val root = assertIs<PaneNode.Split>(after.selectedPanel!!.root)
        assertEquals("android:release", after.focusedTab!!.command)
        assertEquals(StoppedAndroidDevice.id, assertIs<PaneNode.Group>(root.second).selectedTab.deviceId)
        assertEquals(AndroidRunChoice(":androidApp", "release", StoppedAndroidDevice.id), after.panels.first().androidRun)
        onNodeWithText("release · ${StoppedAndroidDevice.name}").assertIsDisplayed()
    }

    @Test
    fun iosRunOnAPhysicalDeviceOpensOnlyTheRunTab() = runComposeUiTest {
        val workspace = repoWorkspace()
        val projectRun = FakeProjectRunRepository(kinds = setOf(ProjectKind.IOS))
        openTerminal(workspace, projectRun)

        openRunMenu()
        waitForTag(TerminalRunMenuIosTestTag)
        onNodeWithTag(TerminalRunMenuIosTestTag).performClick()
        waitForTag(TerminalRunDialogDeviceTestTag)
        onNodeWithTag(TerminalRunDialogDeviceTestTag).performClick()
        onNodeWithTag(terminalRunDialogOptionTestTag(PhysicalIosDevice.id)).performClick()
        onNodeWithTag(TerminalRunDialogConfirmTestTag).performClick()

        waitUntil(timeoutMillis = FrameTimeoutMillis) { projectRun.iosRequests.isNotEmpty() }
        waitUntil(timeoutMillis = FrameTimeoutMillis) { workspace.workspace.value.focusedTab?.command != null }
        assertEquals("Debug", projectRun.iosRequests.single().configuration)
        assertEquals(0, workspace.workspace.value.tabs.count { it.program == TerminalProgram.Device })
    }

    @Test
    fun failedVariantReadingCanBeRetried() = runComposeUiTest {
        val projectRun = FakeProjectRunRepository(kinds = setOf(ProjectKind.Android))
        projectRun.androidLoads = mutableListOf(flowOf(ProjectLoad.Failed("boom")), MutableStateFlow(ProjectLoad.Loading))
        openTerminal(repoWorkspace(), projectRun)

        openRunMenu()
        waitForTag(TerminalRunMenuAndroidTestTag)
        onNodeWithTag(TerminalRunMenuAndroidTestTag).performClick()

        waitForTag(TerminalRunDialogErrorTestTag)
        onNodeWithText("boom").assertIsDisplayed()
        onNodeWithTag(TerminalRunDialogConfirmTestTag).assertIsNotEnabled()
        onNodeWithTag(TerminalRunDialogRetryTestTag).performClick()
        waitForTag(TerminalRunDialogLoadingTestTag)
    }

    private companion object {
        const val FrameTimeoutMillis = 10_000L
    }
}

internal class FakeProjectRunRepository(
    private val kinds: Set<ProjectKind> = emptySet(),
    override val isSupported: Boolean = true,
) : ProjectRunRepository {
    var androidLoads: MutableList<Flow<ProjectLoad<AndroidProject>>> = mutableListOf()

    val androidRequests = mutableListOf<AndroidRunRequest>()

    val iosRequests = mutableListOf<IosRunRequest>()

    override fun observeProjectKinds(directory: String): Flow<Set<ProjectKind>> = flowOf(kinds)

    override fun observeAndroidProject(directory: String): Flow<ProjectLoad<AndroidProject>> =
        androidLoads.removeFirstOrNull() ?: flowOf(
            ProjectLoad.Loaded(
                AndroidProject(directory, listOf(AndroidApp(":androidApp", listOf(AndroidVariant("debug", "app.id"), AndroidVariant("release", "app.id"))))),
            ),
        )

    override fun observeIosProject(directory: String): Flow<ProjectLoad<IosProject>> =
        flowOf(ProjectLoad.Loaded(IosProject(directory, "$directory/iosApp/iosApp.xcodeproj", false, listOf("iosApp"), listOf("Debug", "Release"))))

    override suspend fun androidRunCommand(request: AndroidRunRequest): String {
        androidRequests += request
        return "android:${request.variant.name}"
    }

    override suspend fun iosRunCommand(request: IosRunRequest): String {
        iosRequests += request
        return "ios:${request.configuration}"
    }
}
