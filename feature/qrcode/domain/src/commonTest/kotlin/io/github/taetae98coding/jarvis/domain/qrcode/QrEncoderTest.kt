package io.github.taetae98coding.jarvis.domain.qrcode

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** docs/common/qr-code.html#verification */
class QrEncoderTest {
    @Test
    fun helloWorldQuartileDataCodewords() {
        // thonky.com QR Code Tutorial 의 "HELLO WORLD" 1-Q 예제.
        val segment = QrSegment.of("HELLO WORLD")
        assertEquals(QrMode.ALPHANUMERIC, segment.mode)

        assertContentEquals(
            intArrayOf(32, 91, 11, 120, 209, 114, 220, 77, 67, 64, 236, 17, 236),
            QrEncoder.dataCodewords(segment, version = 1, errorCorrection = QrErrorCorrection.Q),
        )
    }

    @Test
    fun reedSolomonMatchesHelloWorldVector() {
        val data = intArrayOf(32, 91, 11, 120, 209, 114, 220, 77, 67, 64, 236, 17, 236)

        assertContentEquals(
            intArrayOf(168, 72, 22, 82, 217, 54, 156, 0, 46, 15, 180, 122, 16),
            ReedSolomon.remainder(data, ReedSolomon.divisor(13)),
        )
    }

    @Test
    fun reedSolomonMatchesIsoAnnexNumericVector() {
        // ISO/IEC 18004 부록 I 의 "01234567" 1-M 예제.
        val segment = QrSegment.of("01234567")
        val data = QrEncoder.dataCodewords(segment, version = 1, errorCorrection = QrErrorCorrection.M)

        assertContentEquals(intArrayOf(16, 32, 12, 86, 97, 128, 236, 17, 236, 17, 236, 17, 236, 17, 236, 17), data)
        assertContentEquals(
            intArrayOf(165, 36, 212, 193, 237, 54, 199, 135, 44, 85),
            ReedSolomon.remainder(data, ReedSolomon.divisor(10)),
        )
    }

    @Test
    fun galoisFieldMultiplication() {
        assertEquals(0, ReedSolomon.multiply(0, 0x53))
        assertEquals(0x53, ReedSolomon.multiply(1, 0x53))
        // α^8 = α^4 + α^3 + α^2 + 1 = 0x1D.
        assertEquals(0x1D, ReedSolomon.multiply(0x80, 0x02))
        for (x in 1..255) assertEquals(ReedSolomon.multiply(x, 0x35), ReedSolomon.multiply(0x35, x))
    }

    @Test
    fun formatBitsMatchIsoTable() {
        // ISO/IEC 18004 표 C.1 의 마스크 적용 후 형식 정보, 단계마다 여덟 마스크.
        val expected = mapOf(
            QrErrorCorrection.L to listOf(
                "111011111000100", "111001011110011", "111110110101010", "111100010011101",
                "110011000101111", "110001100011000", "110110001000001", "110100101110110",
            ),
            QrErrorCorrection.M to listOf(
                "101010000010010", "101000100100101", "101111001111100", "101101101001011",
                "100010111111001", "100000011001110", "100111110010111", "100101010100000",
            ),
            QrErrorCorrection.Q to listOf(
                "011010101011111", "011000001101000", "011111100110001", "011101000000110",
                "010010010110100", "010000110000011", "010111011011010", "010101111101101",
            ),
            QrErrorCorrection.H to listOf(
                "001011010001001", "001001110111110", "001110011100111", "001100111010000",
                "000011101100010", "000001001010101", "000110100001100", "000100000111011",
            ),
        )
        for ((ecc, masks) in expected) {
            masks.forEachIndexed { mask, bits ->
                assertEquals(bits.toInt(2), QrSymbolBuilder.formatBits(ecc, mask), "$ecc mask $mask")
            }
        }
    }

    @Test
    fun versionBitsMatchIsoTable() {
        assertEquals(0x07C94, QrSymbolBuilder.versionBits(7))
        assertEquals(0x085BC, QrSymbolBuilder.versionBits(8))
        assertEquals(0x1B08E, QrSymbolBuilder.versionBits(27))
        assertEquals(0x28C69, QrSymbolBuilder.versionBits(40))
    }

