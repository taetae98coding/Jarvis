package io.github.taetae98coding.jarvis.domain.terminal

/** 창이 띄우는 것. 저장돼서 앱을 다시 켜도 같은 것을 띄운다. 끝나면 어느 쪽이든 셸이 남는다. */
enum class TerminalProgram {
    Shell,

    /** Claude Code 백그라운드 세션을 YOLO 모드(`--dangerously-skip-permissions`)로. */
    Claude,
}
