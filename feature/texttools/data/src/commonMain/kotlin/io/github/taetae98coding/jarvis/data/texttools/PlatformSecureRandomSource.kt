package io.github.taetae98coding.jarvis.data.texttools

import io.github.taetae98coding.jarvis.domain.texttools.SecureRandomSource

internal object PlatformSecureRandomSource : SecureRandomSource {
    override fun nextInt(): Int = secureRandomInt()
}

/** 플랫폼 CSPRNG 의 32비트. 원천을 얻지 못하면 던진다 — 약한 난수로 조용히 물러나지 않는다. */
internal expect fun secureRandomInt(): Int
