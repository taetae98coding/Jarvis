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
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.webview.cookie.Cookie
import io.github.kdroidfilter.webview.web.WebView
import io.github.kdroidfilter.webview.web.rememberWebViewNavigator
import io.github.kdroidfilter.webview.web.rememberWebViewState
import io.github.taetae98coding.jarvis.designsystem.component.JarvisIconButton
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.ChromeProfile
import io.github.taetae98coding.jarvis.domain.terminal.TerminalTab
import io.github.taetae98coding.jarvis.domain.terminal.browserAddress
import io.github.taetae98coding.jarvis.domain.terminal.expandCookiesForHostOnlyStore
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

fun terminalBrowserTestTag(tabId: Long): String = "terminal:browser:$tabId"

fun terminalBrowserAddressTestTag(tabId: Long): String = "terminal:browser-address:$tabId"

fun terminalChromeImportTestTag(tabId: Long): String = "terminal:chrome-import:$tabId"

fun terminalChromeImportItemTestTag(profileDirectory: String): String = "terminal:chrome-import:menu:$profileDirectory"

/**
 * 브라우저 탭의 창. 웹뷰는 이 컴포지션에만 있다 — 탭이 가려지면 버리고, 다시 보이면 저장된 주소로
 * 새로 연다(docs/common/terminal-browser.html R6).
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

    // rememberWebViewState 는 리컴포지션마다 넘긴 주소를 content 에 다시 넣고, content 가 바뀌면 웹뷰가 그 주소를
    // 다시 연다. 페이지를 옮길 때마다 저장되는 tab.url 을 넘기면 옮긴 페이지를 한 번 더 부른다. 처음 주소로 고정한다.
    val initialUrl = remember { tab.url ?: TerminalTab.DefaultBrowserUrl }
    val state = rememberWebViewState(initialUrl)
    val navigator = rememberWebViewNavigator()
    val address = rememberTextFieldState(initialUrl)
    val currentOnUrl by rememberUpdatedState(onUrl)
    val currentOnTitle by rememberUpdatedState(onTitle)
    val scope = rememberCoroutineScope()
    var importMenuExpanded by remember { mutableStateOf(false) }
    var profiles by remember { mutableStateOf(emptyList<ChromeProfile>()) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        snapshotFlow { state.lastLoadedUrl }.filterNotNull().collect { url ->
            address.setTextAndPlaceCursorAtEnd(url)
            currentOnUrl(url)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.pageTitle }.collect { currentOnTitle(it) }
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
                                                // 데스크톱 웹뷰(wry)는 도메인 쿠키를 못 넣으므로 호스트별로 펴서 넣는다(R3c).
                                                expandCookiesForHostOnlyStore(cookies).forEach { cookie ->
                                                    state.cookieManager.setCookie(cookie.storeUrl(), cookie.toWebViewCookie())
                                                }
                                                // setCookie 는 웹뷰 panel 이 안 붙었으면 예외 없이 no-op 이다(라이브러리 동작).
                                                // 넣은 도메인을 되읽어 실제로 저장소에 들어갔는지 확인한다(R8a).
                                                val stored = cookies.firstOrNull()
                                                    ?.let { state.cookieManager.getCookies(it.storeUrl()).isNotEmpty() } ?: false
                                                cookies.size to stored
                                            }.getOrDefault(0 to false)

                                            // 실제로 저장됐을 때만 지금 페이지에 바로 먹도록 새로 고친다(R3).
                                            if (count > 0 && stored) navigator.reload()
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
        // 가져오기 드롭다운·Snackbar 도 네이티브 페이지에 가려지므로 떠 있는 동안 페이지를 치운다.
        val pageCovered = pageHidden || importMenuExpanded || snackbarHostState.currentSnackbarData != null
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            WebView(
                state = state,
                navigator = navigator,
                modifier = if (pageCovered) Modifier.size(0.dp) else Modifier.fillMaxSize(),
            )
        }
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

/** 쿠키를 넣을 때 넘기는 주소. 도메인 앞의 `.` 을 떼고 secure 면 https 로. */
private fun BrowserCookie.storeUrl(): String {
    val host = domain.trimStart('.')
    return if (isSecure) "https://$host/" else "http://$host/"
}

private fun BrowserCookie.toWebViewCookie(): Cookie =
    Cookie(
        name = name,
        value = value,
        domain = domain.removePrefix("."),
        path = path,
        // 라이브러리는 expiresDate 를 epoch 밀리초로 네이티브에 넘긴다(WebViewCookie.expiresDateMs).
        expiresDate = expiresEpochSeconds?.let { it * 1_000L },
        isSessionOnly = isSessionOnly,
        // WKWebView 는 Secure 가 아닌 SameSite=None 쿠키를 버린다. 미지정(-1)은 Chrome 기본값 Lax 로,
        // None 은 Secure 일 때만 None 으로 둔다(R3a). 안 그러면 Google 의 SID·HSID 가 버려져 로그아웃된다.
        sameSite = when {
            sameSite == 2 -> Cookie.HTTPCookieSameSitePolicy.STRICT
            sameSite == 0 && isSecure -> Cookie.HTTPCookieSameSitePolicy.NONE
            else -> Cookie.HTTPCookieSameSitePolicy.LAX
        },
        isSecure = isSecure,
        isHttpOnly = isHttpOnly,
        maxAge = null,
    )
