package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.data.emulator.agent.androidHostAgentClient
import io.github.taetae98coding.jarvis.data.emulator.agent.hostAgentEmulatorDataSource

// 기기 샌드박스 안의 앱은 — 그 기기가 세려는 에뮬레이터 자신일 수도 있지만 — 호스트 머신의 SDK
// 도구를 직접 들여다볼 수 없다. 그래서 같은 머신의 데스크탑 앱에 물어본다.
internal actual val emulatorDataSource: EmulatorDataSource =
    hostAgentEmulatorDataSource(androidHostAgentClient)
