package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.GitBranch

private const val LocalPrefix = "refs/heads/"
private const val RemotePrefix = "refs/remotes/"

/**
 * `git for-each-ref --format=%(refname)%00%(symref) refs/heads refs/remotes` 의 출력. 줄마다 ref 이름과 심볼릭 ref 의
 * 대상이 NUL 로 갈려 있고, 대상이 있는 것(`refs/remotes/origin/HEAD`)은 뺀다. 로컬을 앞으로 모으되 각 무리 안의 순서는
 * git 이 준 그대로다. 원격 이름은 [remotes] 중 겹치는 가장 긴 것이고, 없으면 첫 `/` 앞이다.
 */
internal fun parseGitBranches(output: String, remotes: List<String>): List<GitBranch> {
    val branches = output.lineSequence().mapNotNull { line ->
        val fields = line.split('\u0000')
        val ref = fields[0].trim()
        if (fields.getOrNull(1).orEmpty().isNotBlank()) return@mapNotNull null

        when {
            ref.startsWith(LocalPrefix) -> GitBranch(ref.removePrefix(LocalPrefix))
            ref.startsWith(RemotePrefix) -> {
                val name = ref.removePrefix(RemotePrefix)
                val remote = remotes.filter { name.startsWith("$it/") }.maxByOrNull { it.length }
                    ?: name.substringBefore('/').takeIf { it != name }
                    ?: return@mapNotNull null
                GitBranch(name, remote)
            }
            else -> null
        }
    }.toList()

    return branches.filter { it.remote == null } + branches.filter { it.remote != null }
}
