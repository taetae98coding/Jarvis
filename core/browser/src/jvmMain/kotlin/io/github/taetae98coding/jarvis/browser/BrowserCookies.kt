package io.github.taetae98coding.jarvis.browser

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Chrome 이 저장한 쿠키 한 개. [sameSite] 는 Chrome DB 의 값(-1 미지정, 0 None, 1 Lax, 2 Strict)이다. */
class BrowserCookieParam(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val isSecure: Boolean,
    val isHttpOnly: Boolean,
    val sameSite: Int,
    val expiresEpochSeconds: Long?,
)

/**
 * 쿠키를 저장한 모양 그대로 엔진의 영구 저장소에 넣고, 넣은 첫 쿠키의 도메인을 되읽어 실제로 들어갔는지 준다
 * (docs/common/chrome-cookie-import.html R8a). WKWebView 때처럼 도메인 쿠키를 호스트마다 펼 필요가 없다.
 */
suspend fun BrowserPage.importCookies(cookies: List<BrowserCookieParam>): Boolean {
    if (cookies.isEmpty()) return false

    cookies.chunked(CookieBatch).forEach { batch ->
        cdp("Network.setCookies", buildJsonObject { put("cookies", JsonArray(batch.map(::cookieJson))) })
    }

    val first = cookies.first()
    val stored = cdp("Network.getCookies", buildJsonObject { put("urls", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(first.storeUrl())) }) })

    return (stored["cookies"] as? JsonArray)?.isNotEmpty() == true
}

private fun cookieJson(cookie: BrowserCookieParam) =
    buildJsonObject {
        put("name", cookie.name)
        put("value", cookie.value)
        put("domain", cookie.domain)
        put("path", cookie.path)
        put("secure", cookie.isSecure)
        put("httpOnly", cookie.isHttpOnly)
        when (cookie.sameSite) {
            0 -> put("sameSite", "None")
            1 -> put("sameSite", "Lax")
            2 -> put("sameSite", "Strict")
        }
        cookie.expiresEpochSeconds?.let { put("expires", it.toDouble()) }
    }

private fun BrowserCookieParam.storeUrl(): String {
    val host = domain.trimStart('.')
    return if (isSecure) "https://$host/" else "http://$host/"
}

// 한 번에 수천 개를 보내면 CDP 메시지가 수 MB 가 된다.
private const val CookieBatch = 500
