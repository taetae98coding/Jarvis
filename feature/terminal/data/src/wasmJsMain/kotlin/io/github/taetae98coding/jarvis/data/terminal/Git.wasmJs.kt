package io.github.taetae98coding.jarvis.data.terminal

// 브라우저 페이지는 로컬 프로세스를 띄울 수 없다(docs/platform/web.html#terminal-worktree).
internal actual fun createGitDataSource(): GitDataSource = UnsupportedGitDataSource
