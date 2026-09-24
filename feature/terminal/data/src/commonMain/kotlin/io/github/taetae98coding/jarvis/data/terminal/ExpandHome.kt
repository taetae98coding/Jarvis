package io.github.taetae98coding.jarvis.data.terminal

/**
 * 사용자가 친 폴더의 `~`, `~/…` 를 [home] 기준으로 편다. 셸이 아니라 프로세스의 작업 디렉터리로 넘기는
 * 경로라 셸이 대신 펴 주지 않는다. `~user` 는 다른 사용자의 홈이라 그대로 둔다.
 */
internal fun expandHome(path: String, home: String): String =
    when {
        path == "~" -> home
        path.startsWith("~/") -> home.trimEnd('/') + path.substring(1)
        else -> path
    }
