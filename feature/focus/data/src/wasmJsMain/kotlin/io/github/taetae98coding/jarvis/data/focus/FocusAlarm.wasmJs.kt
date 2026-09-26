package io.github.taetae98coding.jarvis.data.focus

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.focus.FocusAlarmRepository
import io.github.taetae98coding.jarvis.domain.focus.FocusPhase
import kotlin.time.Clock
import kotlin.time.Instant

internal actual fun createFocusAlarm(context: PlatformContext): FocusAlarmRepository = BrowserFocusAlarm

// getTimezoneOffset 은 UTC 에서 현지를 뺀 분이라 부호가 반대다.
internal actual fun localUtcOffsetSeconds(instant: Instant): Int =
    -(timezoneOffsetMinutes(instant.toEpochMilliseconds().toDouble()) * SecondsPerMinute).toInt()

/**
 * 탭 안의 `setTimeout` 이 끝나는 시각에 Notification API 로 띄운다. 브라우저에는 탭이 닫혀도 남는 예약 알림이
 * 없다(Notification Triggers 는 Chrome 실험에서 멈췄다). 탭을 닫으면 알림도 없다(docs/platform/web.html#focus-timer).
 */
private object BrowserFocusAlarm : FocusAlarmRepository {
    private var timeoutId: Int? = null

    override fun schedule(at: Instant, phase: FocusPhase) {
        // 권한 요청은 사용자 동작 직후여야 한다. 시작 버튼을 누른 흐름에서 불린다.
        requestNotificationPermission()

        cancel()
        val message = focusAlarmMessage(phase)
        val delay = (at - Clock.System.now()).inWholeMilliseconds.coerceAtLeast(0).toDouble()
        timeoutId = scheduleNotification(delay, message.title, message.body)
    }

    override fun cancel() {
        timeoutId?.let(::clearTimeout)
        timeoutId = null
    }
}

private const val SecondsPerMinute = 60

private fun timezoneOffsetMinutes(epochMillis: Double): Double = js("new Date(epochMillis).getTimezoneOffset()")

// Notification 이 없는 브라우저(구형 iOS Safari 등)와 보안 컨텍스트가 아닌 페이지에서는 아무것도 하지 않는다.
private fun requestNotificationPermission(): JsAny? =
    js("((typeof Notification !== 'undefined' && Notification.permission === 'default') ? Notification.requestPermission() : null)")

private fun scheduleNotification(delayMillis: Double, title: String, body: String): Int =
    js(
        """setTimeout(function () {
            if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
                new Notification(title, { body: body });
            }
        }, delayMillis)""",
    )

private fun clearTimeout(id: Int): Unit = js("clearTimeout(id)")
