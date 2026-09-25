package io.github.taetae98coding.jarvis.data.terminal

import io.github.taetae98coding.jarvis.domain.terminal.AndroidProject
import io.github.taetae98coding.jarvis.domain.terminal.AndroidRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.IosProject
import io.github.taetae98coding.jarvis.domain.terminal.IosRunRequest
import io.github.taetae98coding.jarvis.domain.terminal.ProjectKind
import io.github.taetae98coding.jarvis.domain.terminal.ProjectLoad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal actual fun createProjectRunDataSource(): ProjectRunDataSource = JvmProjectRunDataSource()

/**
 * 판정은 파일만 보고, 변형·스킴은 Gradle·xcodebuild 를 한 번 돌려 읽는다. 둘 다 창이 떠 있는 동안 한 번 읽고 끝나는
 * cold 흐름이다 — 프로젝트 파일이 바뀌는 것을 알려 줄 콜백이 없고, 창을 다시 열면 다시 읽으므로 폴링하지 않는다.
 */
internal class JvmProjectRunDataSource(
    private val home: String = System.getProperty("user.home"),
) : ProjectRunDataSource {
    // 빌드 파일이 그대로면 Gradle 을 다시 돌리지 않는다. 앱이 켜져 있는 동안만 둔다.
    private val androidCache = ConcurrentHashMap<String, Pair<List<Pair<String, Long>>, AndroidProject>>()

    override val isSupported: Boolean = true

    override fun observeProjectKinds(directory: String): Flow<Set<ProjectKind>> =
        flow { emit(detectProjectKinds(File(expandHome(directory, home)))) }.flowOn(Dispatchers.IO)

    override fun observeAndroidProject(directory: String): Flow<ProjectLoad<AndroidProject>> =
        flow {
            emit(ProjectLoad.Loading)
            emit(loadAndroidProject(File(expandHome(directory, home))))
        }.flowOn(Dispatchers.IO)

    override fun observeIosProject(directory: String): Flow<ProjectLoad<IosProject>> =
        flow {
            emit(ProjectLoad.Loading)
            emit(loadIosProject(File(expandHome(directory, home))))
        }.flowOn(Dispatchers.IO)

    override suspend fun androidRunCommand(request: AndroidRunRequest): String =
        withContext(Dispatchers.IO) { androidRunScript(request.copy(directory = expandHome(request.directory, home)), androidSdkDirectory()?.path) }

    override suspend fun iosRunCommand(request: IosRunRequest): String {
        val directory = expandHome(request.project.directory, home)
        // 워크트리마다 따로 두어 두 워크트리의 빌드가 서로의 산출물을 덮지 않는다.
        val derivedData = "$home/Library/Developer/Xcode/DerivedData/Jarvis-${Integer.toHexString(directory.hashCode())}"

        return iosRunScript(request.copy(project = request.project.copy(directory = directory)), derivedData)
    }

    private fun loadAndroidProject(directory: File): ProjectLoad<AndroidProject> {
        if (!isAndroidProject(directory)) return ProjectLoad.Failed()

        val stamp = buildFileStamp(directory)
        androidCache[directory.path]?.takeIf { it.first == stamp }?.let { return ProjectLoad.Loaded(it.second) }

        val initScript = File.createTempFile("jarvis-variants", ".gradle")
        return try {
            initScript.writeText(VariantInitScript)
            // 설정 캐시가 적중하면 onVariants 가 돌지 않는다(docs/platform/jvm.html#terminal-run).
            val result = runInLoginShell(
                directory,
                "./gradlew -q --no-configuration-cache -I ${shellQuote(initScript.path)} help",
                GradleTimeoutMinutes,
            ) ?: return ProjectLoad.Failed("Gradle 이 시간 안에 끝나지 않았습니다.")
            if (result.first != 0) return ProjectLoad.Failed(result.second.lastLines())

            val project = AndroidProject(directory.path, parseAndroidApps(result.second))
            androidCache[directory.path] = stamp to project
            ProjectLoad.Loaded(project)
        } finally {
            initScript.delete()
        }
    }

    private fun loadIosProject(directory: File): ProjectLoad<IosProject> {
        val container = findXcodeContainer(directory) ?: return ProjectLoad.Failed()
        val isWorkspace = container.extension == "xcworkspace"
        val command = listOf("xcodebuild", "-list", "-json", if (isWorkspace) "-workspace" else "-project", container.path)
        val result = runProcess(command, directory, XcodeListTimeoutMinutes, env = xcodeEnvironment(), mergeError = false)
            ?: return ProjectLoad.Failed("xcodebuild 가 시간 안에 끝나지 않았습니다.")
        val (schemes, configurations) = parseXcodeList(result.second)
            ?: return ProjectLoad.Failed(result.second.lastLines())

        return ProjectLoad.Loaded(IosProject(directory.path, container.path, isWorkspace, schemes, configurations))
    }

    // Finder 로 띄운 앱에는 JAVA_HOME·PATH 가 없어서 gradlew 가 java 를 못 찾는다. 터미널 탭과 같은 로그인·대화형 셸로 돌린다.
    private fun runInLoginShell(directory: File, script: String, timeoutMinutes: Long): Pair<Int, String>? {
        val shell = System.getenv("SHELL")?.takeIf { File(it).canExecute() } ?: "/bin/zsh"

        return runProcess(listOf(shell, "-l", "-i", "-c", script), directory, timeoutMinutes, terminalEnvironment(System.getenv()), mergeError = true)
    }
}

