package io.github.taetae98coding.jarvis.domain.terminal

/** 창이 띄우는 것. 저장돼서 앱을 다시 켜도 같은 것을 띄운다. 끝나면 어느 쪽이든 셸이 남는다. */
enum class TerminalProgram {
    Shell,

    /** Claude Code 백그라운드 세션을 YOLO 모드(`--dangerously-skip-permissions`)로. */
    Claude,

    /** 셸이 아니라 웹 페이지다. 세션을 열지 않고 화면이 시스템 웹뷰로 [TerminalTab.url] 을 띄운다. */
    Browser,

    /** 셸이 아니라 기기 화면이다. 세션을 열지 않고 화면이 [TerminalTab.deviceId] 기기를 찍어 보인다. */
    Device,
}
