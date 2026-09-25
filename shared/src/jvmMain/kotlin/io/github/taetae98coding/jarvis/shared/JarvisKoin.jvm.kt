package io.github.taetae98coding.jarvis.shared

import io.github.taetae98coding.jarvis.data.PlatformContext
import io.github.taetae98coding.jarvis.domain.theme.ThemeAppearanceRepository
import io.github.taetae98coding.jarvis.domain.theme.ThemeSettingsRepository
import org.koin.mp.KoinPlatformTools

fun startJarvisKoin() {
    startJarvisKoinWith(PlatformContext())

    // macOS 창 제목 막대의 외관은 AWT 가 뜰 때 시스템 프로퍼티를 한 번만 읽는다. 첫 Window 보다 먼저
    // 저장된 모드를 적용해 둔다(docs/platform/jvm.html#theme-mode). 이 함수 앞에서 AWT 를 건드리면 안 된다.
    val koin = KoinPlatformTools.defaultContext().get()
    koin.get<ThemeAppearanceRepository>().applyThemeMode(koin.get<ThemeSettingsRepository>().readThemeMode())
}
