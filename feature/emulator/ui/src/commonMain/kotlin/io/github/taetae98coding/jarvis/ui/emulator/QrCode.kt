package io.github.taetae98coding.jarvis.ui.emulator

import kotlin.math.abs

/**
 * QR 코드(ISO/IEC 18004) 인코더. 바이트 모드, 버전 1–10 만 한다. 페어링 페이로드는 50바이트
 * 안팎이라 오류 정정 M 에서 버전 4 면 들어가고, 가장 긴 경우를 넉넉히 덮는 데까지만 표를 둔다.
 *
 * 구조는 Project Nayuki 의 QR Code generator(MIT)를 따랐다. 라이브러리를 쓰지 않은 이유는
 * docs/common/wireless-pairing.html 의 결정에 있다.
 */
internal class QrCode private constructor(
    val size: Int,
    private val modules: Array<BooleanArray>,
) {
    /** 어두운 칸인지. [x] 가 열, [y] 가 행이다. */
    operator fun get(x: Int, y: Int): Boolean = modules[y][x]

    enum class Ecc(val formatBits: Int) {
        L(1),
        M(0),
        Q(3),
        H(2),
    }

    companion object {
        fun encode(text: String, ecc: Ecc = Ecc.M): QrCode {
            val data = text.encodeToByteArray()
            val version = (MinVersion..MaxVersion).firstOrNull { version ->
                byteModeHeaderBits(version) + data.size * 8 <= dataCodewordCount(version, ecc) * 8
            } ?: throw IllegalArgumentException("QR 버전 $MaxVersion 에 들어가지 않는다: ${data.size}바이트")

            return Builder(version, ecc).build(dataCodewords(data, version, ecc))
        }

        private const val MinVersion = 1
        private const val MaxVersion = 10
    }

    private class Builder(
        private val version: Int,
        private val ecc: Ecc,
    ) {
        val size = version * 4 + 17
        private val modules = Array(size) { BooleanArray(size) }
        private val isFunction = Array(size) { BooleanArray(size) }

        fun build(dataCodewords: ByteArray): QrCode {
            drawFunctionPatterns()
            drawCodewords(addEccAndInterleave(dataCodewords))

            // 마스크는 어느 것이든 규격에 맞는다. 벌점이 가장 낮은 것을 고르면 스캐너가 덜 헷갈린다.
            val mask = (0 until 8).minBy { mask ->
                applyMask(mask)
                drawFormatBits(mask)
                penaltyScore().also { applyMask(mask) }
            }
            applyMask(mask)
            drawFormatBits(mask)

            return QrCode(size, modules)
        }

        private fun drawFunctionPatterns() {
            for (i in 0 until size) {
                setFunction(6, i, i % 2 == 0)
                setFunction(i, 6, i % 2 == 0)
            }

            drawFinder(3, 3)
            drawFinder(size - 4, 3)
            drawFinder(3, size - 4)

            val positions = alignmentPositions()
            val last = positions.lastIndex
            for (i in positions.indices) {
                for (j in positions.indices) {
                    // 찾기 패턴과 겹치는 세 모서리는 건너뛴다.
                    if (!(i == 0 && j == 0 || i == 0 && j == last || i == last && j == 0)) {
                        drawAlignment(positions[i], positions[j])
                    }
                }
            }

            // 마스크를 고르기 전에 자리만 잡아 둔다. 실제 값은 build 가 다시 그린다.
            drawFormatBits(0)
            drawVersion()
        }

        private fun drawFinder(x: Int, y: Int) {
            for (dy in -4..4) {
                for (dx in -4..4) {
                    val distance = maxOf(abs(dx), abs(dy))
                    val xx = x + dx
                    val yy = y + dy

                    if (xx in 0 until size && yy in 0 until size) setFunction(xx, yy, distance != 2 && distance != 4)
                }
            }
        }

        private fun drawAlignment(x: Int, y: Int) {
            for (dy in -2..2) {
                for (dx in -2..2) {
                    setFunction(x + dx, y + dy, maxOf(abs(dx), abs(dy)) != 1)
                }
            }
        }

        private fun alignmentPositions(): IntArray {
            if (version == 1) return IntArray(0)

            val count = version / 7 + 2
            val step = (version * 8 + count * 3 + 5) / (count * 4 - 4) * 2
            val result = IntArray(count)
            result[0] = 6

            var position = size - 7
            for (i in count - 1 downTo 1) {
                result[i] = position
                position -= step
            }

            return result
        }

        private fun drawFormatBits(mask: Int) {
            val data = ecc.formatBits shl 3 or mask
            var remainder = data
            repeat(10) { remainder = (remainder shl 1) xor ((remainder ushr 9) * 0x537) }
            val bits = (data shl 10 or remainder) xor 0x5412

            for (i in 0..5) setFunction(8, i, bits.bit(i))
            setFunction(8, 7, bits.bit(6))
            setFunction(8, 8, bits.bit(7))
            setFunction(7, 8, bits.bit(8))
            for (i in 9 until 15) setFunction(14 - i, 8, bits.bit(i))

            for (i in 0 until 8) setFunction(size - 1 - i, 8, bits.bit(i))
            for (i in 8 until 15) setFunction(8, size - 15 + i, bits.bit(i))
            // 규격이 늘 어둡게 두는 칸이다.
            setFunction(8, size - 8, true)
        }

        private fun drawVersion() {
            if (version < 7) return

            var remainder = version
            repeat(12) { remainder = (remainder shl 1) xor ((remainder ushr 11) * 0x1F25) }
            val bits = version shl 12 or remainder

            for (i in 0 until 18) {
                val a = size - 11 + i % 3
                val b = i / 3
                setFunction(a, b, bits.bit(i))
                setFunction(b, a, bits.bit(i))
            }
        }

        private fun addEccAndInterleave(data: ByteArray): ByteArray {
            val blockCount = EccBlockCount[ecc.ordinal][version - 1]
            val eccLength = EccCodewordsPerBlock[ecc.ordinal][version - 1]
            val rawCodewords = rawDataModuleCount(version) / 8
            val shortBlockCount = blockCount - rawCodewords % blockCount
            val shortBlockLength = rawCodewords / blockCount
            val divisor = reedSolomonDivisor(eccLength)

            var offset = 0
            val blocks = List(blockCount) { i ->
                val dataLength = shortBlockLength - eccLength + if (i < shortBlockCount) 0 else 1
                val blockData = data.copyOfRange(offset, offset + dataLength)
                offset += dataLength

                // 짧은 블록도 긴 블록 길이로 맞춰 두고, 끼워 넣을 때 빈 자리를 건너뛴다.
                blockData.copyOf(shortBlockLength + 1).also { block ->
                    reedSolomonRemainder(blockData, divisor).copyInto(block, block.size - eccLength)
                }
            }

            val result = ByteArray(rawCodewords)
            var k = 0
            for (i in 0..shortBlockLength) {
                for (j in blocks.indices) {
                    if (i != shortBlockLength - eccLength || j >= shortBlockCount) result[k++] = blocks[j][i]
                }
            }

            return result
        }

        private fun drawCodewords(codewords: ByteArray) {
            var i = 0
            var right = size - 1

            while (right >= 1) {
                // 세로 타이밍 패턴 열은 건너뛴다.
                if (right == 6) right = 5

                for (vertical in 0 until size) {
                    for (j in 0..1) {
                        val x = right - j
                        val upward = (right + 1) and 2 == 0
                        val y = if (upward) size - 1 - vertical else vertical

                        if (!isFunction[y][x] && i < codewords.size * 8) {
                            modules[y][x] = (codewords[i ushr 3].toInt() and 0xFF).bit(7 - (i and 7))
                            i++
                        }
                    }
                }

                right -= 2
            }
        }

        private fun applyMask(mask: Int) {
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val invert = when (mask) {
                        0 -> (x + y) % 2 == 0
                        1 -> y % 2 == 0
                        2 -> x % 3 == 0
                        3 -> (x + y) % 3 == 0
                        4 -> (x / 3 + y / 2) % 2 == 0
                        5 -> x * y % 2 + x * y % 3 == 0
                        6 -> (x * y % 2 + x * y % 3) % 2 == 0
                        else -> ((x + y) % 2 + x * y % 3) % 2 == 0
                    }

                    if (invert && !isFunction[y][x]) modules[y][x] = !modules[y][x]
                }
            }
        }

        private fun penaltyScore(): Int {
            var result = 0

            for (y in 0 until size) result += linePenalty { modules[y][it] }
            for (x in 0 until size) result += linePenalty { modules[it][x] }

            for (y in 0 until size - 1) {
                for (x in 0 until size - 1) {
                    val color = modules[y][x]

                    if (color == modules[y][x + 1] && color == modules[y + 1][x] && color == modules[y + 1][x + 1]) {
                        result += PenaltyBlock
                    }
                }
            }

            val dark = modules.sumOf { row -> row.count { it } }
            val total = size * size
            val k = (abs(dark * 20 - total * 10) + total - 1) / total - 1
            result += k * PenaltyBalance

            return result
        }

        // 같은 색이 5칸 넘게 이어지는 것과, 찾기 패턴처럼 보이는 1:1:3:1:1 을 센다.
        private fun linePenalty(module: (Int) -> Boolean): Int {
            var result = 0
            var runColor = false
            var runLength = 0
            val history = IntArray(7)

            for (i in 0 until size) {
                if (module(i) == runColor) {
                    runLength++
                    if (runLength == 5) result += PenaltyRun else if (runLength > 5) result++
                } else {
                    addHistory(runLength, history)
                    if (!runColor) result += countFinderLike(history) * PenaltyFinderLike
                    runColor = module(i)
                    runLength = 1
                }
            }

            if (runColor) {
                addHistory(runLength, history)
                runLength = 0
            }
            addHistory(runLength + size, history)

            return result + countFinderLike(history) * PenaltyFinderLike
        }

        private fun addHistory(runLength: Int, history: IntArray) {
            // 줄 앞의 여백을 밝은 칸으로 친다.
            val length = if (history[0] == 0) runLength + size else runLength
            history.copyInto(history, destinationOffset = 1, startIndex = 0, endIndex = history.size - 1)
            history[0] = length
        }

        private fun countFinderLike(history: IntArray): Int {
            val n = history[1]
            val core = n > 0 && history[2] == n && history[3] == n * 3 && history[4] == n && history[5] == n

            return (if (core && history[0] >= n * 4 && history[6] >= n) 1 else 0) +
                (if (core && history[6] >= n * 4 && history[0] >= n) 1 else 0)
        }

        private fun setFunction(x: Int, y: Int, dark: Boolean) {
            modules[y][x] = dark
            isFunction[y][x] = true
        }
    }
}

