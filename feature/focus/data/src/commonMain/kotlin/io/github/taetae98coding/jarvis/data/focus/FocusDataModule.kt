package io.github.taetae98coding.jarvis.data.focus

import io.github.taetae98coding.jarvis.data.settings.createSettingsStore
import io.github.taetae98coding.jarvis.domain.focus.FocusAlarmRepository
import io.github.taetae98coding.jarvis.domain.focus.FocusClock
import io.github.taetae98coding.jarvis.domain.focus.FocusSessionRepository
import org.koin.dsl.module

val focusDataModule = module {
    single<FocusSessionRepository> { DefaultFocusSessionRepository(createSettingsStore(get())) }

    single<FocusAlarmRepository> { createFocusAlarm(get()) }

    single<FocusClock> { SystemFocusClock }
}
