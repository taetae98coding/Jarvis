package io.github.taetae98coding.jarvis.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import io.github.taetae98coding.jarvis.ui.navigation.NavKeySerializers
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.currentKoinScope

/**
 * 백스택을 저장할 때 쓰는 직렬화 설정.
 *
 * `rememberNavBackStack` 은 `PolymorphicSerializer(NavKey::class)` 로 저장하고, `NavKey` 는 sealed 가
 * 아니라서 구현을 직접 등록해 줘야 한다. 셸은 자기 [HomeRoute] 만 알고 나머지는 기능들이 Koin 에
 * 등록해 둔 [NavKeySerializers] 를 모아서 채운다.
 */
@Composable
internal fun rememberJarvisSavedStateConfiguration(): SavedStateConfiguration {
    val scope = currentKoinScope()

    return remember(scope) {
        val registrations = scope.getAll<NavKeySerializers>()

        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(HomeRoute::class, HomeRoute.serializer())

                    registrations.forEach { it.register(this) }
                }
            }
        }
    }
}
