package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.taetae98coding.jarvis.ui.navigation.LocalNavigator
import io.github.taetae98coding.jarvis.ui.navigation.Navigator
import io.github.taetae98coding.jarvis.ui.screen.appScreenAwake
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun JarvisApp(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(rememberJarvisSavedStateConfiguration(), HomeRoute)
    val navigator = remember(backStack) { Navigator(backStack) }

    MaterialTheme {
        // 루트 Surface 에 붙여서 특정 화면의 수명과 무관하게 효과가 유지되도록 한다. 무엇을
        // 적용하는지는 :feature:screen:ui 가 안다.
        Surface(
            modifier = modifier
                .fillMaxSize()
                .appScreenAwake(),
        ) {
            CompositionLocalProvider(LocalNavigator provides navigator) {
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding()
                        .padding(16.dp),
                    onBack = navigator::back,
                    entryDecorators = listOf(
                        // NavDisplay 의 기본값이다. 목록을 직접 주면 기본값이 사라지므로 함께 적는다.
                        rememberSaveableStateHolderNavEntryDecorator(),
                        // 엔트리마다 ViewModelStore 를 만들고 백스택에서 빠질 때 비운다. 별도
                        // 아티팩트에 있어서 nav3 가 기본값으로 넣을 수 없다.
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    // 어떤 화면이 있는지는 기능들의 Koin 모듈이 안다.
                    entryProvider = koinEntryProvider(),
                )
            }
        }
    }
}
