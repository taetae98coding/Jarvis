package io.github.taetae98coding.jarvis.domain.texttools

enum class LimitBasis {
    WITH_SPACES,
    WITHOUT_SPACES,
    KOREAN_BYTES,
    ;

    val storedValue: String get() = name.lowercase()

    fun valueOf(stats: TextStats): Int =
        when (this) {
            WITH_SPACES -> stats.charactersWithSpaces
            WITHOUT_SPACES -> stats.charactersWithoutSpaces
            KOREAN_BYTES -> stats.koreanBytes
        }

    companion object {
        fun fromStored(value: String?): LimitBasis = entries.firstOrNull { it.storedValue == value } ?: WITH_SPACES
    }
}

/** [target] 이 null 이면 목표가 없다. 0 이하는 만들 때 null 로 바꾼다. */
data class TextLimit(
    val target: Int?,
    val basis: LimitBasis,
) {
    fun progress(stats: TextStats): LimitProgress? {
        val target = target ?: return null
        val current = basis.valueOf(stats)
        return LimitProgress(current = current, target = target)
    }

    companion object {
        val None = TextLimit(target = null, basis = LimitBasis.WITH_SPACES)

        fun of(target: Int?, basis: LimitBasis): TextLimit = TextLimit(target?.takeIf { it > 0 }, basis)
    }
}

data class LimitProgress(
    val current: Int,
    val target: Int,
) {
    val fraction: Float get() = (current.toFloat() / target).coerceIn(0f, 1f)

    val isOver: Boolean get() = current > target

    val overBy: Int get() = (current - target).coerceAtLeast(0)
}
