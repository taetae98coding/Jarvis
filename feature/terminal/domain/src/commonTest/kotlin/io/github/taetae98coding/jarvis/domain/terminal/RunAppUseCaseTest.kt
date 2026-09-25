package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RunAppUseCaseTest {
    private val repository = FakeProjectRunRepository()

    private val workspaces = InMemoryTerminalWorkspaceRepository(
        TerminalWorkspace.initial().let { TerminalWorkspace(it.panels.map { panel -> panel.copy(directory = "/repo") }, it.selectedPanelId, it.nextId) },
    )

    private val update = UpdateTerminalWorkspaceUseCase(workspaces, RecordingTerminalRepository())

    @Test
    fun androidRunOpensTheCommandTabAndRemembersTheChoice() = runTest {
        val device = RunDevice("avd:Pixel_9", "Pixel_9", DevicePlatform.Android, isRunning = false)
        val request = AndroidRunRequest("/repo", ":androidApp", AndroidVariant("debug", "app.id"), device)

        RunAndroidAppUseCase(repository, update)(panelId = 1, groupId = null, request = request)

        val workspace = workspaces.workspace.value
        val runTab = workspace.focusedTab!!
        assertEquals("android:$request", runTab.command)
        assertEquals("debug · Pixel_9", runTab.commandTitle)
        assertEquals("/repo", runTab.directory)
        assertEquals("avd:Pixel_9", workspace.tabs.single { it.program == TerminalProgram.Device }.deviceId)
        assertEquals(AndroidRunChoice(":androidApp", "debug", "avd:Pixel_9"), workspace.panels.first().androidRun)
    }

    @Test
    fun aPhysicalIosDeviceGetsNoDeviceTab() = runTest {
        val device = RunDevice("ios:0000", "iPhone", DevicePlatform.IOS, isPhysical = true, canMirror = false)
        val project = IosProject("/repo", "/repo/iosApp/iosApp.xcodeproj", isWorkspace = false, listOf("iosApp"), listOf("Debug", "Release"))

        RunIosAppUseCase(repository, update)(1, null, IosRunRequest(project, "iosApp", "Release", device))

        val workspace = workspaces.workspace.value
        assertEquals("Release · iPhone", workspace.focusedTab!!.commandTitle)
        assertEquals(0, workspace.tabs.count { it.program == TerminalProgram.Device })
        assertEquals(IosRunChoice("iosApp", "Release", "ios:0000"), workspace.panels.first().iosRun)
    }

    @Test
    fun unsupportedTargetsDoNothing() = runTest {
        repository.isSupported = false
        val before = workspaces.workspace.value
        val device = RunDevice("emulator-5554", "Pixel", DevicePlatform.Android)

        val change = RunAndroidAppUseCase(repository, update)(1, null, AndroidRunRequest("/repo", ":app", AndroidVariant("debug", null), device))

        assertNull(change)
        assertEquals(before, workspaces.workspace.value)
    }
}

internal class FakeProjectRunRepository : ProjectRunRepository {
    override var isSupported: Boolean = true

    override fun observeProjectKinds(directory: String): Flow<Set<ProjectKind>> = flowOf(emptySet())

    override fun observeAndroidProject(directory: String): Flow<ProjectLoad<AndroidProject>> = flowOf(ProjectLoad.Loading)

    override fun observeIosProject(directory: String): Flow<ProjectLoad<IosProject>> = flowOf(ProjectLoad.Loading)

    override suspend fun androidRunCommand(request: AndroidRunRequest): String = "android:$request"

    override suspend fun iosRunCommand(request: IosRunRequest): String = "ios:$request"
}
