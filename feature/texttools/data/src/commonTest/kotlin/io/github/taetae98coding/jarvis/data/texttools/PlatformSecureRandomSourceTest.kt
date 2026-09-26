package io.github.taetae98coding.jarvis.data.texttools

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * docs/common/text-tools.html R8. 네 타깃의 actual 이 실제로 불리고, 값이 32비트 전체에 퍼지는지 본다.
 * 난수의 질을 재는 시험이 아니라, 바이트를 잘못 이어 붙여 윗자리가 늘 0 인 것 같은 실수를 잡는다.
 */
class PlatformSecureRandomSourceTest {
    @Test
    fun everyBitIsSometimesSetAndSometimesClear() {
        val values = List(256) { PlatformSecureRandomSource.nextInt() }

        for (bit in 0 until Int.SIZE_BITS) {
            val mask = 1 shl bit
            assertTrue(values.any { it and mask != 0 }, "bit $bit 가 한 번도 1 이 아니다")
            assertTrue(values.any { it and mask == 0 }, "bit $bit 가 한 번도 0 이 아니다")
        }
    }

    @Test
    fun valuesAreNotRepeated() {
        val values = List(64) { PlatformSecureRandomSource.nextInt() }

        // 2^32 에서 64개를 뽑아 겹칠 확률은 약 5 × 10^-7 이다.
        assertTrue(values.toSet().size == values.size, values.toString())
    }
}