/** Command Line Tools 만 선택된 Mac 에서도 xcodebuild 가 돌도록 Xcode.app 을 가리킨다. */
private fun xcodeEnvironment(): Map<String, String> {
    val environment = System.getenv().toMutableMap()
    val selected = runProcess(listOf("xcode-select", "-p"), null, 1, environment, mergeError = false)?.second?.trim().orEmpty()
    if ("DEVELOPER_DIR" !in environment && !selected.contains("Xcode") && File(XcodeDeveloperDir).isDirectory) {
        environment["DEVELOPER_DIR"] = XcodeDeveloperDir
    }
    return environment
}

/** 종료 코드와 출력. 시간 안에 끝나지 않았거나 띄우지 못했으면 null 이다. */
private fun runProcess(
    command: List<String>,
    directory: File?,
    timeoutMinutes: Long,
    env: Map<String, String>,
    mergeError: Boolean,
): Pair<Int, String>? =
    runCatching {
        // 출력은 파일로 받는다. Gradle 데몬·adb 가 stdout 을 물려받으면 파이프 읽기가 끝나지 않는다.
        val output = File.createTempFile("jarvis-run", ".out")
        try {
            val process = ProcessBuilder(command)
                .directory(directory)
                .redirectOutput(output)
                .apply {
                    if (mergeError) redirectErrorStream(true) else redirectError(ProcessBuilder.Redirect.DISCARD)
                    environment().clear()
                    environment().putAll(env)
                }
                .start()
            process.outputStream.close()

            if (!process.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
                process.destroyForcibly()
                null
            } else {
                process.exitValue() to output.readText()
            }
        } finally {
            output.delete()
        }
    }.getOrNull()

private fun String.lastLines(): String = lineSequence().filter { it.isNotBlank() }.toList().takeLast(ErrorLines).joinToString("\n")

/** Gradle 설정에 영향을 주는 파일의 수정 시각. 하나라도 바뀌면 변형을 다시 읽는다. */
private fun buildFileStamp(directory: File): List<Pair<String, Long>> {
    val buildFiles = setOf("settings.gradle.kts", "settings.gradle", "build.gradle.kts", "build.gradle", "gradle.properties")
    val candidates = directory.listFiles().orEmpty().flatMap { file ->
        if (file.isDirectory) file.listFiles().orEmpty().filter { it.name in buildFiles } else listOf(file).filter { it.name in buildFiles }
    } + File(directory, "gradle/libs.versions.toml")

    return candidates.filter { it.isFile }.map { it.path to it.lastModified() }.sortedBy { it.first }
}

internal fun androidSdkDirectory(): File? =
    sequenceOf(
        System.getenv("ANDROID_HOME"),
        System.getenv("ANDROID_SDK_ROOT"),
        // 데스크탑은 macOS 만 지원한다. Android Studio 의 기본 설치 경로다.
        System.getProperty("user.home")?.let { "$it/Library/Android/sdk" },
    ).filterNotNull()
        .map(::File)
        .firstOrNull(File::isDirectory)

private const val GradleTimeoutMinutes = 5L

private const val XcodeListTimeoutMinutes = 2L

private const val ErrorLines = 8
