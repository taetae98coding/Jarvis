package io.github.taetae98coding.jarvis.domain.texttools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** docs/common/text-tools.html R5 */
class TextLimitTest {
    private val stats = TextCounter.count("가나 다") // 공백 포함 4, 공백 제외 3, 바이트(한글 2) 7

    @Test
    fun basisPicksTheMatchingCount() {
        assertEquals(4, LimitBasis.WITH_SPACES.valueOf(stats))
        assertEquals(3, LimitBasis.WITHOUT_SPACES.valueOf(stats))
        assertEquals(7, LimitBasis.KOREAN_BYTES.valueOf(stats))
    }

    @Test
    fun progressUnderTarget() {
        val progress = TextLimit.of(8, LimitBasis.WITH_SPACES).progress(stats)!!

        assertEquals(4, progress.current)
        assertEquals(0.5f, progress.fraction)
        assertFalse(progress.isOver)
        assertEquals(0, progress.overBy)
    }

    @Test
    fun exactlyTargetIsNotOver() {
        assertFalse(TextLimit.of(4, LimitBasis.WITH_SPACES).progress(stats)!!.isOver)
    }

    @Test
    fun overTargetIsCappedAtFullAndReportsExcess() {
        val progress = TextLimit.of(5, LimitBasis.KOREAN_BYTES).progress(stats)!!

        assertTrue(progress.isOver)
        assertEquals(2, progress.overBy)
        assertEquals(1f, progress.fraction)
    }

    @Test
    fun zeroOrMissingTargetMeansNoLimit() {
        assertNull(TextLimit.of(0, LimitBasis.WITH_SPACES).target)
        assertNull(TextLimit.of(-3, LimitBasis.WITH_SPACES).target)
        assertNull(TextLimit.of(null, LimitBasis.WITHOUT_SPACES).progress(stats))
    }

    @Test
    fun unknownStoredBasisFallsBack() {
        assertEquals(LimitBasis.WITH_SPACES, LimitBasis.fromStored("removed"))
        assertEquals(LimitBasis.KOREAN_BYTES, LimitBasis.fromStored("korean_bytes"))
    }
}
