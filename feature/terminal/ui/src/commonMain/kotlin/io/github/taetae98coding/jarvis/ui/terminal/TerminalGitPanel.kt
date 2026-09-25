package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import io.github.taetae98coding.jarvis.designsystem.icon.JarvisIcons
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.GitChange
import io.github.taetae98coding.jarvis.domain.terminal.GitChangeKind
import io.github.taetae98coding.jarvis.domain.terminal.GitCommit
import io.github.taetae98coding.jarvis.domain.terminal.GitGraphLine
import io.github.taetae98coding.jarvis.domain.terminal.GitPushTarget
import io.github.taetae98coding.jarvis.domain.terminal.GitStatus

const val TerminalGitNotRepositoryTestTag = "terminal:git:not-repository"
const val TerminalGitBranchTestTag = "terminal:git:branch"
const val TerminalGitErrorTestTag = "terminal:git:error"
const val TerminalGitPushTestTag = "terminal:git:push"
const val TerminalGitPushTargetTestTag = "terminal:git:push-target"
const val TerminalGitStageAllTestTag = "terminal:git:stage-all"
const val TerminalGitUnstageAllTestTag = "terminal:git:unstage-all"
const val TerminalGitGraphTestTag = "terminal:git:graph"
const val TerminalGitNoCommitsTestTag = "terminal:git:no-commits"
const val TerminalGitCommitFilesTestTag = "terminal:git:commit-files"
const val TerminalGitCommitFilesEmptyTestTag = "terminal:git:commit-files:empty"
const val TerminalGitCommitFilesUnreadableTestTag = "terminal:git:commit-files:unreadable"

fun terminalGitStagedTestTag(path: String): String = "terminal:git:staged:$path"

fun terminalGitUnstagedTestTag(path: String): String = "terminal:git:unstaged:$path"

fun terminalGitStageTestTag(path: String): String = "terminal:git:stage:$path"

fun terminalGitUnstageTestTag(path: String): String = "terminal:git:unstage:$path"

fun terminalGitCommitTestTag(hash: String): String = "terminal:git:commit:$hash"

fun terminalGitCommitFileTestTag(path: String): String = "terminal:git:commit-file:$path"

