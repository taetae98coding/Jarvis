package io.github.taetae98coding.jarvis.widget.screen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.taetae98coding.jarvis.domain.screen.ScreenAwakeSettingsRepository
import io.github.taetae98coding.jarvis.domain.screen.SetKeepSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SyncSystemScreenAwakeUseCase
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeRepository
import io.github.taetae98coding.jarvis.domain.screen.SystemScreenAwakeStatus
import io.github.taetae98coding.jarvis.widget.WriteSettingsPermissionActivity
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SystemScreenAwakeWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val system = inject<SystemScreenAwakeRepository>()
        val settings = inject<ScreenAwakeSettingsRepository>()
        val initialStatus = system.observeStatus().first()
        val initialEnabled = settings.observeKeepSystemScreenAwake().first()

        provideContent {
            // Glance 는 세션이 살아 있는 동안(마지막 이벤트 뒤 약 45초) provideGlance 를 다시 부르지 않고
            // 이 컴포지션을 재구성만 한다. 밖에서 읽은 값은 그동안 고정되므로 여기서 수집한다. 세션이
            // 닫히면 컴포지션과 함께 수집도 끝나 리스너가 풀린다.
            val status by system.observeStatus().collectAsState(initialStatus)
            val enabled by settings.observeKeepSystemScreenAwake().collectAsState(initialEnabled)

            GlanceTheme {
                SystemScreenAwakeWidgetContent(status, enabled)
            }
        }
    }
}

@Composable
private fun SystemScreenAwakeWidgetContent(
    status: SystemScreenAwakeStatus,
    enabled: Boolean,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "화면 꺼짐 방지",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )

            Button(
                text = if (enabled) "켜짐" else "꺼짐",
                // 권한이 없으면 권한 중계 Activity 를 연다. 런처가 Activity PendingIntent 를 직접 보내므로
                // BroadcastReceiver 안의 startActivity 에 걸리는 백그라운드 시작 제한과 무관하고, 그려진
                // 버튼이 낡아도 중계 Activity 가 탭 시점에 권한을 다시 본다.
                onClick = if (status.permitted) {
                    actionRunCallback<ToggleSystemScreenAwakeAction>(actionParametersOf(EnabledKey to !enabled))
                } else {
                    actionStartActivity<WriteSettingsPermissionActivity>()
                },
                colors = if (enabled) {
                    ButtonDefaults.buttonColors(
                        backgroundColor = GlanceTheme.colors.primary,
                        contentColor = GlanceTheme.colors.onPrimary,
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        backgroundColor = GlanceTheme.colors.secondaryContainer,
                        contentColor = GlanceTheme.colors.onSecondaryContainer,
                    )
                },
            )
        }

        Text(
            text = describe(status, enabled),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            modifier = GlanceModifier.padding(top = 4.dp),
        )
    }
}

class ToggleSystemScreenAwakeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val enabled = parameters[EnabledKey] ?: return

        inject<SetKeepSystemScreenAwakeUseCase>()(enabled)
        // 카드 경로에서 시스템에 쓰는 ApplySystemScreenAwakeUseCase 는 앱 루트 ViewModel 의 스코프에서만
        // 돈다. 위젯에는 그 ViewModel 이 없어 여기서 한 번 반영한다.
        inject<SyncSystemScreenAwakeUseCase>()()
        SystemScreenAwakeWidget().update(context, glanceId)
    }
}

// 문구는 docs/common/home-screen-widget.html#behavior 가 기준이다. 카드(:feature:screen:ui)와 비슷하지만
// 위젯 모듈은 ui 를 의존하지 않아 공유할 수 없다.
private fun describe(status: SystemScreenAwakeStatus, enabled: Boolean): String {
    if (!status.permitted) return "시스템 설정 변경 권한이 필요합니다. 누르면 권한 설정 화면이 열립니다."

    val state = if (enabled) "앱을 닫아도 화면이 꺼지지 않습니다." else "시스템 설정대로 화면이 꺼집니다."
    val current = status.screenOffTimeout?.let { " 현재 시스템 설정은 ${it.describe()}입니다." } ?: ""

    return state + current
}

private fun Duration.describe(): String =
    when {
        this >= 1.days -> "${inWholeDays}일"
        this >= 1.hours -> "${inWholeHours}시간"
        this >= 1.minutes -> "${inWholeMinutes}분"
        else -> "${inWholeSeconds}초"
    }

private val EnabledKey = ActionParameters.Key<Boolean>("enabled")

// Glance 가 ActionCallback 과 GlanceAppWidget 을 리플렉션으로 만들어 생성자 주입이 안 된다.
// 이 모듈에서만 Koin 을 직접 꺼낸다.
internal inline fun <reified T : Any> inject(): T = GlobalContext.get().get()
