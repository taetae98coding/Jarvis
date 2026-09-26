package io.github.taetae98coding.jarvis.domain.texttools

/**
 * 플랫폼 CSPRNG 에서 온 32비트. `kotlin.random.Random` 으로 구현하면 안 된다(docs/common/text-tools.html R8).
 */
fun interface SecureRandomSource {
    fun nextInt(): Int
}

class PasswordGenerator(
    private val random: SecureRandomSource,
) {
    fun generate(options: PasswordOptions): String {
        val normalized = options.normalized()
        val classes = normalized.classes.map { it.characters(normalized.excludeAmbiguous) }
        val pool = classes.joinToString("")

        val characters = CharArray(normalized.length)
        // MinLength 가 종류 수(4) 이상이라 종류마다 한 자리를 먼저 채울 수 있다.
        classes.forEachIndexed { index, set -> characters[index] = set[nextIndex(set.length)] }
        for (index in classes.size until characters.size) {
            characters[index] = pool[nextIndex(pool.length)]
        }

        // Fisher–Yates. 보장한 글자가 늘 앞에 있지 않게 섞는다.
        for (index in characters.lastIndex downTo 1) {
            val other = nextIndex(index + 1)
            val swap = characters[index]
            characters[index] = characters[other]
            characters[other] = swap
        }
        return characters.concatToString()
    }

    /**
     * `0 until bound` 의 균등한 정수. `2^32` 가 [bound] 로 나누어떨어지지 않으면 `mod` 가 앞쪽 값을 더 자주 내므로,
     * 나머지 꼬리에 떨어진 값은 버리고 다시 뽑는다.
     */
    internal fun nextIndex(bound: Int): Int {
        require(bound > 0)
        val range = 1L shl 32
        val limit = range - range % bound
        while (true) {
            val value = random.nextInt().toLong() and 0xFFFF_FFFFL
            if (value < limit) return (value % bound).toInt()
        }
    }
}
