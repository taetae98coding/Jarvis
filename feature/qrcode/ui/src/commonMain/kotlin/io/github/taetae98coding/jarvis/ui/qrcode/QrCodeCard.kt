package io.github.taetae98coding.jarvis.ui.qrcode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCard
import io.github.taetae98coding.jarvis.designsystem.component.JarvisCardHeader
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.qrcode.QrCodeResult
import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

const val QrCodeTestTag = "feature:qrcode"
const val QrCodeCardImageTestTag = "qrcode:card:image"

@Composable
fun QrCodeCard(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val viewModel = koinViewModel<QrCodeCardViewModel>()
    val result by viewModel.result.collectAsStateWithLifecycle()

    QrCodeCard(
        result = result,
        onClick = { navigator.goTo(QrCodeRoute) },
        modifier = modifier.testTag(QrCodeTestTag),
    )
}

@Composable
internal fun QrCodeCard(
    result: QrCodeResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JarvisCard(onClick = onClick, modifier = modifier) {
        JarvisCardHeader(
            title = "QR 코드",
            icon = JarvisIcons.QrCode,
            trailing = { Icon(imageVector = JarvisIcons.ChevronRight, contentDescription = null) },
        )

        if (result is QrCodeResult.Ready) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QrCodeImage(
                    matrix = result.code.matrix,
                    modifier = Modifier.size(QrCodeCardDefaults.imageSize).testTag(QrCodeCardImageTestTag),
                )
                Text(
                    text = result.payload,
                    style = JarvisTheme.typography.bodySmall,
                    color = JarvisTheme.colorScheme.onSurfaceVariant,
                    maxLines = QrCodeCardDefaults.PayloadMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(
                text = "텍스트·URL·Wi-Fi·연락처를 QR 코드로 만든다. 눌러서 연다.",
                style = JarvisTheme.typography.bodySmall,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal object QrCodeCardDefaults {
    const val PayloadMaxLines = 4

    val imageSize: Dp
        @Composable @ReadOnlyComposable get() = JarvisTheme.dimens.spacing.xxl * 4
}
