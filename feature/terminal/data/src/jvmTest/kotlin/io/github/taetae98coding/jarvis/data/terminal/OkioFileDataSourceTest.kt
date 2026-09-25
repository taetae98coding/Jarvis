package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.FileContent
import io.github.taetae98coding.jarvis.domain.terminal.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OkioFileDataSourceTest {
    private val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    // 첫 값은 신호를 구독하기 전에 읽는다. 구독 전에 낸 신호는 사라지므로 구독을 기다린 뒤 낸다.
    private suspend fun signal() {
        signals.subscriptionCount.first { it > 0 }
        signals.emit(Unit)
    }

    private fun newDirectory(): File = Files.createTempDirectory("jarvis-files").toFile().canonicalFile

    private fun source(home: String = "/nowhere", changes: Flow<Unit> = flowOf()) =
        OkioFileDataSource(FileSystem.SYSTEM, home, Dispatchers.IO) { _, _ -> changes }

    @Test
    fun foldersComeFirstThenNamesIgnoringCaseAndGitIsHidden() = runTest {
        val directory = newDirectory()
        File(directory, "b.txt").writeText("b")
        File(directory, "A.txt").writeText("a")
        File(directory, "src").mkdir()
        File(directory, ".git").mkdir()
        File(directory, ".env").writeText("x")

        val entries = source().observeDirectory(directory.path).first()

        assertEquals(listOf("src", ".env", "A.txt", "b.txt"), entries!!.map { it.name })
        assertEquals(FileEntry("src", File(directory, "src").path, isDirectory = true), entries.first())
    }

    @Test
    fun aSymlinkToAFolderIsAFolder() = runTest {
        val directory = newDirectory()
        val target = File(directory, "real").apply { mkdir() }
        Files.createSymbolicLink(File(directory, "link").toPath(), target.toPath())

        val entries = source().observeDirectory(directory.path).first()!!

        assertTrue(entries.single { it.name == "link" }.isDirectory)
    }

    @Test
    fun missingFoldersAreNullAndTildeIsExpanded() = runTest {
        val home = newDirectory()
        File(home, "work").mkdir()
        File(home, "work/a.txt").writeText("a")

        assertNull(source().observeDirectory(File(home, "missing").path).first())
        assertEquals(listOf("a.txt"), source(home = home.path).observeDirectory("~/work").first()!!.map { it.name })
    }

    @Test
    fun theListingFollowsChangesOnEachSignal() = runTest {
        val directory = newDirectory()
        val values = source(changes = signals).observeDirectory(directory.path).produceIn(backgroundScope)
        assertEquals(emptyList(), values.receive())

        File(directory, "new.txt").writeText("n")
        signal()

        assertEquals(listOf("new.txt"), values.receive()!!.map { it.name })
    }

    @Test
    fun textFilesAreReadAsUtf8() = runTest {
        val file = File(newDirectory(), "a.txt").apply { writeText("첫 줄\n둘째 줄") }

        assertEquals(FileContent.Text("첫 줄\n둘째 줄", truncated = false), source().observeFile(file.path).first())
    }

    @Test
    fun bigFilesAreCutAtTheLimit() = runTest {
        val file = File(newDirectory(), "big.txt").apply { writeText("a".repeat(FileContent.FileViewerMaxBytes.toInt() + 10)) }

        val content = assertIs<FileContent.Text>(source().observeFile(file.path).first())

        assertTrue(content.truncated)
        assertEquals(FileContent.FileViewerMaxBytes.toInt(), content.text.length)
    }

    @Test
    fun aNulByteNearTheStartMeansBinaryAndMissingFilesAreUnreadable() = runTest {
        val directory = newDirectory()
        val binary = File(directory, "a.bin").apply { writeBytes(byteArrayOf(0x50, 0x4b, 0x00, 0x03)) }

        assertEquals(FileContent.Binary, source().observeFile(binary.path).first())
        assertEquals(FileContent.Unreadable, source().observeFile(File(directory, "missing").path).first())
        assertEquals(FileContent.Unreadable, source().observeFile(directory.path).first())
    }

    @Test
    fun theContentFollowsEditsButIsNotReReadWhenNothingChanged() = runTest {
        val file = File(newDirectory(), "a.txt").apply { writeText("one") }
        val values = source(changes = signals).observeFile(file.path).produceIn(backgroundScope)
        assertEquals(FileContent.Text("one", truncated = false), values.receive())

        signal()
        file.writeText("two!")
        file.setLastModified(file.lastModified() + 2_000)
        signal()

        assertEquals(FileContent.Text("two!", truncated = false), values.receive())
        assertTrue(values.tryReceive().isFailure)
    }

    @Test
    fun writingOverwritesTheFileAndTheWatcherReReadsWithoutWaitingForASignal() = runTest {
        val file = File(newDirectory(), "a.txt").apply { writeText("예전 내용이 더 길다") }
        val source = source()
        val values = source.observeFile(file.path).produceIn(backgroundScope)
        assertEquals(FileContent.Text("예전 내용이 더 길다", truncated = false), values.receive())

        assertTrue(source.writeFile(file.path, "새 내용\n").isSuccess)

        assertEquals("새 내용\n", file.readText())
        assertEquals(FileContent.Text("새 내용\n", truncated = false), values.receive())
    }

    @Test
    fun writingIntoAMissingFolderFails() = runTest {
        val file = File(newDirectory(), "gone/a.txt")

        assertTrue(source().writeFile(file.path, "x").isFailure)
    }
}
