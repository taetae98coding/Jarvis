package io.github.taetae98coding.jarvis.domain.mcp

import org.koin.dsl.module

// 이음새는 기능·타깃마다 있을 수도 없을 수도 있다. 없는 쪽의 도구는 목록에서 빠진다.
val mcpDomainModule = module {
    single { McpToolbox(tabs = getOrNull(), browser = getOrNull(), devices = getOrNull()) }
}
