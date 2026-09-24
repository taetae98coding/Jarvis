package io.github.taetae98coding.jarvis.automation

/** 도구가 돌려주는 그림 한 장. [width]·[height] 가 다음 입력의 좌표 공간이다. */
class AutomationImage(
    val bytes: ByteArray,
    val mimeType: String,
    val width: Int,
    val height: Int,
)

/** 도구 결과의 오류 문구로 그대로 나간다. 사용자가 무엇을 하면 되는지까지 적는다. */
class AutomationException(message: String) : Exception(message)
