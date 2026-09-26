package io.github.taetae98coding.jarvis.data.texttools

import io.github.taetae98coding.jarvis.data.settings.createSettingsStore
import io.github.taetae98coding.jarvis.domain.texttools.SecureRandomSource
import io.github.taetae98coding.jarvis.domain.texttools.TextToolsSettingsRepository
import org.koin.dsl.module

val textToolsDataModule = module {
    single<TextToolsSettingsRepository> { DefaultTextToolsSettingsRepository(createSettingsStore(get())) }
    single<SecureRandomSource> { PlatformSecureRandomSource }
}
