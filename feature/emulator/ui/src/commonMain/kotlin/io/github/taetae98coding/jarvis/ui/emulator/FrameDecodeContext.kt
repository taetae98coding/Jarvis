package io.github.taetae98coding.jarvis.ui.emulator

import kotlin.coroutines.CoroutineContext

/**
 * 프레임 PNG 를 `ImageBitmap` 으로 푸는 자리. 스레드가 둘 이상인 타깃은 UI 스레드 밖으로 뺀다.
 *
 * Wasm 만 제자리다. 스레드가 하나라 `Dispatchers.Default` 도 같은 이벤트 루프인데, Compose UI 테스트가 그
 * 루프를 점유한 동안 넘긴 디코딩이 끝나지 않는다.
 */
internal expect val FrameDecodeContext: CoroutineContext
