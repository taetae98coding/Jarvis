package io.github.taetae98coding.jarvis.domain.terminal

/** 기기 탭이 보이는 기기의 플랫폼. emulator 기능의 플랫폼 값과 따로 둔다 — 기능은 서로를 의존하지 않는다. */
enum class DevicePlatform {
    Android,
    IOS,
}

/** 탭 줄에서 탭마다 보이는 종류. [TerminalTab.program] 과 기기 플랫폼에서 나온다. */
enum class TerminalTabKind {
    Terminal,
    Claude,
    Browser,
    Android,
    IOS,

    /** 플랫폼을 모르는 기기 탭. */
    Device,
}
