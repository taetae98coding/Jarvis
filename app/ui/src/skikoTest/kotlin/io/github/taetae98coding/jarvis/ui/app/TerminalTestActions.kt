package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import io.github.taetae98coding.jarvis.ui.terminal.TerminalScreenTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalGroupTestTag
import io.github.taetae98coding.jarvis.ui.terminal.terminalTabTestTag

/** ⌘+[key] (Shift 는 선택). 포커스는 창의 입력 필드에 있고 화면 루트가 먼저 가로챈다. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.pressTerminalShortcut(key: Key, shift: Boolean = false) {
    onNodeWithTag(TerminalScreenTestTag).performKeyInput {
        withKeyDown(Key.MetaLeft) {
            if (shift) withKeyDown(Key.ShiftLeft) { pressKey(key) } else pressKey(key)
        }
    }
}

/** 그룹마다 하나씩 있는 새 탭(+) 버튼. 그룹이 하나뿐일 때 `onNode` 로 고른다. */
internal val newTabButton: SemanticsMatcher = SemanticsMatcher("testTag starts with terminal:new-tab:") {
    it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("terminal:new-tab:") == true
}

/**
 * 마우스로 [tabId] 탭을 눌러 [groupId] 그룹 영역 안의 [fraction](0..1, 왼쪽 위 기준) 자리까지 끌고 간다.
 * [release] 가 false 면 누른 채로 둔다 — 끄는 동안의 화면을 볼 때 쓴다.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.dragTabOntoGroup(tabId: Long, groupId: Long, fraction: Offset, release: Boolean = true) {
    val group = onNodeWithTag(terminalGroupTestTag(groupId)).fetchSemanticsNode().boundsInRoot
    val destination = Offset(group.left + group.width * fraction.x, group.top + group.height * fraction.y)

    dragTabTo(tabId, destination, release)
}

/** 마우스로 [tabId] 탭을 눌러 화면 루트 좌표 [destination] 까지 끌고 간다. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.dragTabTo(tabId: Long, destination: Offset, release: Boolean = true) {
    val tab = onNodeWithTag(terminalTabTestTag(tabId))
    val origin = tab.fetchSemanticsNode().boundsInRoot.topLeft

    tab.performMouseInput {
        // 마우스 위치는 이전 입력 자리에 남아 있다. 옮기지 않고 누르면 이 노드 밖을 누를 수 있다.
        moveTo(center)
        press()
        // 슬롭을 넘기는 첫 이동과 목적지까지의 이동을 나눠서, 시작 판정과 이동이 서로 다른 이벤트로 온다.
        moveBy(Offset(SlopDistance, SlopDistance))
        moveTo(destination - origin)
        moveTo(destination - origin)
        if (release) release()
    }
}

private const val SlopDistance = 40f