private fun Int.bit(index: Int): Boolean = (this ushr index) and 1 != 0

// 모드 표시 4비트 + 글자 수. 글자 수 칸은 버전 1–9 에서 8비트, 10–26 에서 16비트다.
private fun byteModeHeaderBits(version: Int): Int = 4 + if (version <= 9) 8 else 16

private fun dataCodewords(data: ByteArray, version: Int, ecc: QrCode.Ecc): ByteArray {
    val capacityBits = dataCodewordCount(version, ecc) * 8
    val bits = ArrayList<Boolean>(capacityBits)

    fun append(value: Int, length: Int) {
        for (i in length - 1 downTo 0) bits += value.bit(i)
    }

    append(0b0100, 4)
    append(data.size, byteModeHeaderBits(version) - 4)
    data.forEach { append(it.toInt() and 0xFF, 8) }

    append(0, minOf(4, capacityBits - bits.size))
    append(0, (8 - bits.size % 8) % 8)

    // 남는 자리는 규격이 정한 두 바이트를 번갈아 채운다.
    var pad = 0xEC
    while (bits.size < capacityBits) {
        append(pad, 8)
        pad = pad xor 0xEC xor 0x11
    }

    return ByteArray(capacityBits / 8) { i ->
        (0 until 8).fold(0) { acc, j -> acc shl 1 or if (bits[i * 8 + j]) 1 else 0 }.toByte()
    }
}

