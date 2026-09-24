package io.github.taetae98coding.jarvis.domain.terminal

/** 패널이 처음 띄울 프로그램. 끝나면 어느 쪽이든 셸이 남는다. */
enum class TerminalProgram {
    Shell,

    /** Claude Code 를 YOLO 모드(`--dangerously-skip-permissions`)로. */
    Claude,
}
