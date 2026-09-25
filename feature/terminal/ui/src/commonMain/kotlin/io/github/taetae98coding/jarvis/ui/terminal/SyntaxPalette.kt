package io.github.taetae98coding.jarvis.ui.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import io.github.taetae98coding.jarvis.designsystem.theme.JarvisTheme
import io.github.taetae98coding.jarvis.domain.terminal.SyntaxKind
import io.github.taetae98coding.jarvis.domain.terminal.SyntaxLanguage
import io.github.taetae98coding.jarvis.domain.terminal.SyntaxToken
import io.github.taetae98coding.jarvis.domain.terminal.highlightSyntax

/** 편집 보기에서 이보다 긴 글은 칠하지 않는다. 타자마다 파일 전체를 다시 나누므로(docs/common/terminal-file-editor.html H4). */
internal const val EditorHighlightMaxChars = 200_000

/** 문법 색(H1, H4). 값은 VS Code 의 Dark+·Light+ 를 따랐다. */
@Immutable
internal class SyntaxPalette(
    private val keyword: Color,
    private val type: Color,
    private val function: Color,
    private val string: Color,
    private val number: Color,
    private val comment: Color,
    private val annotation: Color,
    private val constant: Color,
    private val property: Color,
    private val tag: Color,
    private val heading: Color,
    private val link: Color,
) {
    fun style(kind: SyntaxKind): SpanStyle =
        when (kind) {
            SyntaxKind.Keyword -> SpanStyle(color = keyword)
            SyntaxKind.Type -> SpanStyle(color = type)
            SyntaxKind.Function -> SpanStyle(color = function)
            SyntaxKind.String, SyntaxKind.Code -> SpanStyle(color = string)
            SyntaxKind.Number -> SpanStyle(color = number)
            SyntaxKind.Comment -> SpanStyle(color = comment, fontStyle = FontStyle.Italic)
            SyntaxKind.Annotation -> SpanStyle(color = annotation)
            SyntaxKind.Constant -> SpanStyle(color = constant)
            SyntaxKind.Property -> SpanStyle(color = property)
            SyntaxKind.Tag -> SpanStyle(color = tag)
            SyntaxKind.Heading -> SpanStyle(color = heading, fontWeight = FontWeight.Bold)
            SyntaxKind.Emphasis -> SpanStyle(fontWeight = FontWeight.Bold)
            SyntaxKind.Link -> SpanStyle(color = link, textDecoration = TextDecoration.Underline)
        }
}

private val DarkSyntaxPalette = SyntaxPalette(
    keyword = Color(0xFFC586C0),
    type = Color(0xFF4EC9B0),
    function = Color(0xFFDCDCAA),
    string = Color(0xFFCE9178),
    number = Color(0xFFB5CEA8),
    comment = Color(0xFF6A9955),
    annotation = Color(0xFFD7BA7D),
    constant = Color(0xFF569CD6),
    property = Color(0xFF9CDCFE),
    tag = Color(0xFF569CD6),
    heading = Color(0xFF569CD6),
    link = Color(0xFF4FC1FF),
)

private val LightSyntaxPalette = SyntaxPalette(
    keyword = Color(0xFFAF00DB),
    type = Color(0xFF267F99),
    function = Color(0xFF795E26),
    string = Color(0xFFA31515),
    number = Color(0xFF098658),
    comment = Color(0xFF008000),
    annotation = Color(0xFF808000),
    constant = Color(0xFF0000FF),
    property = Color(0xFF0451A5),
    tag = Color(0xFF800000),
    heading = Color(0xFF0000FF),
    link = Color(0xFF0070C1),
)

// JarvisTheme 은 다크 여부를 내놓지 않는다. 바탕색의 밝기로 가른다.
@Composable
internal fun syntaxPalette(): SyntaxPalette = if (JarvisTheme.colorScheme.surface.luminance() < 0.5f) DarkSyntaxPalette else LightSyntaxPalette

/**
 * 한 줄 [text] 에 줄 안 위치의 [tokens] 를 입힌다. 탭은 읽기 보기와 같이 공백 네 칸으로 편다. 디스크가 바뀐 직후에는
 * 옛 글의 토큰이 올 수 있어서 위치를 줄 길이 안으로 자른다.
 */
internal fun highlightedLine(text: String, tokens: List<SyntaxToken>?, palette: SyntaxPalette): AnnotatedString {
    if (tokens.isNullOrEmpty()) return AnnotatedString(text.replace("\t", "    "))

    return buildAnnotatedString {
        var position = 0
        fun appendRange(start: Int, end: Int) {
            if (start < end) append(text.substring(start, end).replace("\t", "    "))
        }
        tokens.forEach { token ->
            val start = token.start.coerceIn(position, text.length)
            val end = token.end.coerceIn(start, text.length)
            appendRange(position, start)
            withStyle(palette.style(token.kind)) { appendRange(start, end) }
            position = end
        }
        appendRange(position, text.length)
    }
}

/** 여러 줄 글 전체를 칠한다. 마크다운 코드 블록이 쓴다. */
internal fun highlightedText(text: String, language: SyntaxLanguage?, palette: SyntaxPalette): AnnotatedString {
    if (language == null) return AnnotatedString(text)

    return buildAnnotatedString {
        append(text)
        highlightSyntax(text, language).forEach { addStyle(palette.style(it.kind), it.start, it.end) }
    }
}

/** 편집 보기의 입력 칸에 문법 색을 입힌다. 글자를 바꾸지 않으므로 위치는 그대로다. */
internal data class SyntaxVisualTransformation(
    private val language: SyntaxLanguage,
    private val palette: SyntaxPalette,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.length > EditorHighlightMaxChars) return TransformedText(text, OffsetMapping.Identity)

        return TransformedText(highlightedText(text.text, language, palette), OffsetMapping.Identity)
    }
}
