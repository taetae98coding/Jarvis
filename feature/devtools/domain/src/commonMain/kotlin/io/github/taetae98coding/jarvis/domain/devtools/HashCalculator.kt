package io.github.taetae98coding.jarvis.domain.devtools

object HashCalculator {
    /** 빈 입력도 해시한다. 빈 문자열의 해시를 찾는 것도 흔한 쓰임이다. */
    fun convert(input: String): List<DevToolOutput> {
        val bytes = input.encodeToByteArray()

        return listOf(
            DevToolOutput(DevToolOutputKind.SHA256, DevToolValue.Text(Sha256.digest(bytes).toHex())),
            DevToolOutput(DevToolOutputKind.SHA1, DevToolValue.Text(Sha1.digest(bytes).toHex())),
            DevToolOutput(DevToolOutputKind.MD5, DevToolValue.Text(Md5.digest(bytes).toHex())),
        )
    }
}

internal fun ByteArray.toHex(): String = buildString(size * 2) {
    this@toHex.forEach { byte ->
        val code = byte.toInt() and 0xFF
        append(HexDigits[code ushr 4].lowercaseChar())
        append(HexDigits[code and 0x0F].lowercaseChar())
    }
}

/**
 * 메시지 뒤에 `0x80`, 0 들, 비트 길이 8바이트를 붙여 64바이트 배수로 만든다. SHA 는 길이를 빅엔디언으로,
 * MD5 는 리틀엔디언으로 적는다.
 */
private fun pad(message: ByteArray, bigEndianLength: Boolean): ByteArray {
    val paddedSize = ((message.size + 8) / 64 + 1) * 64
    val padded = message.copyOf(paddedSize)
    padded[message.size] = 0x80.toByte()

    val bitLength = message.size.toLong() * 8
    for (i in 0 until 8) {
        val shift = if (bigEndianLength) (7 - i) * 8 else i * 8
        padded[paddedSize - 8 + i] = (bitLength ushr shift).toByte()
    }

    return padded
}

private fun ByteArray.bigEndianInt(offset: Int): Int =
    ((this[offset].toInt() and 0xFF) shl 24) or
        ((this[offset + 1].toInt() and 0xFF) shl 16) or
        ((this[offset + 2].toInt() and 0xFF) shl 8) or
        (this[offset + 3].toInt() and 0xFF)

private fun ByteArray.littleEndianInt(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)

/** 표준 문서의 부호 없는 32비트 상수를 그대로 옮겨 적으려고 Long 으로 받는다. */
private fun words(vararg values: Long): IntArray = IntArray(values.size) { values[it].toInt() }

private fun IntArray.toBigEndianBytes(): ByteArray = ByteArray(size * 4) { (this[it / 4] ushr ((3 - it % 4) * 8)).toByte() }

private fun IntArray.toLittleEndianBytes(): ByteArray = ByteArray(size * 4) { (this[it / 4] ushr ((it % 4) * 8)).toByte() }

/** FIPS 180-4 §6.2. */
internal object Sha256 {
    private val K = words(
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
    )

    fun digest(message: ByteArray): ByteArray {
        val h = words(0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19)
        val padded = pad(message, bigEndianLength = true)
        val w = IntArray(64)

        for (block in padded.indices step 64) {
            for (t in 0 until 16) w[t] = padded.bigEndianInt(block + t * 4)
            for (t in 16 until 64) {
                val s0 = w[t - 15].rotateRight(7) xor w[t - 15].rotateRight(18) xor (w[t - 15] ushr 3)
                val s1 = w[t - 2].rotateRight(17) xor w[t - 2].rotateRight(19) xor (w[t - 2] ushr 10)
                w[t] = w[t - 16] + s0 + w[t - 7] + s1
            }

            var a = h[0]
            var b = h[1]
            var c = h[2]
            var d = h[3]
            var e = h[4]
            var f = h[5]
            var g = h[6]
            var hh = h[7]

            for (t in 0 until 64) {
                val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
                val ch = (e and f) xor (e.inv() and g)
                val t1 = hh + s1 + ch + K[t] + w[t]
                val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val t2 = s0 + maj

                hh = g
                g = f
                f = e
                e = d + t1
                d = c
                c = b
                b = a
                a = t1 + t2
            }

            h[0] += a
            h[1] += b
            h[2] += c
            h[3] += d
            h[4] += e
            h[5] += f
            h[6] += g
            h[7] += hh
        }

        return h.toBigEndianBytes()
    }
}

/** FIPS 180-4 §6.1. */
internal object Sha1 {
    fun digest(message: ByteArray): ByteArray {
        val h = words(0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476, 0xc3d2e1f0)
        val padded = pad(message, bigEndianLength = true)
        val w = IntArray(80)

        for (block in padded.indices step 64) {
            for (t in 0 until 16) w[t] = padded.bigEndianInt(block + t * 4)
            for (t in 16 until 80) w[t] = (w[t - 3] xor w[t - 8] xor w[t - 14] xor w[t - 16]).rotateLeft(1)

            var a = h[0]
            var b = h[1]
            var c = h[2]
            var d = h[3]
            var e = h[4]

            for (t in 0 until 80) {
                val (f, k) = when (t) {
                    in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5a827999
                    in 20..39 -> (b xor c xor d) to 0x6ed9eba1
                    in 40..59 -> ((b and c) or (b and d) or (c and d)) to 0x8f1bbcdc.toInt()
                    else -> (b xor c xor d) to 0xca62c1d6.toInt()
                }
                val temp = a.rotateLeft(5) + f + e + k + w[t]

                e = d
                d = c
                c = b.rotateLeft(30)
                b = a
                a = temp
            }

            h[0] += a
            h[1] += b
            h[2] += c
            h[3] += d
            h[4] += e
        }

        return h.toBigEndianBytes()
    }
}

/** RFC 1321. */
internal object Md5 {
    private val S = intArrayOf(
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
    )

    // RFC 1321 §3.4 의 T[i] = floor(2^32 × |sin(i + 1)|). 부동소수점 sin 은 타깃마다 마지막 자리가 다를 수 있어 상수로 둔다.
    private val T = words(
        0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee, 0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
        0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be, 0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
        0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa, 0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
        0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed, 0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
        0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c, 0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
        0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05, 0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
        0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039, 0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
        0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1, 0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391,
    )

    fun digest(message: ByteArray): ByteArray {
        val h = words(0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476)
        val padded = pad(message, bigEndianLength = false)
        val m = IntArray(16)

        for (block in padded.indices step 64) {
            for (i in 0 until 16) m[i] = padded.littleEndianInt(block + i * 4)

            var a = h[0]
            var b = h[1]
            var c = h[2]
            var d = h[3]

            for (i in 0 until 64) {
                val (f, g) = when (i / 16) {
                    0 -> ((b and c) or (b.inv() and d)) to i
                    1 -> ((d and b) or (d.inv() and c)) to (5 * i + 1) % 16
                    2 -> (b xor c xor d) to (3 * i + 5) % 16
                    else -> (c xor (b or d.inv())) to (7 * i) % 16
                }
                val rotated = (a + f + T[i] + m[g]).rotateLeft(S[i])

                a = d
                d = c
                c = b
                b += rotated
            }

            h[0] += a
            h[1] += b
            h[2] += c
            h[3] += d
        }

        return h.toLittleEndianBytes()
    }
}
