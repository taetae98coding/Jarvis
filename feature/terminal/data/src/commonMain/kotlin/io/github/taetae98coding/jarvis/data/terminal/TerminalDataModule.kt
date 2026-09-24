package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.ClaudeActivityRepository
import io.github.taetae98coding.jarvis.domain.terminal.FileRepository
import io.github.taetae98coding.jarvis.domain.terminal.GitChangesRepository
import io.github.taetae98coding.jarvis.domain.terminal.GitWorktreeRepository
import io.github.taetae98coding.jarvis.domain.terminal.TerminalRepository
import io.github.taetae98coding.jarvis.domain.terminal.TerminalWorkspaceRepository
import org.koin.dsl.module

val terminalDataModule = module {
    single<TerminalRepository> { DefaultTerminalRepository(createTerminalDataSource(get())) }
    single<TerminalWorkspaceRepository> { DefaultTerminalWorkspaceRepository(createTerminalWorkspaceStore(get())) }
    // 워크트리와 변경이 같은 데이터 소스를 쓴다. stage·unstage 가 끝났다는 신호가 그 안에서 관측으로 간다.
    single<GitDataSource> { createGitDataSource() }
    single<GitWorktreeRepository> { DefaultGitWorktreeRepository(get()) }
    single<GitChangesRepository> { DefaultGitChangesRepository(get()) }
    single<FileRepository> { DefaultFileRepository(createFileDataSource(get())) }
    single<ClaudeActivityRepository> { DefaultClaudeActivityRepository(createClaudeActivityDataSource()) }
}
