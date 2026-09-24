package io.github.taetae98coding.jarvis.domain.terminal

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Claude Code 는 sessionId 로 UUID 만 받는다(`--session-id <uuid>`, `--resume <uuid>`). */
@OptIn(ExperimentalUuidApi::class)
fun newClaudeSessionId(): String = Uuid.random().toString()
