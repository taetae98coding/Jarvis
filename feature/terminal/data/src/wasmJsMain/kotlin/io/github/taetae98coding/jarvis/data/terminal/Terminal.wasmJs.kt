package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.PlatformContext

// 브라우저 페이지는 로컬 프로세스를 띄울 수 없다.
// 데스크톱 에이전트로 셸을 빌려 오는 안을 버린 이유는 docs/platform/web.html#terminal 에 있다.
internal actual fun createTerminalDataSource(context: PlatformContext): TerminalDataSource =
    UnsupportedTerminalDataSource
