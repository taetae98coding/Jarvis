package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class IsProjectRunSupportedUseCase(
    private val repository: ProjectRunRepository,
) {
    operator fun invoke(): Boolean = repository.isSupported
}

/** 폴더가 없거나 실행을 지원하지 않는 타깃이면 빈 집합이다(R2·R3). */
class ObserveProjectKindsUseCase(
    private val repository: ProjectRunRepository,
) {
    operator fun invoke(directory: String?): Flow<Set<ProjectKind>> =
        if (directory == null || !repository.isSupported) flowOf(emptySet()) else repository.observeProjectKinds(directory)
}

class ObserveAndroidProjectUseCase(
    private val repository: ProjectRunRepository,
) {
    operator fun invoke(directory: String): Flow<ProjectLoad<AndroidProject>> =
        if (repository.isSupported) repository.observeAndroidProject(directory) else flowOf(ProjectLoad.Failed())
}

class ObserveIosProjectUseCase(
    private val repository: ProjectRunRepository,
) {
    operator fun invoke(directory: String): Flow<ProjectLoad<IosProject>> =
        if (repository.isSupported) repository.observeIosProject(directory) else flowOf(ProjectLoad.Failed())
}

/** 선택을 [panelId] 패널 가족에 저장하고 [groupId] 그룹에 실행 탭(과 기기 탭)을 연다(R8·R9·R10·R17). */
class RunAndroidAppUseCase(
    private val repository: ProjectRunRepository,
    private val updateWorkspace: UpdateTerminalWorkspaceUseCase,
) {
    suspend operator fun invoke(panelId: Long, groupId: Long?, request: AndroidRunRequest): TerminalWorkspaceChange? {
        if (!repository.isSupported) return null

        val command = repository.androidRunCommand(request)
        val choice = AndroidRunChoice(request.modulePath, request.variant.name, request.device.id)
        val title = "${request.variant.name} · ${request.device.name}"

        return updateWorkspace {
            it.rememberAndroidRun(panelId, choice).runInGroup(groupId, request.directory, command, title, request.device.mirror())
        }
    }
}

/** [RunAndroidAppUseCase] 의 iOS 판(R13). */
class RunIosAppUseCase(
    private val repository: ProjectRunRepository,
    private val updateWorkspace: UpdateTerminalWorkspaceUseCase,
) {
    suspend operator fun invoke(panelId: Long, groupId: Long?, request: IosRunRequest): TerminalWorkspaceChange? {
        if (!repository.isSupported) return null

        val command = repository.iosRunCommand(request)
        val choice = IosRunChoice(request.scheme, request.configuration, request.device.id)
        val title = "${request.configuration} · ${request.device.name}"

        return updateWorkspace {
            it.rememberIosRun(panelId, choice).runInGroup(groupId, request.project.directory, command, title, request.device.mirror())
        }
    }
}
