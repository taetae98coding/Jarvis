package io.github.taetae98coding.jarvis.domain.theme

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ApplyThemeModeUseCaseTest {
    @Test
    fun followsTheSettingAndKeepsItWhenCancelled() = runTest {
        val settings = FakeThemeSettingsRepository()
        val applied = mutableListOf<ThemeMode>()
        val useCase = ApplyThemeModeUseCase(settings, ThemeAppearanceRepository { applied += it })

        val job = launch { useCase() }
        runCurrent()
        settings.setThemeMode(ThemeMode.DARK)
        runCurrent()
        job.cancel()
        runCurrent()

        assertEquals(listOf(ThemeMode.SYSTEM, ThemeMode.DARK), applied)
    }
}
