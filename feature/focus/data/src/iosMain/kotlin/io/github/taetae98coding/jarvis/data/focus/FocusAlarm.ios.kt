package io.github.taetae98coding.jarvis.data.focus

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.focus.FocusAlarmRepository
import io.github.taetae98coding.jarvis.domain.focus.FocusPhase
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Clock
import kotlin.time.Instant

internal actual fun createFocusAlarm(context: PlatformContext): FocusAlarmRepository = UserNotificationFocusAlarm

internal actual fun localUtcOffsetSeconds(instant: Instant): Int =
    NSTimeZone.localTimeZone.secondsFromGMTForDate(NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())).toInt()

/**
 * 로컬 알림을 시스템에 예약한다. 앱이 꺼지거나 백그라운드여도 시스템이 띄운다.
 *
 * 앱이 앞에 있을 때는 UNUserNotificationCenterDelegate 가 없어 배너가 뜨지 않는다. 그때는 카드가 다음 단계로
 * 넘어가는 것으로 보인다(docs/platform/ios.html#focus-timer).
 */
private object UserNotificationFocusAlarm : FocusAlarmRepository {
    private val center: UNUserNotificationCenter get() = UNUserNotificationCenter.currentNotificationCenter()

    override fun schedule(at: Instant, phase: FocusPhase) {
        // 이미 허용·거부했으면 시스템이 다시 묻지 않고 곧바로 결과를 준다. 거부돼도 add 는 조용히 버려진다.
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { _, _ -> }

        val message = focusAlarmMessage(phase)
        val content = UNMutableNotificationContent().apply {
            setTitle(message.title)
            setBody(message.body)
            setSound(UNNotificationSound.defaultSound)
        }
        // 트리거 간격은 0 보다 커야 한다. 이미 지난 시각이면 곧바로 울리게 최소값을 둔다.
        val seconds = (at - Clock.System.now()).inWholeMilliseconds.coerceAtLeast(MinTriggerMillis) / MillisPerSecond
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)

        // 같은 식별자로 add 하면 기다리던 요청이 바뀐다.
        center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(RequestId, content, trigger), null)
    }

    override fun cancel() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(RequestId))
    }
}

private const val RequestId = "io.github.taetae98coding.jarvis.focus"
private const val MinTriggerMillis = 1_000L
private const val MillisPerSecond = 1_000.0
