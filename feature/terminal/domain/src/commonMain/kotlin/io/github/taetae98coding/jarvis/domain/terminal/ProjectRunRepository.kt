package io.github.taetae98coding.jarvis.domain.terminal

import kotlinx.coroutines.flow.Flow

/** 폴더의 Android·iOS 앱을 알아보고 실행 탭에 넣을 명령 줄을 만든다(docs/common/terminal-run.html). */
interface ProjectRunRepository {
    /** 이 타깃에서 앱을 빌드·실행할 수 있는지. 실행 중에 바뀌지 않는다. */
    val isSupported: Boolean

    /** [directory] 가 어떤 프로젝트인지 파일만 보고 판정한다(R4). 수집할 때 한 번 읽는다(cold). */
    fun observeProjectKinds(directory: String): Flow<Set<ProjectKind>>

    /** Gradle 에 물어 앱 모듈과 빌드 변형을 읽는다(R6). [ProjectLoad.Loading] 으로 시작한다. */
    fun observeAndroidProject(directory: String): Flow<ProjectLoad<AndroidProject>>

    /** `xcodebuild -list` 로 스킴과 구성을 읽는다(R12). [ProjectLoad.Loading] 으로 시작한다. */
    fun observeIosProject(directory: String): Flow<ProjectLoad<IosProject>>

    /** 실행 탭이 돌릴 빌드·설치·실행 명령 줄(R8). */
    suspend fun androidRunCommand(request: AndroidRunRequest): String

    /** 실행 탭이 돌릴 빌드·설치·실행 명령 줄(R13). */
    suspend fun iosRunCommand(request: IosRunRequest): String
}
