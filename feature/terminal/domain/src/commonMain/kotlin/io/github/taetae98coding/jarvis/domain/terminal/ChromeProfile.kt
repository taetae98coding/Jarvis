package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 이 PC 의 Chrome 프로필 하나. [directory] 는 Chrome 데이터 폴더 안의 프로필 폴더 이름(`Default`,
 * `Profile 1`)이고, [email] 은 그 프로필에 로그인된 Google 계정(없으면 null)이다.
 */
data class ChromeProfile(
    val directory: String,
    val name: String,
    val email: String?,
)
