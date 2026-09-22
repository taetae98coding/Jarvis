package io.github.taetae98coding.jarvis.shared

import io.github.taetae98coding.jarvis.data.emulator.agent.startEmulatorHostAgent as startAgent

/**
 * 데스크탑 앱이 같은 머신의 다른 타깃에 에뮬레이터 개수를 넘겨 주는 로컬 HTTP 에이전트를 띄운다.
 *
 * 구현은 :data 에 있다. 앱 모듈이 :shared 하나만 의존하도록 여기서 한 번 감싼다.
 */
fun startEmulatorHostAgent(): AutoCloseable = startAgent()
