package io.github.taetae98coding.jarvis.data.appinfo

import io.github.taetae98coding.jarvis.data.PlatformContext

// 데스크탑은 macOS 만 지원한다. 다른 OS 에는 scutil·ioreg 가 없어서 실행이 실패하고 빈 값이 된다.
// InetAddress.getLocalHost().hostName 은 macOS 에서 역방향 DNS 조회로 몇 초를 멈출 수 있어 쓰지 않는다.
internal actual fun readDeviceIdentity(context: PlatformContext): DeviceIdentity =
    DeviceIdentity(
        name = commandOutput("scutil", "--get", "ComputerName").trim(),
        id = platformUuid(commandOutput("ioreg", "-rd1", "-c", "IOPlatformExpertDevice")),
    )

/** `ioreg -rd1 -c IOPlatformExpertDevice` 출력의 `"IOPlatformUUID" = "…"` 줄에서 따옴표 안 값. 없으면 빈 문자열. */
internal fun platformUuid(ioregOutput: String): String =
    PlatformUuidLine.find(ioregOutput)?.groupValues?.get(1).orEmpty()

private val PlatformUuidLine = Regex(""""IOPlatformUUID"\s*=\s*"([^"]+)"""")

private fun commandOutput(vararg command: String): String =
    runCatching {
        val process = ProcessBuilder(*command)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        process.outputStream.close()
        val output = process.inputStream.readBytes().decodeToString()
        if (process.waitFor() == 0) output else ""
    }.getOrDefault("")
