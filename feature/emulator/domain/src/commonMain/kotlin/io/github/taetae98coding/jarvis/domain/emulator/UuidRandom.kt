package io.github.taetae98coding.jarvis.domain.emulator

import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 네 타깃에서 모두 암호학적으로 안전한 난수. `Random.Default` 는 JVM 에서 ThreadLocalRandom 이라
 * 예측할 수 있고, 같은 네트워크의 누군가가 QR 비밀번호를 맞히면 그쪽이 먼저 기기와 짝을 맺는다.
 * commonMain 에서 안전한 난수에 닿는 표준 경로는 `Uuid.random()` 뿐이다.
 */
@OptIn(ExperimentalUuidApi::class)
internal object UuidRandom : Random() {
    // 하위 64비트의 윗 2비트는 변형 표시라 고정이다. 아래 32비트는 전부 난수다.
    override fun nextBits(bitCount: Int): Int {
        val bits = Uuid.random().toLongs { _, leastSignificantBits -> leastSignificantBits }.toInt()

        return if (bitCount == 0) 0 else bits ushr (Int.SIZE_BITS - bitCount)
    }
}
