package io.github.taetae98coding.jarvis.domain.theme

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /** 모드를 실제 다크 여부로 푼다. [systemInDark] 는 SYSTEM 일 때만 쓰인다. */
    fun isDark(systemInDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDark
        LIGHT -> false
        DARK -> true
    }

    /** 저장소에 적는 문자열. 서수 대신 이름을 써서 상수 순서를 바꿔도 저장값이 뒤바뀌지 않는다. */
    val storedValue: String get() = name.lowercase()

    companion object {
        /** 모르는 값은 SYSTEM 이다. 앱 밖에서 잘못 쓴 값 때문에 화면이 잠기지 않게 한다. */
        fun fromStored(value: String?): ThemeMode =
            entries.firstOrNull { it.storedValue == value } ?: SYSTEM
    }
}
