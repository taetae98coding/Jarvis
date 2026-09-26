package io.github.taetae98coding.jarvis.domain.qrcode

/** 오류 정정 단계. [formatBits] 는 ISO/IEC 18004 표 C.1 의 두 비트라 L·M·Q·H 순서와 다르다. */
enum class QrErrorCorrection(internal val formatBits: Int) {
    L(1),
    M(0),
    Q(3),
    H(2),
    ;

    val storedValue: String get() = name

    companion object {
        val Default = M

        fun fromStored(value: String?): QrErrorCorrection = entries.firstOrNull { it.storedValue == value } ?: Default
    }
}
