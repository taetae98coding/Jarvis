package io.github.taetae98coding.jarvis.domain.qrcode

/** GF(2^8), 원시 다항식 x^8 + x^4 + x^3 + x^2 + 1(0x11D), 생성원 α = 2 위의 리드–솔로몬. 바이트는 0..255 의 Int 로 다룬다. */
internal object ReedSolomon {
    /** 차수 [degree] 인 생성 다항식 (x − α^0)(x − α^1)…(x − α^(degree−1)) 의 계수. 최고차 계수 1 은 빼고 높은 차수부터. */
    fun divisor(degree: Int): IntArray {
        require(degree in 1..255)
        val result = IntArray(degree)
        result[degree - 1] = 1
        var root = 1
        repeat(degree) {
            for (j in result.indices) {
                result[j] = multiply(result[j], root)
                if (j + 1 < result.size) result[j] = result[j] xor result[j + 1]
            }
            root = multiply(root, 0x02)
        }
        return result
    }

    fun remainder(data: IntArray, divisor: IntArray): IntArray {
        val result = IntArray(divisor.size)
        for (b in data) {
            val factor = b xor result[0]
            result.copyInto(result, destinationOffset = 0, startIndex = 1)
            result[result.size - 1] = 0
            for (i in result.indices) result[i] = result[i] xor multiply(divisor[i], factor)
        }
        return result
    }

    fun multiply(x: Int, y: Int): Int {
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            z = z xor ((y ushr i) and 1) * x
        }
        return z
    }
}
