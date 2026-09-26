package io.github.taetae98coding.jarvis.domain.texttools

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** docs/common/text-tools.html R6–R9 */
class PasswordGeneratorTest {
    // 테스트만 결정적인 PRNG 를 쓴다. 앱은 textToolsDataModule 의 플랫폼 CSPRNG 를 쓴다.
    private fun seeded(seed: Int = 42) = PasswordGenerator(Random(seed).let { random -> SecureRandomSource { random.nextInt() } })

    @Test
    fun hasRequestedLengthAndOnlyChosenCharacters() {
        val generator = seeded()
        val options = PasswordOptions(length = 20, uppercase = false, lowercase = true, digits = true, symbols = false)

        repeat(200) {
            val password = generator.generate(options)
            assertEquals(20, password.length)
            assertTrue(password.all { it in 'a'..'z' || it in '0'..'9' }, password)
        }
    }

    @Test
    fun containsEveryChosenClass() {
        val generator = seeded()
        val options = PasswordOptions(length = PasswordOptions.MinLength)

        repeat(500) {
            val password = generator.generate(options)
            CharacterClass.entries.forEach { set ->
                assertTrue(password.any { it in set.characters }, "$password 에 ${set.name} 가 없다")
            }
        }
    }

    @Test
    fun excludesAmbiguousCharacters() {
        val generator = seeded()
        val options = PasswordOptions(length = 64, excludeAmbiguous = true)

        repeat(200) {
            val password = generator.generate(options)
            assertTrue(password.none { it in CharacterClass.AmbiguousCharacters }, password)
        }
    }

    @Test
    fun lengthIsClampedIntoRange() {
        val generator = seeded()

        assertEquals(PasswordOptions.MinLength, generator.generate(PasswordOptions(length = 1)).length)
        assertEquals(PasswordOptions.MaxLength, generator.generate(PasswordOptions(length = 1000)).length)
    }

    @Test
    fun noClassFallsBackToLowercase() {
        val password = seeded().generate(PasswordOptions(uppercase = false, lowercase = false, digits = false, symbols = false))

        assertTrue(password.all { it in 'a'..'z' }, password)
    }

    @Test
    fun rejectionSamplingSkipsTheBiasedTail() {
        // 2^32 mod 3 = 1 이라 가장 큰 값 0xFFFFFFFF(-1) 하나만 버려야 한다.
        val values = ArrayDeque(listOf(-1, 5))
        val generator = PasswordGenerator { values.removeFirst() }

        assertEquals(2, generator.nextIndex(3))
        assertTrue(values.isEmpty())
    }

    @Test
    fun powerOfTwoBoundNeverRejects() {
        val generator = PasswordGenerator { -1 }

        assertEquals(15, generator.nextIndex(16))
    }

    @Test
    fun zeroBoundIsRejected() {
        assertFailsWith<IllegalArgumentException> { seeded().nextIndex(0) }
    }

    @Test
    fun indicesAreUniform() {
        val generator = seeded(7)
        val bound = 89
        val samples = 89_000
        val counts = IntArray(bound)
        repeat(samples) { counts[generator.nextIndex(bound)]++ }

        val expected = samples.toDouble() / bound
        val chiSquare = counts.sumOf { (it - expected) * (it - expected) / expected }
        // 자유도 88 의 카이제곱 분포에서 p = 0.001 의 임계값은 약 135 다.
        assertTrue(chiSquare < 135, "chi² = $chiSquare")
    }

    @Test
    fun entropyAndStrength() {
        val all = PasswordOptions(length = 16)

        assertEquals(89, all.poolSize)
        assertEquals(PasswordStrength.VERY_STRONG, all.strength)
        assertEquals(84, all.copy(excludeAmbiguous = true).poolSize)

        // 소문자 8자: 8 × log2(26) ≈ 37.6
        val weak = PasswordOptions(length = 8, uppercase = false, digits = false, symbols = false)
        assertEquals(PasswordStrength.WEAK, weak.strength)
        // 영문 대소문자·숫자 12자: 12 × log2(62) ≈ 71.4
        val strong = PasswordOptions(length = 12, symbols = false)
        assertEquals(PasswordStrength.STRONG, strong.strength)
        // 숫자 16자: 16 × log2(10) ≈ 53.2
        val fair = PasswordOptions(length = 16, uppercase = false, lowercase = false, symbols = false)
        assertEquals(PasswordStrength.FAIR, fair.strength)
    }

    @Test
    fun strengthBoundaries() {
        assertEquals(PasswordStrength.WEAK, PasswordStrength.of(39.99))
        assertEquals(PasswordStrength.FAIR, PasswordStrength.of(40.0))
        assertEquals(PasswordStrength.STRONG, PasswordStrength.of(60.0))
        assertEquals(PasswordStrength.VERY_STRONG, PasswordStrength.of(80.0))
    }
}
