package io.github.taetae98coding.jarvis.shared.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

internal actual val emulatorProbe: EmulatorProbe = EmulatorProbe {
    withContext(Dispatchers.IO) {
        EmulatorStatus(android = androidSummary(), ios = iosSummary())
    }
}

private fun androidSummary(): EmulatorSummary {
    val sdk = androidSdkDirectory() ?: return EmulatorSummary()

    val avds = runCommand(listOf(File(sdk, "emulator/emulator").path, "-list-avds"))
    val devices = runCommand(listOf(File(sdk, "platform-tools/adb").path, "devices"))

    return EmulatorSummary(
        total = avds?.let(::parseAvdCount) ?: 0,
        running = devices?.let(::parseRunningEmulatorCount) ?: 0,
    )
}

private fun iosSummary(): EmulatorSummary {
    val simctl = simctlCommand() ?: return EmulatorSummary()
    val output = runCommand(simctl + listOf("list", "devices", "available")) ?: return EmulatorSummary()

    return parseSimulatorSummary(output)
}

// `emulator -list-avds` prints one AVD name per line and nothing else; diagnostics go to stderr.
internal fun parseAvdCount(output: String): Int = output.lineSequence().count { it.isNotBlank() }

// A booted AVD shows up in `adb devices` as a serial like `emulator-5554`. Physical devices and
// network targets use other serial shapes, so the prefix is what separates emulators from hardware.
internal fun parseRunningEmulatorCount(output: String): Int =
    output.lineSequence().count { it.startsWith("emulator-") }

internal fun parseSimulatorSummary(output: String): EmulatorSummary {
    val states = SimulatorLine.findAll(output).map { it.groupValues[1] }.toList()

    return EmulatorSummary(total = states.size, running = states.count { it == "Booted" })
}

// A device line looks like `    iPhone 17 (66C9B671-...-DF9528508CD7) (Shutdown)`. Anchoring on the
// UDID keeps runtime headers and device names — which may themselves contain parentheses, e.g.
// `iPad mini (A17 Pro)` — from matching.
private val SimulatorLine = Regex("""\([0-9A-F-]{36}\) \((\w+)\)""")

private fun androidSdkDirectory(): File? =
    sequenceOf(
        System.getenv("ANDROID_HOME"),
        System.getenv("ANDROID_SDK_ROOT"),
        // Android Studio's default location on macOS, which is the only OS this app targets.
        System.getProperty("user.home")?.let { "$it/Library/Android/sdk" },
    ).filterNotNull()
        .map(::File)
        .firstOrNull(File::isDirectory)

private fun simctlCommand(): List<String>? {
    // `xcrun` only finds simctl when xcode-select points at a full Xcode install. A machine left on
    // the Command Line Tools still has the simulator tooling inside Xcode.app, so fall back to the
    // default install path before reporting no simulators.
    if (runCommand(listOf("xcrun", "--find", "simctl")) != null) return listOf("xcrun", "simctl")

    val bundled = File("/Applications/Xcode.app/Contents/Developer/usr/bin/simctl")

    return if (bundled.canExecute()) listOf(bundled.path) else null
}

private const val CommandTimeoutSeconds = 10L

private fun runCommand(command: List<String>): String? =
    runCatching {
        // Output goes to a file rather than a pipe: `adb` forks a daemon that inherits stdout, and a
        // pipe reader would block past the child's exit, outliving the timeout below.
        val output = File.createTempFile("jarvis-emulator", ".out")

        try {
            val process = ProcessBuilder(command)
                .redirectOutput(output)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()

            process.outputStream.close()

            when {
                !process.waitFor(CommandTimeoutSeconds, TimeUnit.SECONDS) -> {
                    process.destroyForcibly()
                    null
                }

                process.exitValue() != 0 -> null
                else -> output.readText()
            }
        } finally {
            output.delete()
        }
    }.getOrNull()
