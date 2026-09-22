package io.github.taetae98coding.jarvis.shared.platform

// 기기 샌드박스 안의 앱은 — 그 기기가 세려는 에뮬레이터 자신일 수도 있지만 — 호스트 머신의 SDK
// 도구를 들여다볼 수 없다.
internal actual val emulatorProbe: EmulatorProbe = EmulatorProbe { EmulatorStatus() }
