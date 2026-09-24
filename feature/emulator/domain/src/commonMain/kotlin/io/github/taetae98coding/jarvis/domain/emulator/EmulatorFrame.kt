package io.github.taetae98coding.jarvis.domain.emulator

/** 기기 화면 한 장. `ImageBitmap` 은 Compose 타입이라 여기 둘 수 없어서 바이트로만 다룬다. */
sealed interface EmulatorFrame {
    /** PNG 나 JPEG 한 장. 크기는 풀어 봐야 안다. */
    class Encoded(
        val bytes: ByteArray,
    ) : EmulatorFrame

    /**
     * 디코딩된 픽셀. BGRA 8888, 행 간격은 `width * 4` 다.
     *
     * [pixels] 는 데이터 소스가 돌려 쓰는 버퍼라 몇 프레임 뒤에 덮어써진다. 받는 쪽은 받은 자리에서 곧바로
     * 복사해야 하고, 들고 있다가 나중에 읽으면 다른 장이 보인다. 장마다 새로 할당하지 않는 이유는
     * docs/common/device-mirroring.html#implementation 에 있다.
     */
    class Pixels(
        val width: Int,
        val height: Int,
        val pixels: ByteArray,
    ) : EmulatorFrame
}
