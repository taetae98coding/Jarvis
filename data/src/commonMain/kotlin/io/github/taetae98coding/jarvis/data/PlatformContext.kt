package io.github.taetae98coding.jarvis.data

/**
 * 플랫폼 핸들을 담는 상자.
 *
 * Android 저장소와 시스템 설정 접근이 `Context` 를 요구하는데, 그것을 이 모듈이 전역으로 들고
 * 있으면 초기화 순서에 의존하게 되고 테스트에서 갈아끼울 수 없다. 그래서 컨텍스트를 얻을 수 있는
 * 조립 모듈이 [DataModule] 에 넘긴다. Android 외에는 넘길 것이 없어 빈 클래스다.
 */
expect class PlatformContext
