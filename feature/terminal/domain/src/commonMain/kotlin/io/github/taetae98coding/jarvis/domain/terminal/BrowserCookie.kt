package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 웹뷰에 넣을 쿠키 하나. Chrome 에서 복호화해 꺼낸 값이며, data 계층이 이 모델로 돌려주고 UI 가
 * 웹뷰 라이브러리의 쿠키로 바꿔 넣는다(docs/common/chrome-cookie-import.html).
 *
 * [expiresEpochSeconds] 는 만료 시각(epoch 초), 세션 쿠키면 null 이다. [sameSite] 는 Chrome `cookies.samesite`
 * 값 그대로다 — -1=미지정, 0=None, 1=Lax, 2=Strict. 웹뷰가 받아들이는 정책으로 바꾸는 것은 UI 가 한다.
 */
data class BrowserCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expiresEpochSeconds: Long?,
    val isSecure: Boolean,
    val isHttpOnly: Boolean,
    val sameSite: Int,
    val isSessionOnly: Boolean,
)
