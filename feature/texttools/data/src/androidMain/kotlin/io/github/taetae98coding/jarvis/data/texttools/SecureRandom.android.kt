package io.github.taetae98coding.jarvis.data.texttools

import java.security.SecureRandom

// Android 의 기본 SecureRandom 은 Conscrypt 의 OpenSSLRandom(BoringSSL, 커널 난수 시드)이다.
// setSeed 로 시드를 넣으면 옛 SHA1PRNG 에서 결정적이 되던 문제가 있어 넣지 않는다.
private val random = SecureRandom()

internal actual fun secureRandomInt(): Int = random.nextInt()
