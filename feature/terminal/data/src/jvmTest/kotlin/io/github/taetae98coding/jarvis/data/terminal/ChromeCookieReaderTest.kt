package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.ChromeProfile
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 프로필 파싱·만료 시각 변환·쿠키 복호화의 순수 로직을 확인한다. 실제 키체인·SQLite·웹뷰는 붙지 않아
 * docs/platform/jvm.html#chrome-cookie-import 대로 손으로 확인한다.
 */
class ChromeCookieReaderTest {
    @Test
    fun parsesProfilesWithDefaultFirstAndEmail() {
        val json = """
            {
              "profile": {
                "info_cache": {
                  "Profile 1": { "name": "Work", "user_name": "work@example.com" },
                  "Default": { "name": "Home", "user_name": "home@example.com" }
                }
              }
            }
        """.trimIndent()

        assertEquals(
            listOf(
                ChromeProfile(directory = "Default", name = "Home", email = "home@example.com"),
                ChromeProfile(directory = "Profile 1", name = "Work", email = "work@example.com"),
            ),
            parseChromeProfiles(json),
        )
    }

    @Test
    fun profileWithoutUserNameHasNullEmail() {
        val json = """{ "profile": { "info_cache": { "Default": { "name": "Home", "user_name": "" } } } }"""

        assertEquals(listOf(ChromeProfile("Default", "Home", null)), parseChromeProfiles(json))
    }

    @Test
    fun missingInfoCacheIsEmpty() {
        assertEquals(emptyList(), parseChromeProfiles("""{ "profile": {} }"""))
    }

    @Test
    fun parsesSqliteJsonRow() {
        // sqlite3 -json 이 내는 모양(hex 는 enc). 값·필드가 그대로 매핑되는지.
        val json = """
            [{ "value": "", "host_key": ".google.com", "name": "SID", "enc": "763130AABB",
               "path": "/", "expires_utc": 13390000000000000, "is_secure": 1, "is_httponly": 0,
               "samesite": 1, "is_persistent": 1 }]
        """.trimIndent()

        val row = parseCookieJson(json).single()

        assertEquals(".google.com", row.hostKey)
        assertEquals("SID", row.name)
        assertEquals("/", row.path)
        assertEquals(13390000000000000L, row.expiresUtc)
        assertEquals(true, row.isSecure)
        assertEquals(false, row.isHttpOnly)
        assertEquals(1, row.sameSite)
        assertEquals(true, row.isPersistent)
        // enc "763130AABB" = v10 + 0xAA 0xBB
        assertEquals(listOf(0x76, 0x31, 0x30, 0xAA, 0xBB), row.encrypted.map { it.toInt() and 0xFF })
    }

    @Test
    fun emptySqliteJsonIsEmpty() {
        assertEquals(emptyList(), parseCookieJson(""))
    }

    @Test
    fun convertsChromeEpochToEpochSeconds() {
        // 1601-01-01 기준 마이크로초. 11644473600 초가 1601→1970 간격이다.
        assertEquals(0L, chromeEpochToEpochSeconds(11_644_473_600L * 1_000_000L))
        assertEquals(1_000L, chromeEpochToEpochSeconds((11_644_473_600L + 1_000L) * 1_000_000L))
    }

    @Test
    fun zeroOrNegativeExpiryIsSession() {
        assertNull(chromeEpochToEpochSeconds(0L))
        assertNull(chromeEpochToEpochSeconds(-1L))
    }

    @Test
    fun decryptsRoundTripWithoutDomainPrefix() {
        val key = deriveKey("test-password")

        val encrypted = encryptV10("session=abc123".toByteArray(Charsets.UTF_8), key)

        assertEquals("session=abc123", decryptCookieValue(encrypted, key, "example.com"))
    }

    @Test
    fun decryptsRoundTripStrippingDomainHashPrefix() {
        val key = deriveKey("test-password")
        val hostKey = ".google.com"
        val domainHash = MessageDigest.getInstance("SHA-256").digest(hostKey.toByteArray(Charsets.UTF_8))

        val encrypted = encryptV10(domainHash + "value".toByteArray(Charsets.UTF_8), key)

        assertEquals("value", decryptCookieValue(encrypted, key, hostKey))
    }

    @Test
    fun keepsPrefixWhenItIsNotThisDomainsHash() {
        val key = deriveKey("test-password")
        val otherHash = MessageDigest.getInstance("SHA-256").digest("other.com".toByteArray(Charsets.UTF_8))

        // 앞 32바이트가 host_key 해시가 아니면 값의 일부다 — 떼지 않는다.
        val encrypted = encryptV10(otherHash + "tail".toByteArray(Charsets.UTF_8), key)

        assertEquals(String(otherHash + "tail".toByteArray(Charsets.UTF_8), Charsets.UTF_8), decryptCookieValue(encrypted, key, "example.com"))
    }

    @Test
    fun rejectsValueWithoutV10Prefix() {
        val key = deriveKey("test-password")

        assertNull(decryptCookieValue("nope".toByteArray(Charsets.UTF_8), key, "example.com"))
    }

    private fun encryptV10(plain: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(ByteArray(16) { ' '.code.toByte() }))
        return "v10".toByteArray(Charsets.UTF_8) + cipher.doFinal(plain)
    }
}
