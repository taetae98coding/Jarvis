package io.github.taetae98coding.jarvis.data.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.GraphicsEnvironment
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 시스템 트레이 아이콘으로 운영체제 알림을 보낸다. macOS JDK 는 `NSUserNotificationCenter` 로 보낸다.
 * 다른 후보(Compose Tray, osascript, UNUserNotificationCenter)를 버린 이유는 docs/platform/jvm.html#claude-notification 에 있다.
 */
internal class TrayNotifier(private val linger: Duration = TrayIconLinger) {
    private val lock = Any()
    private val timer by lazy { Timer("jarvis-tray-notifier", true) }
    private var icon: TrayIcon? = null
    private var removal: TimerTask? = null

    suspend fun show(title: String, message: String) {
        withContext(Dispatchers.IO) {
            // 헤드리스(테스트·CI)이거나 트레이가 없는 데스크톱이면 조용히 넘어간다.
            if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) return@withContext

            runCatching {
                synchronized(lock) {
                    val shown = icon ?: TrayIcon(BlankImage, "Jarvis").also {
                        SystemTray.getSystemTray().add(it)
                        icon = it
                    }
                    shown.displayMessage(title, message, TrayIcon.MessageType.NONE)
                    scheduleRemoval()
                }
            }
        }
    }

    // 아이콘을 붙인 직후에 떼면 알림이 전달되지 않는다(macOS, JDK 25.0.4 에서 확인). 마지막 알림 뒤 [linger] 만큼 둔다.
    private fun scheduleRemoval() {
        removal?.cancel()
        removal = timer.schedule(linger.inWholeMilliseconds) {
            synchronized(lock) {
                icon?.let { runCatching { SystemTray.getSystemTray().remove(it) } }
                icon = null
            }
        }
    }
}

private val TrayIconLinger: Duration = 10.seconds

// 메뉴 막대에 보이지 않게 1×1 투명 이미지를 쓴다. 알림에는 앱 아이콘이 붙는다.
private val BlankImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
