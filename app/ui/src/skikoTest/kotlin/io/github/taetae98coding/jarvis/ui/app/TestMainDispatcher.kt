package io.github.taetae98coding.jarvis.ui.app

/**
 * `viewModelScope` 와 `collectAsStateWithLifecycle` 이 쓰는 `Dispatchers.Main` 을 화면 테스트가 기다릴 수 있는
 * 곳에 둔다.
 *
 * JVM 의 화면 테스트는 Swing EDT 가 아닌 스레드에서 컴포지션을 돌린다. 앱처럼 Main 을 EDT 로 두면
 * ViewModel 의 상태 전파와 클릭 처리가 테스트가 기다리지 않는 스레드에서 일어나 단언을 앞지른다.
 */
internal expect fun installTestMainDispatcher()