/** 위 절반은 두 변경 목록, 아래 절반은 커밋 그래프다. 둘은 따로 스크롤된다. */
@Composable
internal fun TerminalGitPanel(
    state: GitPanelState,
    graph: List<GitGraphLine>?,
    expandedCommit: ExpandedGitCommit?,
    error: String?,
    pushing: Boolean,
    onStage: (root: String, changes: List<GitChange>) -> Unit,
    onUnstage: (root: String, changes: List<GitChange>) -> Unit,
    onPush: (root: String, target: GitPushTarget) -> Unit,
    onToggleCommit: (hash: String) -> Unit,
    onOpen: (String) -> Unit,
    onOpenCommitFile: (path: String, hash: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        GitPanelState.Loading -> Box(modifier = modifier)

        GitPanelState.NoFolder -> TerminalSideBarMessage(
            text = "폴더가 없는 패널입니다",
            modifier = modifier.testTag(TerminalSideBarNoFolderTestTag),
        )

        GitPanelState.NotRepository -> TerminalSideBarMessage(
            text = "git 저장소가 아닙니다",
            modifier = modifier.testTag(TerminalGitNotRepositoryTestTag),
        )

        is GitPanelState.Loaded -> Column(modifier = modifier) {
            if (error != null) {
                Text(
                    text = error,
                    style = JarvisTheme.typography.bodySmall,
                    color = JarvisTheme.colorScheme.error,
                    modifier = Modifier.padding(JarvisTheme.dimens.spacing.s).testTag(TerminalGitErrorTestTag),
                )
            }

            GitChanges(
                status = state.status,
                pushing = pushing,
                onStage = { onStage(state.status.root, it) },
                onPush = { onPush(state.status.root, it) },
                onUnstage = { onUnstage(state.status.root, it) },
                onOpen = { onOpen("${state.status.root.trimEnd('/')}/${it.path}") },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )

            HorizontalDivider()

            GitGraph(
                lines = graph,
                expanded = expandedCommit,
                onToggleCommit = onToggleCommit,
                onOpenFile = { hash, change -> onOpenCommitFile("${state.status.root.trimEnd('/')}/${change.path}", hash) },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

@Composable
private fun GitChanges(
    status: GitStatus,
    pushing: Boolean,
    onStage: (List<GitChange>) -> Unit,
    onUnstage: (List<GitChange>) -> Unit,
    onPush: (GitPushTarget) -> Unit,
    onOpen: (GitChange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        GitBranchHeader(status = status, pushing = pushing, onPush = onPush)

        GitChangeSection(
            title = "스테이지된 변경",
            changes = status.staged,
            allLabel = "모두 unstage",
            allModifier = Modifier.testTag(TerminalGitUnstageAllTestTag),
            onAll = { onUnstage(status.staged) },
        ) { change ->
            GitChangeItem(
                change = change,
                actionIcon = JarvisIcons.Remove,
                actionLabel = "unstage",
                onAction = { onUnstage(listOf(change)) },
                onClick = { onOpen(change) },
                modifier = Modifier.testTag(terminalGitStagedTestTag(change.path)),
                actionModifier = Modifier.testTag(terminalGitUnstageTestTag(change.path)),
            )
        }

        GitChangeSection(
            title = "변경",
            changes = status.unstaged,
            allLabel = "모두 stage",
            allModifier = Modifier.testTag(TerminalGitStageAllTestTag),
            onAll = { onStage(status.unstaged) },
        ) { change ->
            GitChangeItem(
                change = change,
                actionIcon = JarvisIcons.Add,
                actionLabel = "stage",
                onAction = { onStage(listOf(change)) },
                onClick = { onOpen(change) },
                modifier = Modifier.testTag(terminalGitUnstagedTestTag(change.path)),
                actionModifier = Modifier.testTag(terminalGitStageTestTag(change.path)),
            )
        }
    }
}

@Composable
private fun GitBranchHeader(
    status: GitStatus,
    pushing: Boolean,
    onPush: (GitPushTarget) -> Unit,
) {
    val spacing = JarvisTheme.dimens.spacing
    val target = status.pushTarget

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = spacing.s, top = spacing.xs, bottom = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = JarvisIcons.GitBranch,
                    contentDescription = null,
                    modifier = Modifier.size(JarvisTheme.dimens.iconSize.small),
                    tint = JarvisTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = status.branch ?: "HEAD (detached)",
                    style = JarvisTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(TerminalGitBranchTestTag),
                )
            }
            if (target != null) {
                Text(
                    text = if (target.exists) "${target.remote}/${target.branch}" else "${target.remote} 에 없음",
                    style = JarvisTheme.typography.labelSmall,
                    color = JarvisTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = JarvisTheme.dimens.iconSize.small + spacing.xs)
                        .testTag(TerminalGitPushTargetTestTag),
                )
            }
        }

        if (target != null) {
            TextButton(
                onClick = { onPush(target) },
                enabled = !pushing && target.canPush,
                modifier = Modifier.testTag(TerminalGitPushTestTag),
            ) {
                Text(text = TerminalGitPanelDefaults.pushLabel(target, pushing), style = JarvisTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun GitChangeSection(
    title: String,
    changes: List<GitChange>,
    allLabel: String,
    allModifier: Modifier,
    onAll: () -> Unit,
    item: @Composable (GitChange) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = JarvisTheme.dimens.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$title (${changes.size})",
            style = JarvisTheme.typography.labelMedium,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (changes.isNotEmpty()) {
            TextButton(onClick = onAll, modifier = allModifier) {
                Text(text = allLabel, style = JarvisTheme.typography.labelSmall)
            }
        }
    }

    changes.forEach { item(it) }
}

@Composable
private fun GitChangeItem(
    change: GitChange,
    actionIcon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionModifier: Modifier = Modifier,
) {
    val spacing = JarvisTheme.dimens.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(JarvisTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(start = spacing.s, end = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GitChangeLabel(change = change, modifier = Modifier.weight(1f))
        Box(
            modifier = actionModifier
                .clip(JarvisTheme.shapes.small)
                .clickable(onClick = onAction)
                .padding(spacing.xs),
        ) {
            Icon(imageVector = actionIcon, contentDescription = actionLabel, modifier = Modifier.size(JarvisTheme.dimens.iconSize.small))
        }
    }
}

/** 상태 글자, 파일 이름, 상위 폴더. 변경 줄과 커밋의 파일 줄이 같이 쓴다. */
@Composable
private fun GitChangeLabel(change: GitChange, modifier: Modifier = Modifier) {
    val spacing = JarvisTheme.dimens.spacing
    val name = change.path.substringAfterLast('/')
    val parent = change.path.substringBeforeLast('/', missingDelimiterValue = "")

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(spacing.s), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = change.kind.symbol.toString(),
            style = JarvisTheme.codeTextStyle,
            color = TerminalGitPanelDefaults.kindColor(change.kind),
        )
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, style = JarvisTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (parent.isNotEmpty()) {
                Text(
                    text = parent,
                    style = JarvisTheme.typography.labelSmall,
                    color = JarvisTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}

@Composable
private fun GitGraph(
    lines: List<GitGraphLine>?,
    expanded: ExpandedGitCommit?,
    onToggleCommit: (hash: String) -> Unit,
    onOpenFile: (hash: String, change: GitChange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "커밋 그래프",
            style = JarvisTheme.typography.labelMedium,
            color = JarvisTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(JarvisTheme.dimens.spacing.s),
        )

        if (lines == null) return@Column
        if (lines.isEmpty()) {
            TerminalSideBarMessage(text = "커밋이 없습니다", modifier = Modifier.testTag(TerminalGitNoCommitsTestTag))
            return@Column
        }

        // 가지가 많아 선 그림이 넓어지면 제목을 말줄임하지 않고 줄 전체를 가로로 민다.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .testTag(TerminalGitGraphTestTag),
        ) {
            // 펼친 커밋의 파일 목록은 그 커밋의 둘째 줄 바로 아래 한 항목이다(R25).
            lines.forEach { line ->
                val commit = line.commit
                item {
                    GitGraphItem(
                        line = line,
                        selected = commit != null && commit.hash == expanded?.hash,
                        onClick = commit?.let { { onToggleCommit(it.hash) } },
                    )
                }
                if (line.isDetail && expanded != null && commit?.hash == expanded.hash) {
                    item { GitCommitFiles(state = expanded.files, onOpen = { onOpenFile(expanded.hash, it) }) }
                }
            }
        }
    }
}

@Composable
private fun GitCommitFiles(state: GitCommitFilesState, onOpen: (GitChange) -> Unit) {
    val spacing = JarvisTheme.dimens.spacing

    // 그래프 줄과 달리 사이드 바 폭에 맞춰 긴 경로를 말줄임한다. 가로 스크롤 안에서는 폭을 정해야 말줄임이 된다.
    Column(
        modifier = Modifier
            .width(TerminalSideBarDefaults.width)
            .padding(start = spacing.l, end = spacing.s, bottom = spacing.xs)
            .testTag(TerminalGitCommitFilesTestTag),
    ) {
        when (state) {
            GitCommitFilesState.Loading -> Unit

            GitCommitFilesState.Unreadable -> Text(
                text = "커밋을 읽을 수 없습니다",
                style = JarvisTheme.typography.labelMedium,
                color = JarvisTheme.colorScheme.error,
                modifier = Modifier.testTag(TerminalGitCommitFilesUnreadableTestTag),
            )

            is GitCommitFilesState.Loaded -> {
                Text(
                    text = if (state.files.isEmpty()) "바뀐 파일이 없습니다" else "파일 ${state.files.size}개",
                    style = JarvisTheme.typography.labelMedium,
                    color = JarvisTheme.colorScheme.onSurfaceVariant,
                    modifier = if (state.files.isEmpty()) Modifier.testTag(TerminalGitCommitFilesEmptyTestTag) else Modifier,
                )
                state.files.forEach { change ->
                    GitChangeLabel(
                        change = change,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(JarvisTheme.shapes.small)
                            .clickable { onOpen(change) }
                            .padding(horizontal = spacing.xs, vertical = spacing.xxs)
                            .testTag(terminalGitCommitFileTestTag(change.path)),
                    )
                }
            }
        }
    }
}

@Composable
private fun GitGraphItem(line: GitGraphLine, selected: Boolean, onClick: (() -> Unit)?) {
    val commit = line.commit
    val graph = remember(line.graph) { graphText(line.graph) }
    val spacing = JarvisTheme.dimens.spacing

    Row(
        modifier = Modifier
            .widthIn(min = TerminalSideBarDefaults.width)
            .then(if (selected) Modifier.background(JarvisTheme.colorScheme.surfaceVariant) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = spacing.s)
            .then(if (commit != null && !line.isDetail) Modifier.testTag(terminalGitCommitTestTag(commit.hash)) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = graph, style = JarvisTheme.codeTextStyle, softWrap = false)

        when {
            commit == null -> Unit
            line.isDetail -> Text(
                text = "${commit.shortHash} · ${commit.author} · ${commit.date}",
                style = JarvisTheme.typography.labelSmall,
                color = JarvisTheme.colorScheme.onSurfaceVariant,
                softWrap = false,
            )
            else -> GitCommitTitle(commit)
        }
    }
}

@Composable
private fun GitCommitTitle(commit: GitCommit) {
    commit.refs.forEach { ref ->
        val head = ref == "HEAD" || ref.startsWith("HEAD -> ")
        Text(
            text = ref,
            style = JarvisTheme.typography.labelSmall,
            color = if (head) JarvisTheme.colorScheme.onPrimaryContainer else JarvisTheme.colorScheme.onSecondaryContainer,
            softWrap = false,
            modifier = Modifier
                .background(if (head) JarvisTheme.colorScheme.primaryContainer else JarvisTheme.colorScheme.secondaryContainer, JarvisTheme.shapes.small)
                .padding(horizontal = JarvisTheme.dimens.spacing.xs),
        )
    }
    Text(
        text = commit.subject,
        style = JarvisTheme.typography.bodySmall,
        fontWeight = if (commit.isHead) FontWeight.Bold else null,
        softWrap = false,
    )
}

// git 은 가지 하나를 글자 두 칸(선과 빈칸)에 그린다. 칸마다 색을 돌려 가지를 구분한다.
private fun graphText(graph: String): AnnotatedString =
    buildAnnotatedString {
        graph.forEachIndexed { index, char ->
            if (char == ' ') {
                append(char)
            } else {
                withStyle(SpanStyle(color = TerminalGitPanelDefaults.laneColors[(index / 2) % TerminalGitPanelDefaults.laneColors.size])) { append(char) }
            }
        }
    }

internal object TerminalGitPanelDefaults {
    fun pushLabel(target: GitPushTarget, pushing: Boolean): String =
        when {
            pushing -> "push 중…"
            !target.exists -> "새 브랜치 push"
            target.ahead == 0 -> "push"
            target.behind > 0 -> "push ↑${target.ahead} ↓${target.behind}"
            else -> "push ↑${target.ahead}"
        }

    // 테마와 무관한 고정 색이다. 밝은·어두운 배경 모두에서 서로 구분되는 색을 골랐다.
    val laneColors: List<Color> = listOf(
        Color(0xFF3B8EEA),
        Color(0xFFD97757),
        Color(0xFF34A853),
        Color(0xFFA66CFF),
        Color(0xFFE0A800),
        Color(0xFF00A3A3),
    )

    @Composable
    fun kindColor(kind: GitChangeKind): Color =
        when (kind) {
            GitChangeKind.Added, GitChangeKind.Untracked -> JarvisTheme.colors.success
            GitChangeKind.Deleted, GitChangeKind.Conflicted -> JarvisTheme.colorScheme.error
            GitChangeKind.Modified, GitChangeKind.Renamed, GitChangeKind.Copied, GitChangeKind.TypeChanged -> JarvisTheme.colors.warning
        }
}
