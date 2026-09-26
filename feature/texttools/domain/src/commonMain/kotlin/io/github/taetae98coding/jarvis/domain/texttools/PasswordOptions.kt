package io.github.taetae98coding.jarvis.domain.texttools

import kotlin.math.log2

enum class CharacterClass(val characters: String) {
    UPPERCASE("ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
    LOWERCASE("abcdefghijklmnopqrstuvwxyz"),
    DIGITS("0123456789"),
    SYMBOLS("!@#$%^&*()-_=+[]{};:,.<>/?~"),
    ;

    fun characters(excludeAmbiguous: Boolean): String =
        if (excludeAmbiguous) characters.filterNot { it in AmbiguousCharacters } else characters

    companion object {
        const val AmbiguousCharacters = "0O1lI"
    }
}

data class PasswordOptions(
    val length: Int = DefaultLength,
    val uppercase: Boolean = true,
    val lowercase: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = true,
    val excludeAmbiguous: Boolean = false,
) {
    val classes: List<CharacterClass>
        get() = buildList {
            if (uppercase) add(CharacterClass.UPPERCASE)
            if (lowercase) add(CharacterClass.LOWERCASE)
            if (digits) add(CharacterClass.DIGITS)
            if (symbols) add(CharacterClass.SYMBOLS)
        }

    val poolSize: Int get() = classes.sumOf { it.characters(excludeAmbiguous).length }

    /** 균등 무작위로 뽑으므로 엔트로피는 만든 문자열이 아니라 옵션이 정한다(docs/common/text-tools.html R9). */
    val entropyBits: Double get() = if (poolSize == 0) 0.0 else length * log2(poolSize.toDouble())

    val strength: PasswordStrength get() = PasswordStrength.of(entropyBits)

    /** 길이를 범위 안으로 넣고, 켜진 종류가 없으면 소문자를 켠다. 저장값을 앱 밖에서 고쳐도 생성기가 멈추지 않게 한다. */
    fun normalized(): PasswordOptions {
        val clamped = copy(length = length.coerceIn(MinLength, MaxLength))
        return if (clamped.classes.isEmpty()) clamped.copy(lowercase = true) else clamped
    }

    companion object {
        const val MinLength = 4
        const val MaxLength = 64
        const val DefaultLength = 16
    }
}

enum class PasswordStrength {
    WEAK,
    FAIR,
    STRONG,
    VERY_STRONG,
    ;

    companion object {
        fun of(entropyBits: Double): PasswordStrength =
            when {
                entropyBits < 40 -> WEAK
                entropyBits < 60 -> FAIR
                entropyBits < 80 -> STRONG
                else -> VERY_STRONG
            }
    }
}
