package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.data.emulator.agent.hostAgentEmulatorDataSource
import io.github.taetae98coding.jarvis.data.emulator.agent.iosHostAgentClient

// iOS 는 서드파티 앱에 프로세스 실행 API 를 주지 않아서 `simctl` 도 `adb` 도 부를 수 없다.
// 시뮬레이터라면 호스트가 곧 이 Mac 이므로, 데스크탑 앱의 에이전트에 물어보면 화면까지 볼 수 있다.
internal actual val emulatorDataSource: EmulatorDataSource =
    hostAgentEmulatorDataSource(iosHostAgentClient)
