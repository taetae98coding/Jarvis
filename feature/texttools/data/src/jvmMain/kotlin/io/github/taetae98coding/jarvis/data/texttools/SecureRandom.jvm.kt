package io.github.taetae98coding.jarvis.data.texttools

import java.security.SecureRandom

// 기본 생성자는 macOS 에서 NativePRNG(/dev/urandom)다. getInstanceStrong() 은 /dev/random 에서 막힐 수 있어 버렸다.
private val random = SecureRandom()

internal actual fun secureRandomInt(): Int = random.nextInt()
