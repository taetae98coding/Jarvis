package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.taetae98coding.jarvis.designsystem.component.JarvisFastScroller
import io.github.taetae98coding.jarvis.designsystem.component.rememberFastScrollerAdapter
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownAlignment
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownBlock
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownInline
import io.github.taetae98coding.jarvis.domain.terminal.parseMarkdown
import io.github.taetae98coding.jarvis.domain.terminal.syntaxLanguageOfName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val TerminalFileViewerMarkdownTestTag = "terminal:file-viewer:markdown"

/** 인라인을 글로 바꿀 때 쓰는 색·링크 동작. */
private class MarkdownStyles(
    val link: TextLinkStyles,
    val code: SpanStyle,
    val onLink: (String) -> Unit,
)

/**
 * 마크다운 미리보기(docs/common/terminal-file-editor.html M2–M4). 해석은 기본 디스패처에서 하고, 끝날 때까지 이전 결과를
 * 보인다. [onLink] 는 누른 링크의 주소를 그대로 받는다.
 */
@Composable
internal fun MarkdownPreview(text: String, onLink: (String) -> Unit, modifier: Modifier = Modifier) {
    val blocks by produceState<List<MarkdownBlock>?>(initialValue = null, text) {
        value = withContext(Dispatchers.Default) { parseMarkdown(text) }
    }
    val listState = rememberLazyListState()
    val palette = syntaxPalette()
    val currentOnLink by rememberUpdatedState(onLink)
    val linkColor = JarvisTheme.colorScheme.primary
    val codeFont = JarvisTheme.codeTextStyle.fontFamily
    val codeBackground = JarvisTheme.colorScheme.surfaceContainerHighest
    val styles = remember(linkColor, codeFont, codeBackground) {
        MarkdownStyles(
            link = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
            code = SpanStyle(fontFamily = codeFont, background = codeBackground),
            onLink = { currentOnLink(it) },
        )
    }

    Box(modifier = modifier.testTag(TerminalFileViewerMarkdownTestTag)) {
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = JarvisTheme.dimens.spacing.l, vertical = JarvisTheme.dimens.spacing.m),
                verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.m),
            ) {
                itemsIndexed(blocks.orEmpty()) { _, block ->
                    MarkdownBlockView(block = block, styles = styles, palette = palette, depth = 0)
                }
            }
        }
        JarvisFastScroller(
            adapter = rememberFastScrollerAdapter(listState),
            modifier = Modifier.align(Alignment.CenterEnd).testTag(TerminalFileViewerScrollerTestTag),
        )
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock, styles: MarkdownStyles, palette: SyntaxPalette, depth: Int) {
    when (block) {
        is MarkdownBlock.Heading -> Column(verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs)) {
            val style = when (block.level) {
                1 -> JarvisTheme.typography.headlineSmall
                2 -> JarvisTheme.typography.titleLarge
                3 -> JarvisTheme.typography.titleMedium
                else -> JarvisTheme.typography.titleSmall
            }
            InlineText(block.content, styles, style.copy(fontWeight = FontWeight.SemiBold))
            if (block.level <= 2) HorizontalDivider(color = JarvisTheme.colorScheme.outlineVariant)
        }

        is MarkdownBlock.Paragraph -> InlineText(block.content, styles, JarvisTheme.typography.bodyMedium)

        is MarkdownBlock.CodeBlock -> CodeBlock(block, palette)

        is MarkdownBlock.Quote -> {
            val bar = JarvisTheme.colorScheme.outlineVariant
            CompositionLocalProvider(LocalContentColor provides JarvisTheme.colorScheme.onSurfaceVariant) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.s),
                    modifier = Modifier
                        .drawBehind { drawRect(bar, size = Size(QuoteBarWidth.toPx(), size.height)) }
                        .padding(start = QuoteBarWidth + JarvisTheme.dimens.spacing.m),
                ) {
                    block.blocks.forEach { MarkdownBlockView(it, styles, palette, depth) }
                }
            }
        }

        is MarkdownBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs)) {
            block.items.forEachIndexed { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs)) {
                    val marker = when {
                        item.checked == true -> "☑"
                        item.checked == false -> "☐"
                        block.ordered -> "${block.start + index}."
                        else -> Bullets[depth % Bullets.size]
                    }
                    Text(
                        text = marker,
                        style = JarvisTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(min = ListMarkerWidth),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs),
                    ) {
                        item.blocks.forEach { MarkdownBlockView(it, styles, palette, depth + 1) }
                    }
                }
            }
        }

        is MarkdownBlock.Table -> Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            MarkdownTable(block, styles)
        }

        MarkdownBlock.Rule -> HorizontalDivider(
            color = JarvisTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = JarvisTheme.dimens.spacing.xs),
        )
    }
}

