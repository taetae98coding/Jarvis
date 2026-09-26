package io.github.taetae98coding.jarvis.domain.qrcode

import kotlin.math.abs

/** 기능 패턴을 놓고 코드워드를 채운 뒤 마스크를 고른다. 한 번 [build] 하고 버린다. */
internal class QrSymbolBuilder(
    private val version: Int,
    private val errorCorrection: QrErrorCorrection,
) {
    val size = version * 4 + 17
    private val modules = BooleanArray(size * size)
    private val isFunction = BooleanArray(size * size)

    fun build(dataCodewords: IntArray, mask: Int?, mode: QrMode): QrCode {
        drawFunctionPatterns()
        drawCodewords(QrEncoder.addEccAndInterleave(dataCodewords, version, errorCorrection))

        val chosen = mask ?: (0..7).minBy { candidate ->
            applyMask(candidate)
            drawFormatBits(candidate)
            val penalty = penaltyScore()
            // 마스크는 XOR 이라 한 번 더 걸면 원래대로 돌아온다.
            applyMask(candidate)
            penalty
        }
        applyMask(chosen)
        drawFormatBits(chosen)

        return QrCode(version, errorCorrection, chosen, mode, QrMatrix(size, modules.copyOf()))
    }

    private fun get(x: Int, y: Int): Boolean = modules[y * size + x]

    private fun setFunction(x: Int, y: Int, dark: Boolean) {
        modules[y * size + x] = dark
        isFunction[y * size + x] = true
    }

    private fun drawFunctionPatterns() {
        for (i in 0 until size) {
            setFunction(6, i, i % 2 == 0)
            setFunction(i, 6, i % 2 == 0)
        }

        drawFinderPattern(3, 3)
        drawFinderPattern(size - 4, 3)
        drawFinderPattern(3, size - 4)

        val positions = QrTables.alignmentPatternPositions(version)
        val last = positions.size - 1
        for (i in positions.indices) {
            for (j in positions.indices) {
                // 세 모서리는 파인더 패턴과 겹친다.
                val overlapsFinder = (i == 0 && j == 0) || (i == 0 && j == last) || (i == last && j == 0)
                if (!overlapsFinder) drawAlignmentPattern(positions[i], positions[j])
            }
        }

        // 자리를 기능 모듈로 잡아 두려는 임시 값이다. 마스크를 고른 뒤 진짜 값으로 덮는다.
        drawFormatBits(0)
        drawVersion()
    }

    private fun drawFormatBits(mask: Int) {
        val bits = formatBits(errorCorrection, mask)

        for (i in 0..5) setFunction(8, i, bit(bits, i))
        setFunction(8, 7, bit(bits, 6))
        setFunction(8, 8, bit(bits, 7))
        setFunction(7, 8, bit(bits, 8))
        for (i in 9 until 15) setFunction(14 - i, 8, bit(bits, i))

        for (i in 0 until 8) setFunction(size - 1 - i, 8, bit(bits, i))
        for (i in 8 until 15) setFunction(8, size - 15 + i, bit(bits, i))
        // 늘 어두운 모듈(ISO/IEC 18004 7.9.1).
        setFunction(8, size - 8, true)
    }

    private fun drawVersion() {
        if (version < 7) return
        val bits = versionBits(version)
        for (i in 0 until 18) {
            val dark = bit(bits, i)
            val a = size - 11 + i % 3
            val b = i / 3
            setFunction(a, b, dark)
            setFunction(b, a, dark)
        }
    }

    private fun drawFinderPattern(x: Int, y: Int) {
        for (dy in -4..4) {
            for (dx in -4..4) {
                val distance = maxOf(abs(dx), abs(dy))
                val xx = x + dx
                val yy = y + dy
                if (xx in 0 until size && yy in 0 until size) setFunction(xx, yy, distance != 2 && distance != 4)
            }
        }
    }

    private fun drawAlignmentPattern(x: Int, y: Int) {
        for (dy in -2..2) {
            for (dx in -2..2) setFunction(x + dx, y + dy, maxOf(abs(dx), abs(dy)) != 1)
        }
    }

    /** 오른쪽 아래에서 시작해 두 열씩 위아래로 지그재그로 채운다. 세로 타이밍 패턴(x = 6) 열은 건너뛴다. */
    private fun drawCodewords(codewords: IntArray) {
        var i = 0
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vertical in 0 until size) {
                for (j in 0..1) {
                    val x = right - j
                    val upward = ((right + 1) and 2) == 0
                    val y = if (upward) size - 1 - vertical else vertical
                    if (!isFunction[y * size + x] && i < codewords.size * 8) {
                        modules[y * size + x] = (codewords[i ushr 3] ushr (7 - (i and 7))) and 1 != 0
                        i++
                    }
                    // 남는 비트(0–7개)는 밝은 모듈로 둔다. 배열이 이미 false 로 차 있다.
                }
            }
            right -= 2
        }
    }

    private fun applyMask(mask: Int) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                val index = y * size + x
                if (!isFunction[index] && maskCondition(mask, x, y)) modules[index] = !modules[index]
            }
        }
    }

    /** ISO/IEC 18004 7.8.3 의 N1–N4 벌점. */
    private fun penaltyScore(): Int {
        var result = 0

        for (y in 0 until size) {
            result += linePenalty { get(it, y) }
        }
        for (x in 0 until size) {
            result += linePenalty { get(x, it) }
        }

        for (y in 0 until size - 1) {
            for (x in 0 until size - 1) {
                val color = get(x, y)
                if (color == get(x + 1, y) && color == get(x, y + 1) && color == get(x + 1, y + 1)) result += PenaltyN2
            }
        }

        val dark = modules.count { it }
        val total = size * size
        // 어두운 비율이 50% 에서 5% 벗어날 때마다 N4. 정수로만 계산해 타깃마다 결과가 같다.
        val k = (abs(dark * 20 - total * 10) + total - 1) / total - 1
        result += k * PenaltyN4
        return result
    }

    /** 한 줄의 같은 색 연속(N1)과 1:1:3:1:1 파인더 닮은꼴(N3). 줄 밖은 밝은 모듈로 본다. */
    private inline fun linePenalty(module: (Int) -> Boolean): Int {
        var result = 0
        var runColor = false
        var runLength = 0
        val history = IntArray(7)
        for (i in 0 until size) {
            if (module(i) == runColor) {
                runLength++
                if (runLength == 5) result += PenaltyN1 else if (runLength > 5) result++
            } else {
                addHistory(runLength, history)
                if (!runColor) result += countFinderLike(history) * PenaltyN3
                runColor = module(i)
                runLength = 1
            }
        }
        if (runColor) {
            addHistory(runLength, history)
            runLength = 0
        }
        addHistory(runLength + size, history)
        result += countFinderLike(history) * PenaltyN3
        return result
    }

    private fun addHistory(runLength: Int, history: IntArray) {
        // 줄 첫 연속 앞에는 밝은 조용한 영역이 있다고 본다.
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

    companion object {
        private const val PenaltyN1 = 3
        private const val PenaltyN2 = 3
        private const val PenaltyN3 = 40
        private const val PenaltyN4 = 10

        /** 15비트 형식 정보: (오류 정정 2비트 + 마스크 3비트) + BCH(15,5) 나머지 10비트, 0x5412 로 XOR. */
        fun formatBits(errorCorrection: QrErrorCorrection, mask: Int): Int {
            val data = errorCorrection.formatBits shl 3 or mask
            var remainder = data
            repeat(10) { remainder = (remainder shl 1) xor ((remainder ushr 9) * 0x537) }
            return ((data shl 10) or remainder) xor 0x5412
        }

        /** 18비트 버전 정보: 버전 6비트 + BCH(18,6) 나머지 12비트. */
        fun versionBits(version: Int): Int {
            var remainder = version
            repeat(12) { remainder = (remainder shl 1) xor ((remainder ushr 11) * 0x1F25) }
            return (version shl 12) or remainder
        }

        fun maskCondition(mask: Int, x: Int, y: Int): Boolean = when (mask) {
            0 -> (x + y) % 2 == 0
            1 -> y % 2 == 0
            2 -> x % 3 == 0
            3 -> (x + y) % 3 == 0
            4 -> (x / 3 + y / 2) % 2 == 0
            5 -> x * y % 2 + x * y % 3 == 0
            6 -> (x * y % 2 + x * y % 3) % 2 == 0
            7 -> ((x + y) % 2 + x * y % 3) % 2 == 0
            else -> error("mask $mask")
        }

        private fun bit(value: Int, index: Int): Boolean = (value ushr index) and 1 != 0
    }
}
