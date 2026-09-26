package io.github.taetae98coding.jarvis.domain.qrcode

internal const val AlphanumericCharset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ \$%*+-./:"

/**
 * 입력 전체를 한 모드로 담는 세그먼트. [length] 는 문자 수 표시자에 적는 값이다(숫자·영숫자는 글자 수, 바이트는 UTF-8 바이트 수).
 *
 * 한 문자열을 모드가 다른 여러 세그먼트로 쪼개면 몇 비트를 더 아낄 수 있지만, 이 앱의 입력(URL·Wi-Fi·연락처)은
 * 대부분 소문자가 섞여 어차피 바이트 모드라 얻는 것이 적어 버렸다.
 */
internal class QrSegment(
    val mode: QrMode,
    val length: Int,
    private val bits: BitBuffer,
) {
    /** 이 버전에서 모드 표시자·문자 수 표시자·데이터를 합친 비트 수. 문자 수가 표시자에 안 들어가면 null. */
    fun totalBits(version: Int): Int? {
        val countBits = mode.charCountBits(version)
        if (length >= 1 shl countBits) return null
        return 4 + countBits + bits.size
    }

    fun appendTo(buffer: BitBuffer, version: Int) {
        buffer.append(mode.modeBits, 4)
        buffer.append(length, mode.charCountBits(version))
        buffer.append(bits)
    }

    companion object {
        fun of(text: String): QrSegment = when {
            text.isNotEmpty() && text.all { it in '0'..'9' } -> numeric(text)
            text.isNotEmpty() && text.all { it in AlphanumericCharset } -> alphanumeric(text)
            else -> bytes(text.encodeToByteArray())
        }

        fun numeric(digits: String): QrSegment {
            val bits = BitBuffer()
            var i = 0
            while (i < digits.length) {
                val n = minOf(digits.length - i, 3)
                bits.append(digits.substring(i, i + n).toInt(), n * 3 + 1)
                i += n
            }
            return QrSegment(QrMode.NUMERIC, digits.length, bits)
        }

        fun alphanumeric(text: String): QrSegment {
            val bits = BitBuffer()
            var i = 0
            while (i + 2 <= text.length) {
                bits.append(AlphanumericCharset.indexOf(text[i]) * 45 + AlphanumericCharset.indexOf(text[i + 1]), 11)
                i += 2
            }
            if (i < text.length) bits.append(AlphanumericCharset.indexOf(text[i]), 6)
            return QrSegment(QrMode.ALPHANUMERIC, text.length, bits)
        }

        fun bytes(data: ByteArray): QrSegment {
            val bits = BitBuffer()
            for (b in data) bits.append(b.toInt() and 0xFF, 8)
            return QrSegment(QrMode.BYTE, data.size, bits)
        }
    }
}

internal class BitBuffer {
    private var bits = BooleanArray(64)

    var size: Int = 0
        private set

    operator fun get(index: Int): Boolean = bits[index]

    fun append(value: Int, length: Int) {
        require(length in 0..31 && value ushr length == 0)
        for (i in length - 1 downTo 0) add((value ushr i) and 1 != 0)
    }

    fun append(other: BitBuffer) {
        for (i in 0 until other.size) add(other[i])
    }

    private fun add(bit: Boolean) {
        if (size == bits.size) bits = bits.copyOf(bits.size * 2)
        bits[size++] = bit
    }
}
