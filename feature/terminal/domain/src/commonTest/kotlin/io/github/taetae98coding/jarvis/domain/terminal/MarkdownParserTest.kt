package io.github.taetae98coding.jarvis.domain.terminal

import io.github.taetae98coding.jarvis.domain.terminal.MarkdownBlock.CodeBlock
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownBlock.Heading
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownBlock.ListBlock
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownBlock.Paragraph
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownBlock.Quote
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownBlock.Rule
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownBlock.Table
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownInline.Code
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownInline.Emphasis
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownInline.Image
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownInline.LineBreak
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownInline.Link
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownInline.Strike
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownInline.Strong
import io.github.taetae98coding.jarvis.domain.terminal.MarkdownInline.Text
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownParserTest {
    private fun paragraph(text: String) = Paragraph(listOf(Text(text)))

    @Test
    fun headings() {
        assertEquals(
            listOf(
                Heading(1, listOf(Text("제목"))),
                Heading(3, listOf(Text("셋째"))),
                Heading(1, listOf(Text("setext"))),
                Heading(2, listOf(Text("둘째"))),
                paragraph("#해시 아님"),
            ),
            parseMarkdown("# 제목\n### 셋째 ###\nsetext\n===\n둘째\n---\n#해시 아님"),
        )
    }

    @Test
    fun paragraphsJoinLinesAndKeepHardBreaks() {
        assertEquals(
            listOf(
                Paragraph(listOf(Text("한 줄 이어짐"), LineBreak, Text("끊김"), LineBreak, Text("역슬래시"))),
                paragraph("다음 문단"),
            ),
            parseMarkdown("한 줄\n이어짐  \n끊김\\\n역슬래시\n\n다음 문단"),
        )
    }

    @Test
    fun inlineStyles() {
        assertEquals(
            listOf(
                Paragraph(
                    listOf(
                        Strong(listOf(Text("굵게"))),
                        Text(" "),
                        Emphasis(listOf(Text("기울임"))),
                        Text(" "),
                        Strike(listOf(Text("취소"))),
                        Text(" "),
                        Code("a * b"),
                        Text(" "),
                        Emphasis(listOf(Text("a "), Strong(listOf(Text("b"))), Text(" c"))),
                        Text(" snake_case_name \\*"),
                        Strong(listOf(Emphasis(listOf(Text("둘 다"))))),
                    ),
                ),
            ),
            parseMarkdown("**굵게** _기울임_ ~~취소~~ `a * b` *a **b** c* snake_case_name \\\\\\****둘 다***"),
        )
    }

    @Test
    fun links() {
        assertEquals(
            listOf(
                Paragraph(
                    listOf(
                        Link(listOf(Text("문서 "), Code("a")), "docs/a.md"),
                        Text(" "),
                        Link(listOf(Text("https://x.io")), "https://x.io"),
                        Text(" 와 "),
                        Link(listOf(Text("https://y.io/a_b")), "https://y.io/a_b"),
                        Text(". "),
                        Image("로고", "logo.png"),
                        Text(" [없음]"),
                    ),
                ),
            ),
            parseMarkdown("[문서 `a`](docs/a.md \"제목\") <https://x.io> 와 https://y.io/a_b. ![로고](logo.png) [없음]"),
        )
    }

    @Test
    fun nestedListsAndTasks() {
        val text = """
            - 하나
              - 안쪽
            - [x] 끝냄
            - [ ] 할 일

            3. 셋
            4. 넷
        """.trimIndent()

        assertEquals(
            listOf(
                ListBlock(
                    ordered = false,
                    start = 1,
                    items = listOf(
                        MarkdownListItem(null, listOf(paragraph("하나"), ListBlock(false, 1, listOf(MarkdownListItem(null, listOf(paragraph("안쪽"))))))),
                        MarkdownListItem(true, listOf(paragraph("끝냄"))),
                        MarkdownListItem(false, listOf(paragraph("할 일"))),
                    ),
                ),
                ListBlock(ordered = true, start = 3, items = listOf(MarkdownListItem(null, listOf(paragraph("셋"))), MarkdownListItem(null, listOf(paragraph("넷"))))),
            ),
            parseMarkdown(text),
        )
    }

    @Test
    fun quotesNest() {
        assertEquals(
            listOf(Quote(listOf(paragraph("바깥 이어짐"), Quote(listOf(paragraph("안쪽")))))),
            parseMarkdown("> 바깥\n이어짐\n>\n> > 안쪽"),
        )
    }

    @Test
    fun codeBlocksAndRules() {
        assertEquals(
            listOf(
                CodeBlock("kotlin", "val a = 1\n\n  // 들여씀"),
                Rule,
                CodeBlock(null, "indented\n  more"),
                CodeBlock(null, "안 닫힘"),
            ),
            parseMarkdown("```kotlin title\nval a = 1\n\n  // 들여씀\n```\n***\n\n    indented\n      more\n\n~~~\n안 닫힘"),
        )
    }

    @Test
    fun tables() {
        val text = """
            | 이름 | 수 | 가운데 |
            |:-----|---:|:---:|
            | a | 1 | `x|y` |
            | b \| c |
        """.trimIndent()

        assertEquals(
            listOf(
                Table(
                    header = listOf(listOf(Text("이름")), listOf(Text("수")), listOf(Text("가운데"))),
                    alignments = listOf(MarkdownAlignment.Start, MarkdownAlignment.End, MarkdownAlignment.Center),
                    rows = listOf(
                        listOf(listOf(Text("a")), listOf(Text("1")), listOf(Code("x|y"))),
                        listOf(listOf(Text("b | c")), emptyList(), emptyList()),
                    ),
                ),
            ),
            parseMarkdown(text),
        )
    }

    @Test
    fun relativeLinksResolveAgainstTheFileFolder() {
        assertEquals("/r/docs/a.md", resolveRelativePath("/r/README.md", "docs/a.md"))
        assertEquals("/r/docs/a.md", resolveRelativePath("/r/README.md", "./docs/a.md#part"))
        assertEquals("/r/README.md", resolveRelativePath("/r/docs/a.md", "../README.md"))
        assertEquals("/r/my file.md", resolveRelativePath("/r/README.md", "my%20file.md"))
        assertEquals("/etc/hosts", resolveRelativePath("/r/README.md", "/etc/hosts"))
        assertEquals(null, resolveRelativePath("/r/README.md", "#part"))
        assertEquals(null, resolveRelativePath("/r/README.md", "https://x.io/a.md"))
        assertEquals(null, resolveRelativePath("/r/README.md", "mailto:a@b.c"))
    }
}
