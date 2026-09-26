package io.github.taetae98coding.jarvis.domain.qrcode

/** ISO/IEC 18004 표 9 에서 옮긴 블록 구성. 0번 칸은 버전 번호와 인덱스를 맞추려는 자리다. */
internal object QrTables {
    const val MinVersion = 1
    const val MaxVersion = 40

    private val EccCodewordsPerBlock: Map<QrErrorCorrection, IntArray> = mapOf(
        QrErrorCorrection.L to intArrayOf(
            -1, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28,
            28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
        ),
        QrErrorCorrection.M to intArrayOf(
            -1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26,
            26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28,
        ),
        QrErrorCorrection.Q to intArrayOf(
            -1, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30,
            28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
        ),
        QrErrorCorrection.H to intArrayOf(
            -1, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28,
            30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
        ),
    )

    private val ErrorCorrectionBlocks: Map<QrErrorCorrection, IntArray> = mapOf(
        QrErrorCorrection.L to intArrayOf(
            -1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 8,
            8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25,
        ),
        QrErrorCorrection.M to intArrayOf(
            -1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16,
            17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49,
        ),
        QrErrorCorrection.Q to intArrayOf(
            -1, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21, 20,
            23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68,
        ),
        QrErrorCorrection.H to intArrayOf(
            -1, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25,
            25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81,
        ),
    )

    fun eccCodewordsPerBlock(version: Int, ecc: QrErrorCorrection): Int = EccCodewordsPerBlock.getValue(ecc)[version]

    fun errorCorrectionBlocks(version: Int, ecc: QrErrorCorrection): Int = ErrorCorrectionBlocks.getValue(ecc)[version]

    /** 기능 패턴과 형식·버전 정보를 뺀, 데이터와 ECC 가 들어가는 모듈 수. 8 의 배수가 아닐 수 있다(남는 비트). */
    fun rawDataModules(version: Int): Int {
        var result = (16 * version + 128) * version + 64
        if (version >= 2) {
            val alignments = version / 7 + 2
            result -= (25 * alignments - 10) * alignments - 55
            if (version >= 7) result -= 36
        }
        return result
    }

    fun dataCodewords(version: Int, ecc: QrErrorCorrection): Int =
        rawDataModules(version) / 8 - eccCodewordsPerBlock(version, ecc) * errorCorrectionBlocks(version, ecc)

    /** 정렬 패턴 중심의 좌표(가로·세로 같음). 버전 1 은 없다. */
    fun alignmentPatternPositions(version: Int): IntArray {
        if (version == 1) return IntArray(0)
        val count = version / 7 + 2
        val step = (version * 8 + count * 3 + 5) / (count * 4 - 4) * 2
        val result = IntArray(count)
        result[0] = 6
        var position = version * 4 + 17 - 7
        for (i in count - 1 downTo 1) {
            result[i] = position
            position -= step
        }
        return result
    }
}
