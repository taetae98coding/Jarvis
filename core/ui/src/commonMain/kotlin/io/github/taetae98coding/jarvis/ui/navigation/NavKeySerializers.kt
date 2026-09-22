package io.github.taetae98coding.jarvis.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.named

/**
 * 기능이 자기 라우트 키를 백스택 저장에 등록하는 수단.
 *
 * `rememberNavBackStack` 은 `polymorphic(NavKey::class)` 등록을 요구한다. 그 목록을 앱 셸에 두면
 * 화면을 더할 때마다 셸을 고쳐야 하므로, 기능이 Koin 에 하나씩 등록하고 셸이 `getAll` 로 모은다.
 */
class NavKeySerializers(
    val register: PolymorphicModuleBuilder<NavKey>.() -> Unit,
)

/**
 * [T] 는 기능의 라우트 타입이다. Koin 은 같은 타입을 한정자 없이 두 번 등록하면 뒤엣것으로
 * 덮어쓰므로, 기능마다 다른 한정자가 필요하다.
 */
inline fun <reified T : Any> Module.navKeySerializers(
    noinline register: PolymorphicModuleBuilder<NavKey>.() -> Unit,
): KoinDefinition<NavKeySerializers> = single(named<T>()) { NavKeySerializers(register) }
