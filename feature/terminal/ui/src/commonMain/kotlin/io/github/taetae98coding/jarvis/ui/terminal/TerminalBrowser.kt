package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.ChromeProfile
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.browserAddress
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

fun terminalBrowserTestTag(tabId: Long): String = "terminal:browser:$tabId"

fun terminalBrowserAddressTestTag(tabId: Long): String = "terminal:browser-address:$tabId"

fun terminalChromeImportTestTag(tabId: Long): String = "terminal:chrome-import:$tabId"

fun terminalChromeImportItemTestTag(profileDirectory: String): String = "terminal:chrome-import:menu:$profileDirectory"

/**
 * 브라우저 탭의 창. 주소 줄은 공통이고 페이지는 [rememberBrowserPage] 가 준다. JVM 의 페이지는 탭이 가려져도 살아 있고,
 * Android 의 웹뷰는 이 컴포지션에만 있다(docs/common/terminal-browser.html R6).
 */
@Composable
internal fun TerminalBrowser(
    tab: TerminalTab,
    isSupported: Boolean,
    isChromeImportSupported: Boolean,
    pageHidden: Boolean,
    onUrl: (String) -> Unit,
    onTitle: (String?) -> Unit,
    onFocus: () -> Unit,
    chromeProfiles: suspend () -> List<ChromeProfile>,
    importCookies: suspend (String) -> List<BrowserCookie>,
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

    val page = rememberBrowserPage(tab)
    val address = rememberTextFieldState(page.url ?: tab.url ?: TerminalTab.DefaultBrowserUrl)
    val currentOnUrl by rememberUpdatedState(onUrl)
    val currentOnTitle by rememberUpdatedState(onTitle)
    val scope = rememberCoroutineScope()
    var importMenuExpanded by remember { mutableStateOf(false) }
    var profiles by remember { mutableStateOf(emptyList<ChromeProfile>()) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(page) {
        snapshotFlow { page.url }.filterNotNull().collect { url ->
            address.setTextAndPlaceCursorAtEnd(url)
            currentOnUrl(url)
        }
    }
    LaunchedEffect(page) {
        snapshotFlow { page.title }.collect { currentOnTitle(it) }
    }

    if (page.failed) {
        Box(modifier = modifier.testTag(terminalBrowserTestTag(tab.id)), contentAlignment = Alignment.Center) {
            Text(
                text = "[웹 페이지를 열 수 없습니다]",
                style = JarvisTheme.typography.bodyMedium,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Box(modifier = modifier.testTag(terminalBrowserTestTag(tab.id))) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JarvisIconButton(
                icon = JarvisIcons.Back,
                contentDescription = "뒤로",
                onClick = page::back,
                enabled = page.canGoBack,
            )
            JarvisIconButton(
                icon = JarvisIcons.Forward,
                contentDescription = "앞으로",
                onClick = page::forward,
                enabled = page.canGoForward,
            )
            JarvisIconButton(icon = JarvisIcons.RotateRight, contentDescription = "새로 고침", onClick = page::reload)

            if (isChromeImportSupported) {
                Box {
                    JarvisIconButton(
                        icon = JarvisIcons.User,
                        contentDescription = "Chrome 계정 가져오기",
                        onClick = {
                            // 드롭다운을 열 때 지금 프로필 목록을 읽는다.
                            scope.launch { profiles = chromeProfiles() }
                            importMenuExpanded = true
                        },
                        modifier = Modifier.testTag(terminalChromeImportTestTag(tab.id)),
                    )

                    DropdownMenu(expanded = importMenuExpanded, onDismissRequest = { importMenuExpanded = false }) {
                        if (profiles.isEmpty()) {
                            DropdownMenuItem(text = { Text("가져올 계정이 없습니다") }, onClick = {}, enabled = false)
                        } else {
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { ChromeProfileLabel(profile) },
                                    onClick = {
                                        importMenuExpanded = false
                                        scope.launch {
                                            // 웹뷰·쿠키 저장소가 실패해도 앱이 죽지 않게 감싼다(공통 스펙 R8).
                                            val (count, stored) = runCatching {
                                                val cookies = importCookies(profile.directory)
                                                // 넣은 도메인을 되읽어 실제로 저장소에 들어갔는지 확인한다(R8a).
                                                cookies.size to (cookies.isNotEmpty() && page.importCookies(cookies))
                                            }.getOrDefault(0 to false)

                                            // 실제로 저장됐을 때만 지금 페이지에 바로 먹도록 새로 고친다(R3).
                                            if (count > 0 && stored) page.reload()
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(
                                                message = when {
                                                    count == 0 -> "쿠키를 가져오지 못했습니다 — 키체인 접근을 허용했는지 확인하세요"
                                                    !stored -> "웹뷰가 준비되지 않아 넣지 못했습니다 — 페이지가 뜬 뒤 다시 시도하세요"
                                                    else -> "쿠키 ${count}개를 가져왔습니다"
                                                },
                                                duration = SnackbarDuration.Short,
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag(terminalChromeImportItemTestTag(profile.directory)),
                                )
                            }
                        }
                    }
                }
            }

            val textColor = JarvisTheme.colorScheme.onSurface
            BasicTextField(
                state = address,
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                textStyle = JarvisTheme.typography.bodyMedium.copy(color = textColor),
                cursorBrush = SolidColor(textColor),
                onKeyboardAction = { browserAddress(address.text.toString())?.let(page::load) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(terminalBrowserAddressTestTag(tab.id))
                    // 페이지는 네이티브 뷰라 눌러도 Compose 포커스가 오지 않는다. 주소 줄이 그룹 포커스를 대신 받는다.
                    .onFocusChanged { if (it.isFocused) onFocus() }
                    .background(JarvisTheme.colorScheme.surfaceVariant, JarvisTheme.shapes.small)
                    .padding(horizontal = JarvisTheme.dimens.spacing.s, vertical = JarvisTheme.dimens.spacing.xs),
            )
        }

        // 네이티브 페이지는 가져오기 드롭다운·Snackbar 도 가리므로 떠 있는 동안 치운다(R9, 쿠키 가져오기 R6).
        val pageCovered = page.coversOverlays && (pageHidden || importMenuExpanded || snackbarHostState.currentSnackbarData != null)
        BrowserPageView(
            page = page,
            hidden = pageCovered,
            onFocus = onFocus,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
      }

      SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ChromeProfileLabel(profile: ChromeProfile) {
    Column {
        Text(text = profile.name, style = JarvisTheme.typography.bodyMedium)
        val email = profile.email
        if (email != null) {
            Text(
                text = email,
                style = JarvisTheme.typography.bodySmall,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