private fun dataCodewordCount(version: Int, ecc: QrCode.Ecc): Int =
    rawDataModuleCount(version) / 8 -
        EccCodewordsPerBlock[ecc.ordinal][version - 1] * EccBlockCount[ecc.ordinal][version - 1]

// 기능 패턴을 뺀, 데이터와 오류 정정이 들어갈 칸 수.
private fun rawDataModuleCount(version: Int): Int {
    var result = (16 * version + 128) * version + 64

    if (version >= 2) {
        val alignmentCount = version / 7 + 2
        result -= (25 * alignmentCount - 10) * alignmentCount - 55
        if (version >= 7) result -= 36
    }

    return result
}

private fun reedSolomonDivisor(degree: Int): IntArray {
    val result = IntArray(degree)
    result[degree - 1] = 1

    var root = 1
    for (i in 0 until degree) {
        for (j in 0 until degree) {
            result[j] = gfMultiply(result[j], root)
            if (j + 1 < degree) result[j] = result[j] xor result[j + 1]
        }
        root = gfMultiply(root, 0x02)
    }

    return result
}

private fun reedSolomonRemainder(data: ByteArray, divisor: IntArray): ByteArray {
    val result = IntArray(divisor.size)

    for (byte in data) {
        val factor = (byte.toInt() and 0xFF) xor result[0]
        result.copyInto(result, destinationOffset = 0, startIndex = 1)
        result[result.lastIndex] = 0
        for (i in result.indices) result[i] = result[i] xor gfMultiply(divisor[i], factor)
    }

    return ByteArray(result.size) { result[it].toByte() }
}

// GF(2^8), 원시 다항식 x^8 + x^4 + x^3 + x^2 + 1.
private fun gfMultiply(x: Int, y: Int): Int {
    var z = 0
    for (i in 7 downTo 0) {
        z = (z shl 1) xor ((z ushr 7) * 0x11D)
        z = z xor (((y ushr i) and 1) * x)
    }

    return z
}

private const val PenaltyRun = 3
private const val PenaltyBlock = 3
private const val PenaltyFinderLike = 40
private const val PenaltyBalance = 10

// 규격 표 13–22 의 버전 1–10 부분. 행 순서는 Ecc 의 선언 순서(L, M, Q, H)다.
private val EccCodewordsPerBlock = arrayOf(
    intArrayOf(7, 10, 15, 20, 26, 18, 20, 24, 30, 18),
    intArrayOf(10, 16, 26, 18, 24, 16, 18, 22, 22, 26),
    intArrayOf(13, 22, 18, 26, 18, 24, 18, 22, 20, 24),
    intArrayOf(17, 28, 22, 16, 22, 28, 26, 26, 24, 28),
)

private val EccBlockCount = arrayOf(
    intArrayOf(1, 1, 1, 1, 1, 2, 2, 2, 2, 4),
    intArrayOf(1, 1, 1, 2, 2, 4, 4, 4, 5, 5),
    intArrayOf(1, 1, 2, 2, 4, 4, 6, 6, 8, 8),
    intArrayOf(1, 1, 2, 4, 4, 4, 5, 6, 8, 8),
)
