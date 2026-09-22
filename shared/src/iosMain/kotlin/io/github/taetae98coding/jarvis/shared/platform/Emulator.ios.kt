package io.github.taetae98coding.jarvis.shared.platform

// iOS 는 서드파티 앱에 프로세스 실행 API 를 주지 않아서 `simctl` 도 `adb` 도 부를 수 없다.
// 시뮬레이터라면 호스트가 곧 이 Mac 이므로, 데스크탑 앱의 에이전트에 물어보면 숫자를 알 수 있다.
internal actual val emulatorProbe: EmulatorProbe = hostAgentEmulatorProbe(::fetchHostAgentEmulators)
