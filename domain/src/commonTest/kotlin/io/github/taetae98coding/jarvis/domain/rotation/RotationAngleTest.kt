package io.github.taetae98coding.jarvis.domain.rotation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RotationAngleTest {
    @Test
    fun rotatingForwardGoesUpNinetyDegrees() {
        assertEquals(RotationAngle.Degrees90, RotationAngle.Degrees0.rotated(1))
        assertEquals(RotationAngle.Degrees270, RotationAngle.Degrees180.rotated(1))
    }

    @Test
    fun rotatingForwardWrapsAroundAtTheTop() {
        assertEquals(RotationAngle.Degrees0, RotationAngle.Degrees270.rotated(1))
    }

    @Test
    fun rotatingBackwardWrapsAroundAtTheBottom() {
        assertEquals(RotationAngle.Degrees270, RotationAngle.Degrees0.rotated(-1))
        assertEquals(RotationAngle.Degrees90, RotationAngle.Degrees180.rotated(-1))
    }

    @Test
    fun degreesThatAreNotAQuarterTurnHaveNoAngle() {
        assertEquals(RotationAngle.Degrees180, RotationAngle.ofDegrees(180))
        assertNull(RotationAngle.ofDegrees(360))
        assertNull(RotationAngle.ofDegrees(45))
    }
}