    @Test
    fun codewordCountsMatchIsoTable() {
        assertEquals(26, QrTables.rawDataModules(1) / 8)
        assertEquals(44, QrTables.rawDataModules(2) / 8)
        assertEquals(196, QrTables.rawDataModules(7) / 8)
        assertEquals(3706, QrTables.rawDataModules(40) / 8)

        assertEquals(listOf(19, 16, 13, 9), QrErrorCorrection.entries.map { QrTables.dataCodewords(1, it) })
        assertEquals(listOf(274, 216, 154, 122), QrErrorCorrection.entries.map { QrTables.dataCodewords(10, it) })
        assertEquals(listOf(2956, 2334, 1666, 1276), QrErrorCorrection.entries.map { QrTables.dataCodewords(40, it) })

        // 모든 버전·단계에서 블록 구성이 원시 코드워드 수와 맞아떨어진다.
        for (version in 1..40) {
            for (ecc in QrErrorCorrection.entries) {
                val blocks = QrTables.errorCorrectionBlocks(version, ecc)
                val raw = QrTables.rawDataModules(version) / 8
                assertTrue(raw / blocks > QrTables.eccCodewordsPerBlock(version, ecc), "$version-$ecc")
            }
        }
    }

    @Test
    fun alignmentPatternPositions() {
        assertContentEquals(intArrayOf(), QrTables.alignmentPatternPositions(1))
        assertContentEquals(intArrayOf(6, 18), QrTables.alignmentPatternPositions(2))
        assertContentEquals(intArrayOf(6, 22, 38), QrTables.alignmentPatternPositions(7))
        assertContentEquals(intArrayOf(6, 26, 48, 70), QrTables.alignmentPatternPositions(15))
        assertContentEquals(intArrayOf(6, 34, 60, 86, 112, 138), QrTables.alignmentPatternPositions(32))
        assertContentEquals(intArrayOf(6, 30, 58, 86, 114, 142, 170), QrTables.alignmentPatternPositions(40))
    }

    @Test
    fun maxLengthsMatchIsoCapacityTable() {
        assertEquals(listOf(7089, 5596, 3993, 3057), QrErrorCorrection.entries.map { QrEncoder.maxLength(QrMode.NUMERIC, it) })
        assertEquals(listOf(4296, 3391, 2420, 1852), QrErrorCorrection.entries.map { QrEncoder.maxLength(QrMode.ALPHANUMERIC, it) })
        assertEquals(listOf(2953, 2331, 1663, 1273), QrErrorCorrection.entries.map { QrEncoder.maxLength(QrMode.BYTE, it) })
    }

    @Test
    fun versionSelectionBoundaries() {
        // 버전 1 의 바이트 모드 한도: L 17, H 7. 한 바이트 넘으면 버전 2.
        assertEquals(1, version("a".repeat(17), QrErrorCorrection.L))
        assertEquals(2, version("a".repeat(18), QrErrorCorrection.L))
        assertEquals(1, version("a".repeat(7), QrErrorCorrection.H))
        assertEquals(2, version("a".repeat(8), QrErrorCorrection.H))
        // 숫자 41자, 영숫자 25자가 1-L 에 들어간다.
        assertEquals(1, version("1".repeat(41), QrErrorCorrection.L))
        assertEquals(2, version("1".repeat(42), QrErrorCorrection.L))
        assertEquals(1, version("A".repeat(25), QrErrorCorrection.L))
        assertEquals(2, version("A".repeat(26), QrErrorCorrection.L))
        // 버전 9 → 10 에서 바이트 모드 문자 수 표시자가 8 → 16 비트로 늘어난다. 9-L 한도는 230 바이트.
        assertEquals(9, version("a".repeat(230), QrErrorCorrection.L))
        assertEquals(10, version("a".repeat(231), QrErrorCorrection.L))
        assertEquals(40, version("a".repeat(2953), QrErrorCorrection.L))
    }

    @Test
    fun tooLongReportsLimit() {
        val result = QrEncoder.encode("a".repeat(1274), QrErrorCorrection.H)

        assertEquals(QrEncodeResult.TooLong(QrMode.BYTE, limit = 1273, length = 1274), result)
    }

    @Test
    fun utf8IsCountedInBytes() {
        // 한글 한 글자는 UTF-8 세 바이트다. 1-L 에는 17 바이트, 즉 다섯 글자까지 들어간다.
        assertEquals(1, version("가나다라마", QrErrorCorrection.L))
        assertEquals(2, version("가나다라마바", QrErrorCorrection.L))
        assertEquals(QrMode.BYTE, encode("가", QrErrorCorrection.L).mode)
    }

