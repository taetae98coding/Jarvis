package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.PaneNode
import io.github.taetae98coding.jarvis.domain.terminal.SplitDirection
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
)

@Serializable
internal data class TerminalTabDto(
    val id: Long,
    val program: String = ShellProgram,
    val directory: String? = null,
    val claudeSessionId: String? = null,
    val url: String? = null,
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
            )
        },
        selectedPanelId = selectedPanelId,
        nextId = nextId,
    )

/** 패널이 하나도 없거나 그룹이 하나도 없는 파일은 처음 켠 것과 같게 읽는다. 화면에는 늘 패널이 하나 이상 있다. */
internal fun TerminalWorkspaceDto.toDomain(): TerminalWorkspace {
    val restored = panels.map { panel ->
        TerminalPanel(
            id = panel.id,
            name = panel.name,
            root = panel.root?.toDomain(),
            focusedGroupId = panel.focusedGroupId,
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
                    },
                    directory = tab.directory,
                    claudeSessionId = tab.claudeSessionId,
                    url = tab.url,
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
        program == ClaudeProgram && claudeSessionId != null -> TerminalTab(id, TerminalProgram.Claude, directory, claudeSessionId)
        program == BrowserProgram -> TerminalTab(id, TerminalProgram.Browser, url = url ?: TerminalTab.DefaultBrowserUrl)
        else -> TerminalTab(id, directory = directory)
    }
