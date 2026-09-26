package io.github.taetae98coding.jarvis.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.taetae98coding.jarvis.designsystem.component.JarvisTopBar
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme

/**
 * 홈 타일이 여는 기능 화면의 틀. 뒤로 버튼이 있는 상단 막대 아래에 [content] 를 세로로 놓고 스크롤한다.
 *
 * 기능 화면은 홈 카드와 같은 컨트롤을 담는다. 카드와 화면이 상태를 따로 갖지 않게 [content] 에 그 기능의
 * 카드 컴포저블을 그대로 넣는다(docs/common/home-adaptive-layout.html#implementation).
 */
@Composable
fun HomeFeatureScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        JarvisTopBar(title = title, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
            content = content,
        )
    }
}