    @Test
    fun functionPatternsAreInPlace() {
        val code = encode("https://example.com", QrErrorCorrection.M)
        val matrix = code.matrix
        assertEquals(code.version * 4 + 17, matrix.size)

        // 파인더 패턴 세 개: 7×7 테두리와 3×3 가운데.
        for ((ox, oy) in listOf(0 to 0, matrix.size - 7 to 0, 0 to matrix.size - 7)) {
            for (i in 0 until 7) {
                assertTrue(matrix.isDark(ox + i, oy) && matrix.isDark(ox + i, oy + 6) && matrix.isDark(ox, oy + i) && matrix.isDark(ox + 6, oy + i))
            }
            assertTrue(!matrix.isDark(ox + 1, oy + 1) && matrix.isDark(ox + 3, oy + 3))
        }
        // 타이밍 패턴과 늘 어두운 모듈.
        for (i in 8 until matrix.size - 8) {
            assertEquals(i % 2 == 0, matrix.isDark(i, 6))
            assertEquals(i % 2 == 0, matrix.isDark(6, i))
        }
        assertTrue(matrix.isDark(8, matrix.size - 8))
    }

    @Test
    fun formatInformationIsWrittenTwice() {
        for (mask in 0..7) {
            val code = assertIs<QrEncodeResult.Success>(QrEncoder.encode("HELLO WORLD", QrErrorCorrection.Q, mask)).code
            val m = code.matrix
            val expected = QrSymbolBuilder.formatBits(QrErrorCorrection.Q, mask)

            // 왼쪽 위 사본: (8,0)…(8,5), (8,7), (8,8), (7,8), (5,8)…(0,8) 이 비트 0…14.
            val first = listOf(8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5, 8 to 7, 8 to 8, 7 to 8, 5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8)
            val second = List(8) { m.size - 1 - it to 8 } + List(7) { 8 to m.size - 7 + it }
            for (positions in listOf(first, second)) {
                val read = positions.foldIndexed(0) { i, acc, (x, y) -> if (m.isDark(x, y)) acc or (1 shl i) else acc }
                assertEquals(expected, read, "mask $mask")
            }
        }
    }

    @Test
    fun versionInformationIsWrittenForVersion7AndUp() {
        val code = encode("a".repeat(140), QrErrorCorrection.L)
        assertEquals(7, code.version)
        val m = code.matrix

        var bottomLeft = 0
        var topRight = 0
        for (i in 0 until 18) {
            if (m.isDark(i / 3, m.size - 11 + i % 3)) bottomLeft = bottomLeft or (1 shl i)
            if (m.isDark(m.size - 11 + i % 3, i / 3)) topRight = topRight or (1 shl i)
        }
        assertEquals(0x07C94, bottomLeft)
        assertEquals(0x07C94, topRight)
    }

    @Test
    fun helloWorldQuartileMask0MatchesReferenceMatrix() {
        // 이 격자는 macOS CoreImage 의 CIDetector(QR)로 읽어 "HELLO WORLD", 버전 1, 단계 Q, 마스크 0 으로 확인했다.
        val code = assertIs<QrEncodeResult.Success>(QrEncoder.encode("HELLO WORLD", QrErrorCorrection.Q, mask = 0)).code

        assertEquals(HelloWorldQMask0, code.matrix.toRows())
    }

    @Test
    fun automaticMaskIsTheLowestPenalty() {
        val automatic = encode("HELLO WORLD", QrErrorCorrection.Q)
        val forced = assertIs<QrEncodeResult.Success>(QrEncoder.encode("HELLO WORLD", QrErrorCorrection.Q, automatic.mask)).code

        assertEquals(forced.matrix, automatic.matrix)
        assertTrue(automatic.mask in 0..7)
    }

    @Test
    fun largestSymbolBuildsOnEveryTarget() {
        val code = encode("a".repeat(2953), QrErrorCorrection.L)

        assertEquals(40, code.version)
        assertEquals(177, code.matrix.size)
    }

    private fun encode(text: String, ecc: QrErrorCorrection): QrCode = assertIs<QrEncodeResult.Success>(QrEncoder.encode(text, ecc)).code

    private fun version(text: String, ecc: QrErrorCorrection): Int = encode(text, ecc).version
}

private val HelloWorldQMask0 = listOf(
    "#######.##....#######",
    "#.....#.#..#..#.....#",
    "#.###.#.#..##.#.###.#",
    "#.###.#.#.....#.###.#",
    "#.###.#.#.#...#.###.#",
    "#.....#...#...#.....#",
    "#######.#.#.#.#######",
    "........#............",
    ".##.#.##....#.#.#####",
    ".#......####....#...#",
    "..##.###.##...#.##...",
    ".##.##.#..##.#.#.###.",
    "#...#.#.#.###.###.#.#",
    "........##.#..#...#.#",
    "#######.#.#....#.##..",
    "#.....#..#.##.##.#...",
    "#.###.#.#.#...#######",
    "#.###.#..#.#.#.#...#.",
    "#.###.#.#..#.###.#..#",
    "#.....#.#.####...#.##",
    "#######....#.###....#",
)
