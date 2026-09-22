package io.github.taetae98coding.jarvis.data.emulator.agent

import kotlinx.serialization.json.Json

internal val HostAgentJson = Json {
    // 에이전트와 클라이언트의 버전이 어긋날 수 있다. 서버가 필드를 더해도 옛 클라이언트가 읽을 수
    // 있도록 모르는 키는 넘긴다.
    ignoreUnknownKeys = true
    // 0개를 생략하면 응답만 보고는 "0개" 와 "셀 수 없음(null)" 을 가릴 수 없다. 사람이 curl 로 들여다볼
    // 엔드포인트라 값을 다 적는다.
    encodeDefaults = true
}
