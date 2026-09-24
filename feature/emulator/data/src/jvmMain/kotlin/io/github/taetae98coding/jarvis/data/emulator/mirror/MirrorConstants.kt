package io.github.taetae98coding.jarvis.data.emulator.mirror

import kotlin.time.Duration.Companion.seconds

// scrcpy-server 아티팩트 버전. 서버는 첫 인자로 받은 버전이 자기 BuildConfig.VERSION_NAME 과 다르면
// 시작을 거부하므로, jvmMain/resources/scrcpy/scrcpy-server-v<이 값> 을 바꿀 때 함께 바꾼다.
internal const val ScrcpyServerVersion = "4.1"

// 영상의 긴 변 최대 픽셀. 기기 디스플레이가 더 크면 서버가 여기에 맞춰 축소한다.
internal const val MirrorMaxSize = 2048
internal const val MirrorMaxFps = 60
internal const val MirrorBitRate = 8_000_000

// 마지막 구독자가 떠난 뒤 세션을 얼마나 살려 두는지. 로컬 에이전트가 100ms 마다 붙고 떠나므로 여유가
// 없으면 요청마다 서버를 올리고 내린다(docs/common/device-mirroring.html#implementation).
internal val MirrorSessionLinger = 3.seconds

// 스트림이 끊긴 뒤 처음부터 다시 시도하기까지.
internal val MirrorRetryDelay = 2.seconds

// 영상 소켓이 서버의 더미 바이트를 읽을 때까지 다시 연결하는 상한.
internal val MirrorConnectTimeout = 5.seconds
