package io.github.taetae98coding.jarvis.shared.platform

// 브라우저 샌드박스에는 파일시스템도 프로세스 접근도 없다. 호스트의 SDK 에 닿으려면 짝이 되는
// 서버가 필요해서, 데스크탑 앱이 띄운 에이전트를 그 서버로 쓴다.
internal actual val emulatorProbe: EmulatorProbe = hostAgentEmulatorProbe(::fetchHostAgentEmulators)