@Composable
private fun InlineText(content: List<MarkdownInline>, styles: MarkdownStyles, style: TextStyle, textAlign: TextAlign? = null, modifier: Modifier = Modifier) {
    val text = remember(content, styles) { buildAnnotatedString { appendInlines(content, styles) } }
    Text(text = text, style = style, textAlign = textAlign, modifier = modifier)
}

private fun AnnotatedString.Builder.appendInlines(inlines: List<MarkdownInline>, styles: MarkdownStyles) {
    inlines.forEach { inline ->
        when (inline) {
            is MarkdownInline.Text -> append(inline.text)
            is MarkdownInline.Code -> withStyle(styles.code) { append(inline.text) }
            is MarkdownInline.Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendInlines(inline.children, styles) }
            is MarkdownInline.Strong -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInlines(inline.children, styles) }
            is MarkdownInline.Strike -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { appendInlines(inline.children, styles) }
            is MarkdownInline.Link -> withLink(link(inline.url, styles)) { appendInlines(inline.children, styles) }
            is MarkdownInline.Image -> withLink(link(inline.url, styles)) { append("🖼 ${inline.alt.ifEmpty { inline.url }}") }
            MarkdownInline.LineBreak -> append('\n')
        }
    }
}

private fun link(url: String, styles: MarkdownStyles): LinkAnnotation =
    LinkAnnotation.Clickable(tag = url, styles = styles.link) { styles.onLink(url) }

@Composable
private fun CodeBlock(block: MarkdownBlock.CodeBlock, palette: SyntaxPalette) {
    val text = remember(block, palette) { highlightedText(block.text, block.language?.let(::syntaxLanguageOfName), palette) }

    Surface(
        color = JarvisTheme.colorScheme.surfaceContainer,
        shape = JarvisTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(JarvisTheme.dimens.spacing.m), verticalArrangement = Arrangement.spacedBy(JarvisTheme.dimens.spacing.xs)) {
            block.language?.let {
                Text(text = it, style = JarvisTheme.typography.labelSmall, color = JarvisTheme.colorScheme.onSurfaceVariant)
            }
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Text(text = text, style = JarvisTheme.codeTextStyle, softWrap = false)
            }
        }
    }
}

/**
 * 칸마다 글 폭(최대 [TableCellMaxWidth])으로 열 폭을, 그 폭에서의 높이로 줄 높이를 정한 뒤 모든 칸을 그 크기로 잰다. 칸마다
 * 테두리를 그려도 격자가 맞게 한다.
 */
@Composable
private fun MarkdownTable(table: MarkdownBlock.Table, styles: MarkdownStyles) {
    val columns = table.header.size
    val border = JarvisTheme.colorScheme.outlineVariant
    val headerBackground = JarvisTheme.colorScheme.surfaceContainer
    val cells = listOf(table.header) + table.rows

    Layout(
        content = {
            cells.forEachIndexed { row, cellsInRow ->
                cellsInRow.forEachIndexed { column, content ->
                    val align = when (table.alignments.getOrElse(column) { MarkdownAlignment.Start }) {
                        MarkdownAlignment.Start -> TextAlign.Start
                        MarkdownAlignment.Center -> TextAlign.Center
                        MarkdownAlignment.End -> TextAlign.End
                    }
                    Box(
                        modifier = Modifier
                            .border(0.5.dp, border)
                            .background(if (row == 0) headerBackground else Color.Transparent)
                            .padding(horizontal = JarvisTheme.dimens.spacing.s, vertical = JarvisTheme.dimens.spacing.xs),
                    ) {
                        val style = JarvisTheme.typography.bodyMedium
                        InlineText(
                            content = content,
                            styles = styles,
                            style = if (row == 0) style.copy(fontWeight = FontWeight.SemiBold) else style,
                            textAlign = align,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    ) { measurables, _ ->
        val maxCell = TableCellMaxWidth.roundToPx()
        val widths = IntArray(columns)
        measurables.forEachIndexed { index, measurable ->
            val column = index % columns
            widths[column] = maxOf(widths[column], minOf(measurable.maxIntrinsicWidth(Constraints.Infinity), maxCell))
        }
        val heights = IntArray(cells.size)
        measurables.forEachIndexed { index, measurable ->
            val row = index / columns
            heights[row] = maxOf(heights[row], measurable.minIntrinsicHeight(widths[index % columns]))
        }
        val placeables = measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints.fixed(widths[index % columns], heights[index / columns]))
        }

        layout(widths.sum(), heights.sum()) {
            var y = 0
            for (row in cells.indices) {
                var x = 0
                for (column in 0 until columns) {
                    placeables[row * columns + column].place(x, y)
                    x += widths[column]
                }
                y += heights[row]
            }
        }
    }
}

private val QuoteBarWidth = 3.dp

private val ListMarkerWidth = 20.dp

private val TableCellMaxWidth = 320.dp

private val Bullets = listOf("•", "◦", "▪")
