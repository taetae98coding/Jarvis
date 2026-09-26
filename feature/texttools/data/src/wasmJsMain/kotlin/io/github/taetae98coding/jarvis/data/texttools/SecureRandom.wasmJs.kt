package io.github.taetae98coding.jarvis.data.texttools

// crypto.getRandomValues 는 crypto.subtle 과 달리 보안 컨텍스트가 아니어도 있다. Int32Array 라 값이 Wasm i32 로 그대로 온다.
internal actual fun secureRandomInt(): Int = js("globalThis.crypto.getRandomValues(new Int32Array(1))[0]")
