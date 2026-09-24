package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.TerminalRepository
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspaceRepository
import org.koin.dsl.module

val terminalDataModule = module {
    single<TerminalRepository> { DefaultTerminalRepository(createTerminalDataSource(get())) }
    single<TerminalWorkspaceRepository> { DefaultTerminalWorkspaceRepository(createTerminalWorkspaceStore(get())) }
}
