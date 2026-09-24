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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import io.github.taetae98coding.jarvis.domain.terminal.GitStatus

const val TerminalGitNotRepositoryTestTag = "terminal:git:not-repository"
const val TerminalGitBranchTestTag = "terminal:git:branch"
const val TerminalGitErrorTestTag = "terminal:git:error"
const val TerminalGitStageAllTestTag = "terminal:git:stage-all"
const val TerminalGitUnstageAllTestTag = "terminal:git:unstage-all"
const val TerminalGitGraphTestTag = "terminal:git:graph"
const val TerminalGitNoCommitsTestTag = "terminal:git:no-commits"

fun terminalGitStagedTestTag(path: String): String = "terminal:git:staged:$path"

fun terminalGitUnstagedTestTag(path: String): String = "terminal:git:unstaged:$path"

fun terminalGitStageTestTag(path: String): String = "terminal:git:stage:$path"

fun terminalGitUnstageTestTag(path: String): String = "terminal:git:unstage:$path"

fun terminalGitCommitTestTag(hash: String): String = "terminal:git:commit:$hash"

/** 위 절반은 두 변경 목록, 아래 절반은 커밋 그래프다. 둘은 따로 스크롤된다. */
@Composable
internal fun TerminalGitPanel(
    state: GitPanelState,
    graph: List<GitGraphLine>?,
    error: String?,
    onStage: (root: String, changes: List<GitChange>) -> Unit,
    onUnstage: (root: String, changes: List<GitChange>) -> Unit,
    onOpen: (String) -> Unit,
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
                onStage = { onStage(state.status.root, it) },
                onUnstage = { onUnstage(state.status.root, it) },
                onOpen = { onOpen("${state.status.root.trimEnd('/')}/${it.path}") },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )

            HorizontalDivider()

            GitGraph(lines = graph, modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun GitChanges(
    status: GitStatus,
    onStage: (List<GitChange>) -> Unit,
    onUnstage: (List<GitChange>) -> Unit,
    onOpen: (GitChange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.padding(JarvisTheme.dimens.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
    val name = change.path.substringAfterLast('/')
    val parent = change.path.substringBeforeLast('/', missingDelimiterValue = "")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(JarvisTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(start = spacing.s, end = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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

@Composable
private fun GitGraph(lines: List<GitGraphLine>?, modifier: Modifier = Modifier) {
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
            items(lines) { line -> GitGraphItem(line) }
        }
    }
}

@Composable
private fun GitGraphItem(line: GitGraphLine) {
    val commit = line.commit
    val graph = remember(line.graph) { graphText(line.graph) }
    val spacing = JarvisTheme.dimens.spacing

    Row(
        modifier = Modifier
            .widthIn(min = TerminalSideBarDefaults.width)
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
