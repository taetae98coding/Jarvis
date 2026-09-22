package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.coroutines.flow.flowOf

// 브라우저 샌드박스에는 파일시스템도 프로세스 접근도 없다. 호스트의 SDK 에 닿으려면 짝이 되는
// 서버가 필요한데 이 앱에는 없다.
internal actual val emulatorProbe: EmulatorProbe = EmulatorProbe { flowOf(EmulatorStatus()) }
