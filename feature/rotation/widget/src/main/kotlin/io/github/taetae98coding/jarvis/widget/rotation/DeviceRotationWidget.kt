package io.github.taetae98coding.jarvis.widget.rotation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonColors
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationRepository
import io.github.taetae98coding.jarvis.domain.rotation.DeviceRotationStatus
import io.github.taetae98coding.jarvis.domain.rotation.RotationAngle
import io.github.taetae98coding.jarvis.widget.WriteSettingsPermissionActivity
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext

class DeviceRotationWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = inject<DeviceRotationRepository>()
        val initial = repository.observeStatus().first()

        provideContent {
            // Glance 는 세션이 살아 있는 동안(마지막 이벤트 뒤 약 45초) provideGlance 를 다시 부르지 않고
            // 이 컴포지션을 재구성만 한다. 밖에서 읽은 값은 그동안 고정되므로 여기서 수집한다. 세션이
            // 닫히면 컴포지션과 함께 수집도 끝나 리스너가 풀린다.
            val status by repository.observeStatus().collectAsState(initial)

            GlanceTheme {
                DeviceRotationWidgetContent(status)
            }
        }
    }
}

@Composable
private fun DeviceRotationWidgetContent(status: DeviceRotationStatus) {
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
                text = "화면 회전",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )

            Button(
                text = if (status.locked) "고정" else "자동",
                onClick = status.actionOrPermission {
                    actionRunCallback<SetLockAction>(actionParametersOf(LockedKey to !status.locked))
                },
                colors = if (status.locked) selectedColors() else unselectedColors(),
            )
        }

        Text(
            text = status.describe(),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            modifier = GlanceModifier.padding(vertical = 4.dp),
        )

        AngleRow(status, RotationAngle.Degrees0, RotationAngle.Degrees90)
        Spacer(modifier = GlanceModifier.height(4.dp))
        AngleRow(status, RotationAngle.Degrees180, RotationAngle.Degrees270)
        Spacer(modifier = GlanceModifier.height(4.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Button(
                text = "-90°",
                onClick = status.actionOrPermission {
                    actionRunCallback<RotateAction>(actionParametersOf(StepsKey to -1))
                },
                modifier = GlanceModifier.defaultWeight(),
                colors = unselectedColors(),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Button(
                text = "+90°",
                onClick = status.actionOrPermission {
                    actionRunCallback<RotateAction>(actionParametersOf(StepsKey to 1))
                },
                modifier = GlanceModifier.defaultWeight(),
                colors = unselectedColors(),
            )
        }
    }
}

@Composable
private fun AngleRow(
    status: DeviceRotationStatus,
    first: RotationAngle,
    second: RotationAngle,
) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        listOf(first, second).forEachIndexed { index, angle ->
            if (index > 0) Spacer(modifier = GlanceModifier.width(8.dp))

            Button(
                text = "${angle.degrees}°",
                onClick = status.actionOrPermission {
                    actionRunCallback<SetAngleAction>(actionParametersOf(DegreesKey to angle.degrees))
                },
                modifier = GlanceModifier.defaultWeight(),
                colors = if (status.angle == angle) selectedColors() else unselectedColors(),
            )
        }
    }
}

// 권한이 없으면 모든 버튼이 권한 중계 Activity 를 연다. 런처가 Activity PendingIntent 를 직접 보내므로
// BroadcastReceiver 안에서 startActivity 를 부를 때 걸리는 백그라운드 시작 제한과 무관하다. 그려진
// 버튼이 낡아도 중계 Activity 가 탭 시점에 권한을 다시 보므로 설정으로 되돌아가지 않는다.
private fun DeviceRotationStatus.actionOrPermission(action: () -> Action): Action =
    if (permitted) action() else actionStartActivity<WriteSettingsPermissionActivity>()

@Composable
private fun selectedColors(): ButtonColors =
    ButtonDefaults.buttonColors(
        backgroundColor = GlanceTheme.colors.primary,
        contentColor = GlanceTheme.colors.onPrimary,
    )

@Composable
private fun unselectedColors(): ButtonColors =
    ButtonDefaults.buttonColors(
        backgroundColor = GlanceTheme.colors.secondaryContainer,
        contentColor = GlanceTheme.colors.onSecondaryContainer,
    )

// 문구는 docs/common/home-screen-widget.html#behavior 가 기준이다. 카드(:feature:rotation:ui)와 같은
// 문구지만 위젯 모듈은 ui 를 의존하지 않아 공유할 수 없다.
private fun DeviceRotationStatus.describe(): String {
    val degrees = angle?.degrees

    return when {
        !permitted -> "시스템 설정 변경 권한이 필요합니다. 누르면 권한 설정 화면이 열립니다."
        degrees == null -> "현재 각도를 읽을 수 없습니다."
        locked -> "현재 $degrees°에 고정되어 있습니다."
        else -> "현재 $degrees°, 센서를 따라 회전합니다."
    }
}

internal val DegreesKey = ActionParameters.Key<Int>("degrees")
internal val StepsKey = ActionParameters.Key<Int>("steps")
internal val LockedKey = ActionParameters.Key<Boolean>("locked")

// Glance 가 ActionCallback 과 GlanceAppWidget 을 리플렉션으로 만들어 생성자 주입이 안 된다.
// 이 모듈에서만 Koin 을 직접 꺼낸다.
internal inline fun <reified T : Any> inject(): T = GlobalContext.get().get()
