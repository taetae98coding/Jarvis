package io.github.taetae98coding.jarvis.ui.terminal

import io.github.taetae98coding.jarvis.domain.terminal.IsProjectRunSupportedUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveAndroidProjectUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveIosProjectUseCase
import io.github.taetae98coding.jarvis.domain.terminal.ObserveProjectKindsUseCase
import io.github.taetae98coding.jarvis.domain.terminal.RunAndroidAppUseCase
import io.github.taetae98coding.jarvis.domain.terminal.RunIosAppUseCase

/**
 * 실행 메뉴가 쓰는 유스케이스 묶음(docs/common/terminal-run.html). [TerminalViewModel] 의 생성자 인자가 Koin `viewModelOf` 의
 * 한도(22개)를 넘지 않게 한 인자로 받는다.
 */
internal class ProjectRunner(
    isProjectRunSupported: IsProjectRunSupportedUseCase,
    val observeProjectKinds: ObserveProjectKindsUseCase,
    val observeAndroidProject: ObserveAndroidProjectUseCase,
    val observeIosProject: ObserveIosProjectUseCase,
    val runAndroidApp: RunAndroidAppUseCase,
    val runIosApp: RunIosAppUseCase,
) {
    val isSupported: Boolean = isProjectRunSupported()
}
