package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.BrowserCookie
import io.github.taetae98coding.jarvis.domain.terminal.ChromeProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 이 PC 의 Chrome 프로필과 쿠키를 읽어 복호화한다(docs/platform/jvm.html#chrome-cookie-import).
 * macOS 만 지원한다 — 키는 키체인의 `Chrome Safe Storage` 에 있고, 다른 OS 는 보관 방식이 다르다.
 *
 * 실패(키체인 거부, Chrome 미설치, `sqlite3` 부재 등)는 모두 삼켜 빈 목록으로 둔다(공통 R8).
 */
internal object ChromeCookieReader {
    // 데스크탑은 macOS 만 지원한다. 다른 OS 에는 분기를 만들지 않는다.
    val isSupported: Boolean
        get() = System.getProperty("os.name").orEmpty().startsWith("Mac") && chromeDir().isDirectory

    /** cold: 수집할 때 `Local State` 를 읽어 한 번 내보낸다. */
    fun observeProfiles(): Flow<List<ChromeProfile>> = flow { emit(readProfiles()) }.flowOn(Dispatchers.IO)

    suspend fun importCookies(profileDirectory: String): List<BrowserCookie> =
        withContext(Dispatchers.IO) {
            runCatching {
                val password = readKeychainPassword() ?: return@runCatching emptyList()
                val key = deriveKey(password)
                val cookies = File(File(chromeDir(), profileDirectory), "Cookies").takeIf { it.isFile }
                    ?: return@runCatching emptyList()
                readCookieRows(cookies).mapNotNull { it.toBrowserCookie(key) }
            }.getOrDefault(emptyList())
        }

    private fun readProfiles(): List<ChromeProfile> =
        runCatching {
            val localState = File(chromeDir(), "Local State").takeIf { it.isFile } ?: return emptyList()
            parseChromeProfiles(localState.readText())
                .filter { File(File(chromeDir(), it.directory), "Cookies").isFile }
        }.getOrDefault(emptyList())

    /**
     * 키체인의 generic password(서비스 `Chrome Safe Storage`, 계정 `Chrome`). 이 앱은 Chrome 이 아니라서
     * 처음 읽을 때 키체인 접근 허용 창이 뜬다. 거부하면 종료 코드가 0 이 아니라 null 이 된다.
     */
    private fun readKeychainPassword(): String? =
        runCatching {
            val process = ProcessBuilder(
                "security", "find-generic-password", "-w", "-s", "Chrome Safe Storage", "-a", "Chrome",
            ).start()
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && output.isNotEmpty()) output else null
        }.getOrNull()

    /**
     * `Cookies` SQLite 를 임시 폴더로 복사해(Chrome 이 켜져 있으면 잠겨 있다) 시스템 `sqlite3 -json` 으로 읽는다.
     * SQLite JDBC 의존성을 새로 넣지 않으려고 CLI 를 쓴다(터미널의 `lsof`·`security` 와 같은 방식). 기본 list
     * 모드는 제어문자를 caret 표기(`0x1f`→`^_`)로 바꿔 값이 깨지므로, 값을 그대로 담는 JSON 으로 받는다.
     * 최근 쿠키가 아직 메인 DB 에 없을 수 있어 `-wal`·`-shm` 도 함께 복사한다.
     */
    private fun readCookieRows(cookies: File): List<CookieRow> {
        val temp = File.createTempFile("jarvis-cookies", ".db")
        val wal = File(temp.parentFile, temp.name + "-wal")
        val shm = File(temp.parentFile, temp.name + "-shm")
        try {
            cookies.copyTo(temp, overwrite = true)
            File(cookies.parentFile, cookies.name + "-wal").takeIf { it.isFile }?.copyTo(wal, overwrite = true)
            File(cookies.parentFile, cookies.name + "-shm").takeIf { it.isFile }?.copyTo(shm, overwrite = true)

            val query = "SELECT value,host_key,name,hex(encrypted_value) AS enc,path,expires_utc," +
                "is_secure,is_httponly,samesite,is_persistent FROM cookies;"
            val process = ProcessBuilder("sqlite3", "-json", temp.path, query).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            return parseCookieJson(output)
        } finally {
            temp.delete()
            wal.delete()
            shm.delete()
        }
    }
}

