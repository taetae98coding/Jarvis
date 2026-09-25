package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.AndroidRunChoice
import io.github.taetae98coding.jarvis.domain.terminal.DevicePlatform
import io.github.taetae98coding.jarvis.domain.terminal.IosRunChoice
import io.github.taetae98coding.jarvis.domain.terminal.PaneNode
import io.github.taetae98coding.jarvis.domain.terminal.SplitDirection
import io.github.taetae98coding.jarvis.domain.terminal.TerminalCommand
import io.github.taetae98coding.jarvis.domain.terminal.TerminalPanel
import io.github.taetae98coding.jarvis.domain.terminal.TerminalProgram
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspace
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 저장 파일의 형식. 도메인 이름이 바뀌어도 파일이 흔들리지 않게 도메인 모델과 따로 둔다.
 *
 * 열거값은 문자열로 둔다. 모르는 값(다음 버전이 쓴 파일)이 와도 파일 전체를 버리지 않고 그 탭만
 * 기본값으로 읽는다.
 *
 * 그룹 이전 형식(패널마다 `tabs`, 탭마다 분할 트리)은 변환하지 않는다. 그 키는 모르는 키로 버려져
 * 모든 패널의 `root` 가 null 이 되고, 아래 [toDomain] 이 처음 켠 것으로 읽는다
 * (docs/common/terminal-tab-groups.html#implementation).
 */
@Serializable
internal data class TerminalWorkspaceDto(
    val panels: List<TerminalPanelDto> = emptyList(),
    val selectedPanelId: Long? = null,
    val nextId: Long = 1,
)

@Serializable
internal data class TerminalPanelDto(
    val id: Long,
    val name: String,
    val root: PaneNodeDto? = null,
    val focusedGroupId: Long? = null,
    val directory: String? = null,
    val parentId: Long? = null,
    val branch: String? = null,
    val baseBranch: String? = null,
    val commands: List<TerminalCommandDto> = emptyList(),
    val androidRun: AndroidRunChoiceDto? = null,
    val iosRun: IosRunChoiceDto? = null,
)

@Serializable
internal data class TerminalCommandDto(
    val id: Long,
    val title: String? = null,
    val command: String,
)

@Serializable
internal data class AndroidRunChoiceDto(
    val modulePath: String,
    val variant: String,
    val deviceId: String,
)

@Serializable
internal data class IosRunChoiceDto(
    val scheme: String,
    val configuration: String,
    val deviceId: String,
)

/**
 * [command]·[commandTitle] 은 쓰기만 한다. 다시 켤 때 명령을 다시 돌리지 않으려고 읽을 때 버리고 셸 탭으로 읽는다
 * (docs/common/terminal-run.html R18). 파일을 들여다볼 때 어떤 탭이었는지 알 수 있게 남겨 둔다.
 */
@Serializable
internal data class TerminalTabDto(
    val id: Long,
    val program: String = ShellProgram,
    val directory: String? = null,
    val claudeSessionId: String? = null,
    val url: String? = null,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val devicePlatform: String? = null,
    val deviceLogVisible: Boolean = false,
    val name: String? = null,
    val claudeCheckedAt: Long? = null,
    val filePath: String? = null,
    val commitHash: String? = null,
    val command: String? = null,
    val commandTitle: String? = null,
    val commandTyped: Boolean = false,
)

@Serializable
internal sealed interface PaneNodeDto {
    @Serializable
    @SerialName("group")
    data class Group(
        val id: Long,
        val tabs: List<TerminalTabDto> = emptyList(),
        val selectedTabId: Long? = null,
    ) : PaneNodeDto

    @Serializable
    @SerialName("split")
    data class Split(
        val id: Long,
        val direction: String,
        val first: PaneNodeDto,
        val second: PaneNodeDto,
        val ratio: Float = 0.5f,
    ) : PaneNodeDto
}

private const val ShellProgram = "shell"
private const val ClaudeProgram = "claude"
private const val BrowserProgram = "browser"
private const val DeviceProgram = "device"
private const val FileProgram = "file"
private const val AndroidPlatform = "android"
private const val IosPlatform = "ios"
private const val SideBySideDirection = "sideBySide"
private const val StackedDirection = "stacked"

internal fun TerminalWorkspace.toDto(): TerminalWorkspaceDto =
    TerminalWorkspaceDto(
        panels = panels.map { panel ->
            TerminalPanelDto(
                id = panel.id,
                name = panel.name,
                root = panel.root?.toDto(),
                focusedGroupId = panel.focusedGroupId,
                directory = panel.directory,
                parentId = panel.parentId,
                branch = panel.branch,
                baseBranch = panel.baseBranch,
                commands = panel.commands.map { TerminalCommandDto(it.id, it.title, it.command) },
                androidRun = panel.androidRun?.let { AndroidRunChoiceDto(it.modulePath, it.variant, it.deviceId) },
                iosRun = panel.iosRun?.let { IosRunChoiceDto(it.scheme, it.configuration, it.deviceId) },
            )
        },
        selectedPanelId = selectedPanelId,
        nextId = nextId,
    )

/**
 * 패널이 하나도 없거나 그룹이 하나도 없는 파일은 처음 켠 것과 같게 읽는다. 화면에는 늘 패널이 하나 이상 있다.
 * 부모가 목록에 없거나 부모 자신이 워크트리 패널인 `parentId` 는 버려 최상위로 읽는다 — 목록은 한 단계만 그린다.
 */
internal fun TerminalWorkspaceDto.toDomain(): TerminalWorkspace {
    val topLevelIds = panels.filter { it.parentId == null }.map { it.id }.toSet()
    val restored = panels.map { panel ->
        TerminalPanel(
            id = panel.id,
            name = panel.name,
            root = panel.root?.toDomain(),
            focusedGroupId = panel.focusedGroupId,
            directory = panel.directory,
            parentId = panel.parentId?.takeIf { it in topLevelIds },
            branch = panel.branch,
            baseBranch = panel.baseBranch,
            commands = panel.commands.filter { it.command.isNotBlank() }.map { TerminalCommand(it.id, it.title?.ifBlank { null }, it.command) },
            androidRun = panel.androidRun?.let { AndroidRunChoice(it.modulePath, it.variant, it.deviceId) },
            iosRun = panel.iosRun?.let { IosRunChoice(it.scheme, it.configuration, it.deviceId) },
        )
    }
    if (restored.none { it.root != null }) return TerminalWorkspace.initial()

    return TerminalWorkspace(panels = restored, selectedPanelId = selectedPanelId, nextId = nextId)
}

private fun PaneNode.toDto(): PaneNodeDto =
    when (this) {
        is PaneNode.Group -> PaneNodeDto.Group(
            id = id,
            tabs = tabs.map { tab ->
                TerminalTabDto(
                    id = tab.id,
                    program = when (tab.program) {
                        TerminalProgram.Shell -> ShellProgram
                        TerminalProgram.Claude -> ClaudeProgram
                        TerminalProgram.Browser -> BrowserProgram
                        TerminalProgram.Device -> DeviceProgram
                        TerminalProgram.File -> FileProgram
                    },
                    directory = tab.directory,
                    claudeSessionId = tab.claudeSessionId,
                    url = tab.url,
                    deviceId = tab.deviceId,
                    deviceName = tab.deviceName,
                    devicePlatform = when (tab.devicePlatform) {
                        DevicePlatform.Android -> AndroidPlatform
                        DevicePlatform.IOS -> IosPlatform
                        null -> null
                    },
                    deviceLogVisible = tab.deviceLogVisible,
                    name = tab.name,
                    claudeCheckedAt = tab.claudeCheckedAt,
                    filePath = tab.filePath,
                    commitHash = tab.commitHash,
                    command = tab.command,
                    commandTitle = tab.commandTitle,
                    commandTyped = tab.commandTyped,
                )
            },
            selectedTabId = selectedTabId,
        )

        is PaneNode.Split -> PaneNodeDto.Split(
            id = id,
            direction = when (direction) {
                SplitDirection.SideBySide -> SideBySideDirection
                SplitDirection.Stacked -> StackedDirection
            },
            first = first.toDto(),
            second = second.toDto(),
            ratio = ratio,
        )
    }

// 탭이 없는 그룹은 도메인에 없는 값이라 없는 것으로 읽고, 한쪽이 빈 분할은 남은 쪽으로 접는다.
private fun PaneNodeDto.toDomain(): PaneNode? =
    when (this) {
        is PaneNodeDto.Group -> {
            val tabs = tabs.map { it.toDomain() }
            if (tabs.isEmpty()) null else PaneNode.Group(id, tabs, selectedTabId ?: tabs.first().id)
        }

        is PaneNodeDto.Split -> {
            val first = first.toDomain()
            val second = second.toDomain()
            when {
                first == null -> second
                second == null -> first
                else -> PaneNode.Split(
                    id = id,
                    direction = if (direction == StackedDirection) SplitDirection.Stacked else SplitDirection.SideBySide,
                    first = first,
                    second = second,
                    ratio = ratio.coerceIn(TerminalWorkspace.MinRatio, TerminalWorkspace.MaxRatio),
                )
            }
        }
    }

private fun TerminalTabDto.toDomain(): TerminalTab =
    when {
        program == ClaudeProgram && claudeSessionId != null ->
            TerminalTab(id, TerminalProgram.Claude, directory, claudeSessionId, claudeCheckedAt = claudeCheckedAt)
        program == BrowserProgram -> TerminalTab(id, TerminalProgram.Browser, url = url ?: TerminalTab.DefaultBrowserUrl)
        program == DeviceProgram && deviceId != null -> TerminalTab(
            id = id,
            program = TerminalProgram.Device,
            deviceId = deviceId,
            deviceName = deviceName,
            devicePlatform = when (devicePlatform) {
                AndroidPlatform -> DevicePlatform.Android
                IosPlatform -> DevicePlatform.IOS
                else -> null
            },
            deviceLogVisible = deviceLogVisible,
        )
        program == FileProgram && filePath != null -> TerminalTab(id, TerminalProgram.File, filePath = filePath, commitHash = commitHash?.ifEmpty { null })

        // 명령은 그대로 읽는다. 앱을 다시 켰을 때만 지우는 것은 저장소의 몫이다(docs/common/terminal-run.html R18).
        else -> TerminalTab(id, directory = directory, command = command?.ifEmpty { null }, commandTitle = commandTitle, commandTyped = commandTyped)
    }.copy(name = name?.trim()?.ifEmpty { null })
