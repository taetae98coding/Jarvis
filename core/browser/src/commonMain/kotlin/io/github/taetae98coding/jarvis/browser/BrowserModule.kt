package io.github.taetae98coding.jarvis.browser

import org.koin.core.module.Module

/** 브라우저 엔진이 있는 타깃(JVM 의 macOS)만 `BrowserAutomation` 을 등록한다(docs/common/mcp-server.html#modules). */
expect val browserModule: Module
