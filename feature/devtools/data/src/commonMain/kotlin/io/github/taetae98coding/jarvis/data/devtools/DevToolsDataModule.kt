package io.github.taetae98coding.jarvis.data.devtools

import io.github.taetae98coding.jarvis.data.settings.createSettingsStore
import io.github.taetae98coding.jarvis.domain.devtools.DevToolsSettingsRepository
import org.koin.dsl.module

val devToolsDataModule = module {
    single<DevToolsSettingsRepository> { DefaultDevToolsSettingsRepository(createSettingsStore(get())) }
}
