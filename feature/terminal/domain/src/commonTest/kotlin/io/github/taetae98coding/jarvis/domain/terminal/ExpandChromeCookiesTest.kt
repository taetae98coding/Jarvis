package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpandChromeCookiesTest {
    private fun cookie(domain: String, name: String = "c") =
        BrowserCookie(name, "v", domain, "/", null, isSecure = true, isHttpOnly = false, sameSite = 1, isSessionOnly = false)

    @Test
    fun hostOnlyCookieIsUnchanged() {
        val c = cookie("accounts.google.com")
        assertEquals(listOf(c), expandCookiesForHostOnlyStore(listOf(c)))
    }

    @Test
    fun domainCookieExpandsToApexWwwAndSiblingHosts() {
        val sid = cookie(".google.com", "SID")
        val sibling = cookie("accounts.google.com", "OTZ")
        val other = cookie(".naver.com", "NID")

        val expanded = expandCookiesForHostOnlyStore(listOf(sid, sibling, other))

        val sidHosts = expanded.filter { it.name == "SID" }.map { it.domain }.toSet()
        // apex, www, 그리고 프로필에 있는 하위 도메인(accounts.google.com).
        assertEquals(setOf("google.com", "www.google.com", "accounts.google.com"), sidHosts)
        // 모두 호스트 전용(앞에 . 없음).
        assertTrue(expanded.none { it.domain.startsWith(".") })
    }

    @Test
    fun naverDomainCookieReachesNidAndWww() {
        val nid = cookie(".naver.com", "NID_SES")
        val nidSub = cookie(".nid.naver.com", "x")
        val www = cookie("www.naver.com", "y")

        val hosts = expandCookiesForHostOnlyStore(listOf(nid, nidSub, www))
            .filter { it.name == "NID_SES" }.map { it.domain }.toSet()

        assertTrue("www.naver.com" in hosts)
        assertTrue("nid.naver.com" in hosts)
        assertTrue("naver.com" in hosts)
    }
}
