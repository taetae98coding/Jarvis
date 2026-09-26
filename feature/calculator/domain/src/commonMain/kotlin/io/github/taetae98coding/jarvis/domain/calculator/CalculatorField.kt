package io.github.taetae98coding.jarvis.domain.calculator

/** 앱을 껐다 켜도 남는 입력 칸. */
enum class CalculatorField {
    EXPRESSION,
    PERCENT_OF_BASE,
    PERCENT_OF_RATE,
    RATIO_PART,
    RATIO_WHOLE,
    CHANGE_FROM,
    CHANGE_TO,
    BMI_HEIGHT,
    BMI_WEIGHT,
    ;

    /** 저장 키에 붙는 문자열. 서수 대신 이름을 써서 상수 순서를 바꿔도 저장값이 뒤바뀌지 않는다. */
    val storedValue: String get() = name.lowercase()
}
