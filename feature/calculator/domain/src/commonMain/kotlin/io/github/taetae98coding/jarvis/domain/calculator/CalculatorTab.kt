package io.github.taetae98coding.jarvis.domain.calculator

enum class CalculatorTab {
    EXPRESSION,
    PERCENT,
    BMI,
    ;

    /** 저장소에 적는 문자열. 서수 대신 이름을 써서 상수 순서를 바꿔도 저장값이 뒤바뀌지 않는다. */
    val storedValue: String get() = name.lowercase()

    companion object {
        /** 모르는 값은 첫 탭이다. 앱 밖에서 잘못 쓴 값 때문에 화면이 비지 않게 한다. */
        fun fromStored(value: String?): CalculatorTab = entries.firstOrNull { it.storedValue == value } ?: EXPRESSION
    }
}
