package io.github.taetae98coding.jarvis.domain.qrcode

/** 정사각형 모듈 격자. 조용한 영역(quiet zone)은 들어 있지 않고, 그리는 쪽이 둘레에 붙인다. */
class QrMatrix internal constructor(
    val size: Int,
    private val modules: BooleanArray,
) {
    init {
        require(modules.size == size * size)
    }

    fun isDark(x: Int, y: Int): Boolean = modules[y * size + x]

    /** 한 줄에 한 행, 어두운 모듈은 `#`, 밝은 모듈은 `.`. 테스트의 기준 격자와 비교할 때 쓴다. */
    fun toRows(): List<String> = List(size) { y -> buildString { repeat(size) { x -> append(if (isDark(x, y)) '#' else '.') } } }

    override fun equals(other: Any?): Boolean = other is QrMatrix && size == other.size && modules.contentEquals(other.modules)

    override fun hashCode(): Int = modules.contentHashCode()
}

data class QrCode(
    val version: Int,
    val errorCorrection: QrErrorCorrection,
    val mask: Int,
    val mode: QrMode,
    val matrix: QrMatrix,
)

enum class QrMode(internal val modeBits: Int, private val charCountBits: IntArray) {
    NUMERIC(0x1, intArrayOf(10, 12, 14)),
    ALPHANUMERIC(0x2, intArrayOf(9, 11, 13)),
    BYTE(0x4, intArrayOf(8, 16, 16)),
    ;

    internal fun charCountBits(version: Int): Int = charCountBits[(version + 7) / 17]
}
