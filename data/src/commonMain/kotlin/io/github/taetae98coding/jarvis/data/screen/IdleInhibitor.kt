package io.github.taetae98coding.jarvis.data.screen

/** 앱이 떠 있는 동안 OS 의 유휴 타이머를 막는 장치. */
internal fun interface IdleInhibitor {
    fun setEnabled(enabled: Boolean)
}

// 이 플랫폼은 Compose 의 Modifier.keepScreenOn() 이 처리한다. :ui 의 Modifier.keepScreenAwake 주석을 볼 것.
internal object NoIdleInhibitor : IdleInhibitor {
    override fun setEnabled(enabled: Boolean) = Unit
}

internal expect fun createIdleInhibitor(): IdleInhibitor