/** `sqlite3 -json` 출력(빈 결과면 빈 문자열)을 [CookieRow] 로 읽는다. hex 를 못 읽으면 그 줄만 버린다. */
internal fun parseCookieJson(json: String): List<CookieRow> {
    val trimmed = json.trim()
    if (trimmed.isEmpty()) return emptyList()

    return Json.parseToJsonElement(trimmed).jsonArray.mapNotNull { element ->
        val obj = element.jsonObject
        val encrypted = hexToBytes(obj["enc"]?.jsonPrimitive?.contentOrNull.orEmpty()) ?: return@mapNotNull null

        CookieRow(
            plainValue = obj["value"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            hostKey = obj["host_key"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            encrypted = encrypted,
            path = obj["path"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            expiresUtc = obj["expires_utc"]?.jsonPrimitive?.longOrNull ?: 0L,
            isSecure = (obj["is_secure"]?.jsonPrimitive?.intOrNull ?: 0) == 1,
            isHttpOnly = (obj["is_httponly"]?.jsonPrimitive?.intOrNull ?: 0) == 1,
            sameSite = obj["samesite"]?.jsonPrimitive?.intOrNull ?: -1,
            isPersistent = (obj["is_persistent"]?.jsonPrimitive?.intOrNull ?: 0) == 1,
        )
    }
}

/** 복호화 전의 쿠키 한 줄. */
internal class CookieRow(
    val plainValue: String,
    val hostKey: String,
    val name: String,
    val encrypted: ByteArray,
    val path: String,
    val expiresUtc: Long,
    val isSecure: Boolean,
    val isHttpOnly: Boolean,
    val sameSite: Int,
    val isPersistent: Boolean,
)

private fun CookieRow.toBrowserCookie(key: ByteArray): BrowserCookie? {
    // 암호화되지 않은 쿠키는 평문 value 컬럼에 값이 있다(드물다).
    val value = if (encrypted.size >= 3) decryptCookieValue(encrypted, key, hostKey) ?: return null else plainValue

    return BrowserCookie(
        name = name,
        value = value,
        domain = hostKey,
        path = path,
        expiresEpochSeconds = if (isPersistent) chromeEpochToEpochSeconds(expiresUtc) else null,
        isSecure = isSecure,
        isHttpOnly = isHttpOnly,
        // Chrome 값 그대로(-1 미지정·0 None·1 Lax·2 Strict). 웹뷰가 받아들이는 정책으로 바꾸는 것은 UI 다(R3a).
        sameSite = sameSite,
        isSessionOnly = !isPersistent,
    )
}

internal fun parseChromeProfiles(localStateJson: String): List<ChromeProfile> {
    val cache = Json.parseToJsonElement(localStateJson).jsonObject["profile"]
        ?.jsonObject?.get("info_cache")?.jsonObject
        ?: return emptyList()

    return cache.entries
        .map { (directory, value) ->
            val obj = value.jsonObject
            ChromeProfile(
                directory = directory,
                name = obj["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: directory,
                email = obj["user_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
            )
        }
        // Default 를 맨 앞에, 나머지는 폴더 이름 순으로.
        .sortedWith(compareBy({ it.directory != "Default" }, { it.directory }))
}

internal fun deriveKey(password: String): ByteArray {
    // Chrome 이 macOS 에서 쓰는 고정 파라미터: salt "saltysalt", 반복 1003, AES-128.
    val spec = PBEKeySpec(password.toCharArray(), "saltysalt".toByteArray(Charsets.UTF_8), 1003, 128)
    return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
}

internal fun decryptCookieValue(encrypted: ByteArray, key: ByteArray, hostKey: String): String? =
    runCatching {
        // v10(macOS)만 다룬다. 그 밖의 접두사는 모른다.
        if (encrypted.size < 3 ||
            encrypted[0] != 'v'.code.toByte() ||
            encrypted[1] != '1'.code.toByte() ||
            encrypted[2] != '0'.code.toByte()
        ) {
            return null
        }

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        // IV 는 공백 16바이트다.
        val iv = IvParameterSpec(ByteArray(16) { ' '.code.toByte() })
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), iv)
        val plain = cipher.doFinal(encrypted.copyOfRange(3, encrypted.size))

        String(stripDomainHashPrefix(plain, hostKey), Charsets.UTF_8)
    }.getOrNull()

internal fun stripDomainHashPrefix(plain: ByteArray, hostKey: String): ByteArray {
    // Chrome 130+(macOS)는 평문 앞에 32바이트 SHA256(host_key)를 붙인다. 같으면 뗀다.
    if (plain.size < 32) return plain
    val expected = MessageDigest.getInstance("SHA-256").digest(hostKey.toByteArray(Charsets.UTF_8))
    return if (plain.copyOfRange(0, 32).contentEquals(expected)) plain.copyOfRange(32, plain.size) else plain
}

internal fun chromeEpochToEpochSeconds(expiresUtc: Long): Long? {
    // expires_utc 는 1601-01-01 기준 마이크로초다. 0 이하는 만료 없음(세션).
    if (expiresUtc <= 0L) return null
    return expiresUtc / 1_000_000L - 11_644_473_600L
}

private fun hexToBytes(hex: String): ByteArray? {
    if (hex.length % 2 != 0) return null
    return runCatching {
        ByteArray(hex.length / 2) { ((hex[it * 2].digitToInt(16) shl 4) or hex[it * 2 + 1].digitToInt(16)).toByte() }
    }.getOrNull()
}

private fun chromeDir(): File = File(System.getProperty("user.home"), "Library/Application Support/Google/Chrome")
