package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.data.PlatformContext

// 기기의 샌드박스가 자식 프로세스 생성을 막고, NSTask 는 iOS SDK 에 없다.
// 데스크톱 에이전트로 셸을 빌려 오는 안을 버린 이유는 docs/platform/ios.html#terminal 에 있다.
internal actual fun createTerminalDataSource(context: PlatformContext): TerminalDataSource =
    UnsupportedTerminalDataSource
