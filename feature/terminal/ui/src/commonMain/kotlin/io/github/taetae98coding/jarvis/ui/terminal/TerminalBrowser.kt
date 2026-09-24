package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.webview.web.WebView
import io.github.kdroidfilter.webview.web.rememberWebViewNavigator
import io.github.kdroidfilter.webview.web.rememberWebViewState
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.browserAddress
import kotlinx.coroutines.flow.filterNotNull

fun terminalBrowserTestTag(tabId: Long): String = "terminal:browser:$tabId"

fun terminalBrowserAddressTestTag(tabId: Long): String = "terminal:browser-address:$tabId"

/**
 * 브라우저 탭의 창. 웹뷰는 이 컴포지션에만 있다 — 탭이 가려지면 버리고, 다시 보이면 저장된 주소로
 * 새로 연다(docs/common/terminal-browser.html R6).
 */
@Composable
internal fun TerminalBrowser(
    tab: TerminalTab,
    isSupported: Boolean,
    pageHidden: Boolean,
    onUrl: (String) -> Unit,
    onTitle: (String?) -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isSupported) {
        Box(modifier = modifier.testTag(terminalBrowserTestTag(tab.id)), contentAlignment = Alignment.Center) {
            Text(
                text = "[웹 페이지를 열 수 없습니다]",
                style = JarvisTheme.typography.bodyMedium,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // rememberWebViewState 는 리컴포지션마다 넘긴 주소를 content 에 다시 넣고, content 가 바뀌면 웹뷰가 그 주소를
    // 다시 연다. 페이지를 옮길 때마다 저장되는 tab.url 을 넘기면 옮긴 페이지를 한 번 더 부른다. 처음 주소로 고정한다.
    val initialUrl = remember { tab.url ?: TerminalTab.DefaultBrowserUrl }
    val state = rememberWebViewState(initialUrl)
    val navigator = rememberWebViewNavigator()
    val address = rememberTextFieldState(initialUrl)
    val currentOnUrl by rememberUpdatedState(onUrl)
    val currentOnTitle by rememberUpdatedState(onTitle)

    LaunchedEffect(state) {
        snapshotFlow { state.lastLoadedUrl }.filterNotNull().collect { url ->
            address.setTextAndPlaceCursorAtEnd(url)
            currentOnUrl(url)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.pageTitle }.collect { currentOnTitle(it) }
    }

    Column(
        modifier = modifier.testTag(terminalBrowserTestTag(tab.id)),
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JarvisIconButton(
                icon = JarvisIcons.Back,
                contentDescription = "뒤로",
                onClick = navigator::navigateBack,
                enabled = navigator.canGoBack,
            )
            JarvisIconButton(
                icon = JarvisIcons.Forward,
                contentDescription = "앞으로",
                onClick = navigator::navigateForward,
                enabled = navigator.canGoForward,
            )
            JarvisIconButton(icon = JarvisIcons.RotateRight, contentDescription = "새로 고침", onClick = navigator::reload)

            val textColor = JarvisTheme.colorScheme.onSurface
            BasicTextField(
                state = address,
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                textStyle = JarvisTheme.typography.bodyMedium.copy(color = textColor),
                cursorBrush = SolidColor(textColor),
                onKeyboardAction = { browserAddress(address.text.toString())?.let(navigator::loadUrl) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(terminalBrowserAddressTestTag(tab.id))
                    // 페이지는 네이티브 뷰라 눌러도 Compose 포커스가 오지 않는다. 주소 줄이 그룹 포커스를 대신 받는다.
                    .onFocusChanged { if (it.isFocused) onFocus() }
                    .background(JarvisTheme.colorScheme.surfaceVariant, JarvisTheme.shapes.small)
                    .padding(horizontal = JarvisTheme.dimens.spacing.s, vertical = JarvisTheme.dimens.spacing.xs),
            )
        }

        // 페이지를 컴포지션에서 빼면 웹뷰가 닫히고 다시 열린다. 크기만 0 으로 줄여 살려 둔 채 치운다.
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            WebView(
                state = state,
                navigator = navigator,
                modifier = if (pageHidden) Modifier.size(0.dp) else Modifier.fillMaxSize(),
            )
        }
    }
}
