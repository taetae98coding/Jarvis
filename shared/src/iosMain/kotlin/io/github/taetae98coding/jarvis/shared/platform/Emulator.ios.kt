package io.github.taetae98coding.jarvis.shared.platform

// iOS 는 서드파티 앱에 프로세스 실행 API 를 주지 않는다. 앱이 Mac 의 시뮬레이터에서 돌고 있더라도
// `simctl` 과 `adb` 에는 닿을 수 없다.
internal actual val emulatorProbe: EmulatorProbe = EmulatorProbe { EmulatorStatus() }
