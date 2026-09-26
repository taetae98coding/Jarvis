package io.github.taetae98coding.jarvis.ui.qrcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.ui.home.HomeFeature

const val QrCodeTitle = "QR 코드"

/** 공통 코드만으로 동작해 모든 플랫폼이 지원한다. 타일은 카드가 눌렸을 때와 같은 화면을 연다. */
object QrCodeHomeFeature : HomeFeature {
    override val id: String = "qrCode"
    override val title: String = QrCodeTitle
    override val icon: ImageVector = JarvisIcons.QrCode
    override val route: NavKey = QrCodeRoute

    @Composable
    override fun isSupported(): Boolean = true

    @Composable
    override fun HomeCard(modifier: Modifier) {
        QrCodeCard(modifier = modifier)
    }
}
