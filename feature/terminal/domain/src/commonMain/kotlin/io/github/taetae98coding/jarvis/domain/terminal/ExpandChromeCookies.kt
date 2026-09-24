package io.github.taetae98coding.jarvis.domain.terminal

/**
 * 도메인 쿠키(`.example.com`)를 호스트 전용 쿠키 여러 개로 편다.
 *
 * 데스크톱 웹뷰(wry)는 `setCookie` 로 넣은 쿠키를 앞의 `.` 을 떼고 **호스트 전용**으로 저장한다. 그래서
 * `.google.com` 쿠키가 `google.com` 에만 붙고 `www.google.com`·`accounts.google.com` 같은 하위 도메인에는
 * 붙지 않아, 로그인 쿠키가 정작 로그인 검사를 하는 하위 도메인에 닿지 않는다(docs/platform/jvm.html#chrome-cookie-import).
 *
 * 우회로, 도메인 쿠키를 그것이 닿아야 할 호스트마다 호스트 전용으로 복제해서 넣는다. 대상은
 * `example.com`, `www.example.com`, 그리고 이 프로필이 쿠키를 가진 하위 도메인 전부다(그 계정이 실제로
 * 쓰는 하위 도메인). 호스트 전용 쿠키는 그대로 둔다.
 *
 * 한계: 프로필에 쿠키가 없던 하위 도메인(예: 방문 전의 `myaccount.google.com`)은 빠진다. 로그인 흐름은
 * 보통 쿠키가 있는 하위 도메인(`accounts.*`, `nid.*`)을 거치므로 대개 이어진다.
 */
fun expandCookiesForHostOnlyStore(cookies: List<BrowserCookie>): List<BrowserCookie> {
    val hosts = cookies.mapTo(mutableSetOf()) { it.domain.removePrefix(".") }

    return cookies.flatMap { cookie ->
        if (!cookie.domain.startsWith(".")) {
            listOf(cookie)
        } else {
            val base = cookie.domain.removePrefix(".")
            val targets = buildSet {
                add(base)
                add("www.$base")
                hosts.forEach { if (it == base || it.endsWith(".$base")) add(it) }
            }
            targets.map { cookie.copy(domain = it) }
        }
    }
}
