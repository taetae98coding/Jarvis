package io.github.taetae98coding.jarvis.data.emulator

import io.github.taetae98coding.jarvis.automation.DeviceAutomation

/**
 * Claude 의 기기 도구(docs/common/mcp-server.html). SDK 도구를 직접 부를 수 있는 JVM 에만 있고, 나머지 타깃은 null 이라
 * Koin 에 등록되지 않는다. [emulatorDataSource] 처럼 프로세스 싱글턴이다 — 기기 세션을 붙잡는 임대가 한 벌이어야 한다.
 */
internal expect val deviceAutomation: DeviceAutomation?
