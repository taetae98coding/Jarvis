package io.github.taetae98coding.jarvis.data.terminal

// AOSP 에는 git 이 없고, Termux 의 git 은 다른 앱 샌드박스라 띄울 수 없다(docs/platform/android.html#terminal-worktree).
internal actual fun createGitDataSource(): GitDataSource = UnsupportedGitDataSource
