package io.github.taetae98coding.jarvis.domain.rotation

/**
 * 기기 natural 방향에서 화면이 반시계로 돌아간 각도. Android 의 `Surface.ROTATION_*` 와 같은 기준이다.
 */
enum class RotationAngle(val degrees: Int) {
    Degrees0(0),
    Degrees90(90),
    Degrees180(180),
    Degrees270(270),
    ;

    fun rotated(steps: Int): RotationAngle {
        val size = entries.size

        return entries[((ordinal + steps) % size + size) % size]
    }

    companion object {
        fun ofDegrees(degrees: Int): RotationAngle? = entries.find { it.degrees == degrees }
    }
}
