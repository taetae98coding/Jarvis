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
 * 열거값은 문자열로 둔다. 모르는 값(다음 버전이 쓴 파일)이 와도 파일 전체를 버리지 않고 그 창만
 * 기본값으로 읽는다.
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
    val tabs: List<TerminalTabDto> = emptyList(),
    val selectedTabId: Long? = null,
)

@Serializable
internal data class TerminalTabDto(
    val id: Long,
    val root: PaneNodeDto,
    val focusedPaneId: Long,
)

@Serializable
internal sealed interface PaneNodeDto {
    @Serializable
    @SerialName("leaf")
    data class Leaf(
        val paneId: Long,
        val program: String = ShellProgram,
        val directory: String? = null,
        val claudeSessionId: String? = null,
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
private const val SideBySideDirection = "sideBySide"
private const val StackedDirection = "stacked"

internal fun TerminalWorkspace.toDto(): TerminalWorkspaceDto =
    TerminalWorkspaceDto(
        panels = panels.map { panel ->
            TerminalPanelDto(
                id = panel.id,
                name = panel.name,
                tabs = panel.tabs.map { TerminalTabDto(it.id, it.root.toDto(), it.focusedPaneId) },
                selectedTabId = panel.selectedTabId,
            )
        },
        selectedPanelId = selectedPanelId,
        nextId = nextId,
    )

/** 패널이 하나도 없는 파일은 처음 켠 것과 같게 읽는다. 화면에는 늘 패널이 하나 이상 있다. */
internal fun TerminalWorkspaceDto.toDomain(): TerminalWorkspace {
    if (panels.isEmpty()) return TerminalWorkspace.initial()

    return TerminalWorkspace(
        panels = panels.map { panel ->
            TerminalPanel(
                id = panel.id,
                name = panel.name,
                tabs = panel.tabs.map { TerminalTab(it.id, it.root.toDomain(), it.focusedPaneId) },
                selectedTabId = panel.selectedTabId,
            )
        },
        selectedPanelId = selectedPanelId,
        nextId = nextId,
    )
}

private fun PaneNode.toDto(): PaneNodeDto =
    when (this) {
        is PaneNode.Leaf -> PaneNodeDto.Leaf(
            paneId = paneId,
            program = when (program) {
                TerminalProgram.Shell -> ShellProgram
                TerminalProgram.Claude -> ClaudeProgram
            },
            directory = directory,
            claudeSessionId = claudeSessionId,
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

private fun PaneNodeDto.toDomain(): PaneNode =
    when (this) {
        is PaneNodeDto.Leaf -> PaneNode.Leaf(
            paneId = paneId,
            program = if (program == ClaudeProgram && claudeSessionId != null) TerminalProgram.Claude else TerminalProgram.Shell,
            directory = directory,
            claudeSessionId = claudeSessionId,
        )

        is PaneNodeDto.Split -> PaneNode.Split(
            id = id,
            direction = if (direction == StackedDirection) SplitDirection.Stacked else SplitDirection.SideBySide,
            first = first.toDomain(),
            second = second.toDomain(),
            ratio = ratio.coerceIn(TerminalWorkspace.MinRatio, TerminalWorkspace.MaxRatio),
        )
    }
