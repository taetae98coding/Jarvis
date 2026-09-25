package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.AndroidProject
import io.github.taetae98coding.jarvis.domain.terminal.AndroidRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.IosProject
import io.github.taetae98coding.jarvis.domain.terminal.IosRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.ProjectKind
import io.github.taetae98coding.jarvis.domain.terminal.ProjectLoad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal interface ProjectRunDataSource {
    val isSupported: Boolean

    fun observeProjectKinds(directory: String): Flow<Set<ProjectKind>>

    fun observeAndroidProject(directory: String): Flow<ProjectLoad<AndroidProject>>

    fun observeIosProject(directory: String): Flow<ProjectLoad<IosProject>>

    suspend fun androidRunCommand(request: AndroidRunRequest): String

    suspend fun iosRunCommand(request: IosRunRequest): String
}

// Gradle·Xcode·SDK 가 없는 타깃이다. 실행 메뉴에 프로젝트 줄이 나오지 않는다(docs/common/terminal-run.html#platforms).
internal object UnsupportedProjectRunDataSource : ProjectRunDataSource {
    override val isSupported: Boolean = false

    override fun observeProjectKinds(directory: String): Flow<Set<ProjectKind>> = flowOf(emptySet())

    override fun observeAndroidProject(directory: String): Flow<ProjectLoad<AndroidProject>> = flowOf(ProjectLoad.Failed())

    override fun observeIosProject(directory: String): Flow<ProjectLoad<IosProject>> = flowOf(ProjectLoad.Failed())

    override suspend fun androidRunCommand(request: AndroidRunRequest): String = ""

    override suspend fun iosRunCommand(request: IosRunRequest): String = ""
}

/** JVM(macOS)만 Gradle·xcodebuild 를 돌린다. 판정 근거는 docs/common/terminal-run.html#platforms 에 있다. */
internal expect fun createProjectRunDataSource(): ProjectRunDataSource
