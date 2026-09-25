package io.github.taetae98coding.jarvis.data.profiling

import io.github.taetae98coding.jarvis.domain.profiling.ProfilingRepository
import org.koin.dsl.module

val profilingDataModule = module {
    single<ProfilingRepository> { DefaultProfilingRepository(createProfilingSource(get())) }
}
