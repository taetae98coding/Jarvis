package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.data.emulator.agent.hostAgentEmulatorDataSource
import io.github.taetae98coding.jarvis.data.emulator.agent.webHostAgentClient

// 브라우저 샌드박스에는 파일시스템도 프로세스 접근도 없다. 호스트의 SDK 에 닿으려면 짝이 되는
// 서버가 필요해서, 데스크탑 앱이 띄운 에이전트를 그 서버로 쓴다.
internal actual val emulatorDataSource: EmulatorDataSource =
    hostAgentEmulatorDataSource(webHostAgentClient)
