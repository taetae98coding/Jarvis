package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.AndroidProject
import io.github.taetae98coding.jarvis.domain.terminal.AndroidRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.IosProject
import io.github.taetae98coding.jarvis.domain.terminal.IosRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.ProjectKind
import io.github.taetae98coding.jarvis.domain.terminal.ProjectLoad
import io.github.taetae98coding.jarvis.domain.terminal.ProjectRunRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultProjectRunRepository(
    private val dataSource: ProjectRunDataSource,
) : ProjectRunRepository {
    override val isSupported: Boolean get() = dataSource.isSupported

    override fun observeProjectKinds(directory: String): Flow<Set<ProjectKind>> = dataSource.observeProjectKinds(directory)

    override fun observeAndroidProject(directory: String): Flow<ProjectLoad<AndroidProject>> = dataSource.observeAndroidProject(directory)

    override fun observeIosProject(directory: String): Flow<ProjectLoad<IosProject>> = dataSource.observeIosProject(directory)

    override suspend fun androidRunCommand(request: AndroidRunRequest): String = dataSource.androidRunCommand(request)

    override suspend fun iosRunCommand(request: IosRunRequest): String = dataSource.iosRunCommand(request)
}
