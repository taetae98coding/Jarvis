package io.github.taetae98coding.jarvis.data.emulator.agent

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.taetae98coding.jarvis.data.emulator.DevicePairingDataSource
import io.github.taetae98coding.jarvis.data.emulator.EmulatorDataSource
import io.github.taetae98coding.jarvis.data.emulator.devicePairingDataSource
import io.github.taetae98coding.jarvis.data.emulator.emulatorDataSource
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorDevice
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorFrame
import io.github.taetae98coding.jarvis.domain.emulator.EmulatorStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * 같은 머신의 다른 타깃에 에뮬레이터 개수·목록·화면을 알려주고 제스처·실행·깨우기·페어링을 대신
 * 전달하는 로컬 HTTP 에이전트를 띄운다.
 *
 * Android 에뮬레이터·iOS 시뮬레이터·브라우저는 샌드박스 안이라 SDK 도구를 직접 띄울 수 없다.
 * 도구에 닿을 수 있는 건 개발자 머신에서 도는 데스크탑 앱뿐이어서, 그 결과를 HTTP 로 넘긴다.
 *
 * 포트가 이미 쓰이고 있으면 조용히 no-op 으로 빠진다. 데스크탑 앱 자신은 에이전트 없이도 동작하므로
 * 여기서 실패해도 앱에는 영향이 없다.
 */
fun startEmulatorHostAgent(): AutoCloseable =
    startEmulatorHostAgent(HostAgentPort, emulatorDataSource, devicePairingDataSource)

internal fun startEmulatorHostAgent(
    port: Int,
    dataSource: EmulatorDataSource,
    pairing: DevicePairingDataSource,
): AutoCloseable {
    // 루프백에만 바인딩한다. 에뮬레이터의 10.0.2.2 와 `adb reverse` 는 호스트 루프백으로 들어오므로
    // 이걸로 충분하고, 같은 네트워크의 다른 기기에는 열리지 않는다.
    val address = InetSocketAddress(InetAddress.getLoopbackAddress(), port)
    val server = runCatching { HttpServer.create(address, 0) }.getOrNull() ?: return AutoCloseable {}

    val status = AtomicReference<EmulatorStatus?>(null)
    val devices = AtomicReference<List<EmulatorDevice>?>(null)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 개수와 목록은 요청마다 세지 않고 미리 받아 둔 값을 준다. 구독자가 여럿이어도 SDK 도구는 한 번만
    // 돌고, 응답이 즉시 끝난다.
    scope.launch { dataSource.observeStatus().collect(status::set) }
    scope.launch { dataSource.observeDevices().collect(devices::set) }

    // 화면 요청은 그 자리에서 기기를 찍느라 0.3~1초 걸린다. 기본 실행기는 단일 스레드라 그동안 다른
    // 요청이 전부 막히므로 풀을 따로 준다.
    val executor = Executors.newFixedThreadPool(ScreenWorkerCount)
    server.executor = executor

    server.createContext(HostAgentPath) { exchange ->
        exchange.handle {
            when {
                exchange.requestMethod != "GET" -> exchange.respond(MethodNotAllowed)
                // 아직 한 번도 세지 못했다. 0개로 답해서 "셀 수 없음" 과 섞이게 하지 않는다.
                else -> status.get()
                    ?.let { exchange.respond(Ok, encodeEmulatorStatus(it).encodeToByteArray()) }
                    ?: exchange.respond(ServiceUnavailable)
            }
        }
    }

    server.createContext(HostAgentDevicesPath) { exchange ->
        exchange.handle {
            when {
                exchange.requestMethod != "GET" -> exchange.respond(MethodNotAllowed)
                else -> devices.get()
                    ?.let { exchange.respond(Ok, encodeEmulatorDevices(it).encodeToByteArray()) }
                    ?: exchange.respond(ServiceUnavailable)
            }
        }
    }

    server.createContext(HostAgentScreenPath) { exchange ->
        exchange.handle {
            val deviceId = exchange.queryParameter("id")

            when {
                exchange.requestMethod != "GET" -> exchange.respond(MethodNotAllowed)
                deviceId == null -> exchange.respond(BadRequest)
                else -> {
                    // Flow 의 첫 방출이 지금 프레임이다. Android 는 스트림이 replay 로 즉시 주고,
                    // 시뮬레이터는 폴링이 한 장 찍는다.
                    val frame = runBlocking { dataSource.observeScreen(deviceId).first() }

                    when (frame) {
                        // 스트림의 디코딩된 픽셀은 JPEG 로 인코딩해 보낸다. data 모듈에는 ImageBitmap 이
                        // 없어 AWT 를 쓴다.
                        is EmulatorFrame.Pixels -> exchange.respond(Ok, encodeJpeg(frame), contentType = "image/jpeg")
                        // 시뮬레이터의 PNG 는 그대로 흘려보낸다.
                        is EmulatorFrame.Encoded -> exchange.respond(Ok, frame.bytes, contentType = "image/png")
                        null -> exchange.respond(ServiceUnavailable)
                    }
                }
            }
        }
    }

    server.createContext(HostAgentGesturePath) { exchange ->
        exchange.handle {
            when (exchange.requestMethod) {
                // 브라우저가 단순 요청 조건을 벗어나게 보내면 사전 요청이 먼저 온다.
                "OPTIONS" -> exchange.respond(NoContent)

                "POST" -> {
                    val gesture = exchange.requestBody.use(InputStream::readBytes)
                        .decodeToString()
                        .let(::decodeEmulatorGesture)

                    if (gesture == null) {
                        exchange.respond(BadRequest)
                    } else {
                        runBlocking { dataSource.sendGesture(gesture.deviceId, gesture.gesture) }
                        exchange.respond(NoContent)
                    }
                }

                else -> exchange.respond(MethodNotAllowed)
            }
        }
    }

    server.createContext(HostAgentLaunchPath) { exchange ->
        exchange.handle {
            when (exchange.requestMethod) {
                "OPTIONS" -> exchange.respond(NoContent)

                "POST" -> {
                    val deviceId = exchange.requestBody.use(InputStream::readBytes)
                        .decodeToString()
                        .let(::decodeEmulatorDeviceId)

                    if (deviceId == null) {
                        exchange.respond(BadRequest)
                    } else {
                        // 에뮬레이터가 뜨는 데 걸리는 수십 초를 여기서 기다리면 클라이언트 타임아웃에
                        // 먼저 걸린다. 데이터 소스도 띄우기만 하고 바로 돌아온다.
                        runBlocking { dataSource.launch(deviceId) }
                        exchange.respond(NoContent)
                    }
                }

                else -> exchange.respond(MethodNotAllowed)
            }
        }
    }

    server.createContext(HostAgentWakePath) { exchange ->
        exchange.handle {
            when (exchange.requestMethod) {
                "OPTIONS" -> exchange.respond(NoContent)

                "POST" -> {
                    val deviceId = exchange.requestBody.use(InputStream::readBytes)
                        .decodeToString()
                        .let(::decodeEmulatorDeviceId)

                    if (deviceId == null) {
                        exchange.respond(BadRequest)
                    } else {
                        runBlocking { dataSource.wake(deviceId) }
                        exchange.respond(NoContent)
                    }
                }

                else -> exchange.respond(MethodNotAllowed)
            }
        }
    }

    server.createContext(HostAgentPairingServicesPath) { exchange ->
        exchange.handle {
            when {
                exchange.requestMethod != "GET" -> exchange.respond(MethodNotAllowed)
                // 목록과 달리 미리 받아 두지 않는다. 페어링 화면이 떠 있을 때만 오는 요청이고,
                // 미리 받으려면 아무도 보지 않는 동안에도 1초마다 adb 를 불러야 한다.
                else -> runBlocking { pairing.observePairingServices().first() }
                    ?.let { exchange.respond(Ok, encodePairingServices(it).encodeToByteArray()) }
                    ?: exchange.respond(ServiceUnavailable)
            }
        }
    }

    server.createContext(HostAgentPairPath) { exchange ->
        exchange.handle {
            when (exchange.requestMethod) {
                "OPTIONS" -> exchange.respond(NoContent)

                "POST" -> {
                    val request = exchange.requestBody.use(InputStream::readBytes)
                        .decodeToString()
                        .let(::decodePairRequest)

                    if (request == null) {
                        exchange.respond(BadRequest)
                    } else {
                        // 실행과 달리 끝날 때까지 기다린다. 입력한 코드가 맞았는지 알려 줄 곳이 이
                        // 응답뿐이다. 클라이언트는 이 경로에만 타임아웃을 길게 준다.
                        val result = runBlocking { pairing.pair(request.service, request.code) }
                        exchange.respond(Ok, encodePairResult(result).encodeToByteArray())
                    }
                }

                else -> exchange.respond(MethodNotAllowed)
            }
        }
    }

    server.start()

    return AutoCloseable {
        server.stop(0)
        executor.shutdownNow()
        scope.cancel()
    }
}

// BGRA 픽셀을 JPEG 로 인코딩한다. TYPE_INT_RGB 는 0xRRGGBB 정수를 받으므로 BGRA 바이트에서 뽑아낸다.
private fun encodeJpeg(frame: EmulatorFrame.Pixels): ByteArray {
    val image = BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB)
    val pixels = frame.pixels
    val rgb = IntArray(frame.width * frame.height)

    for (i in rgb.indices) {
        val base = i * 4
        val b = pixels[base].toInt() and 0xFF
        val g = pixels[base + 1].toInt() and 0xFF
        val r = pixels[base + 2].toInt() and 0xFF
        rgb[i] = (r shl 16) or (g shl 8) or b
    }
    image.setRGB(0, 0, frame.width, frame.height, rgb, 0, frame.width)

    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    val output = ByteArrayOutputStream()

    return try {
        ImageIO.createImageOutputStream(output).use { stream ->
            writer.output = stream
            val param = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = 0.8f
            }
            writer.write(null, IIOImage(image, null, null), param)
        }
        output.toByteArray()
    } finally {
        writer.dispose()
    }
}

private const val ScreenWorkerCount = 4

private const val Ok = 200
private const val NoContent = 204
private const val BadRequest = 400
private const val MethodNotAllowed = 405
private const val ServiceUnavailable = 503

// 핸들러가 어떻게 끝나든 교환을 닫는다. 닫지 않으면 클라이언트가 오지 않는 응답을 기다린다.
private fun HttpExchange.handle(block: () -> Unit) {
    try {
        block()
    } catch (_: Exception) {
        runCatching { respond(ServiceUnavailable) }
    } finally {
        close()
    }
}

private fun HttpExchange.queryParameter(name: String): String? =
    requestURI.rawQuery
        ?.split('&')
        ?.map { it.split('=', limit = 2) }
        ?.firstOrNull { it.first() == name }
        ?.getOrNull(1)
        ?.let { URLDecoder.decode(it, Charsets.UTF_8) }

private fun HttpExchange.respond(
    code: Int,
    body: ByteArray? = null,
    contentType: String = "application/json",
) {
    responseHeaders.add("Content-Type", contentType)
    // 브라우저 클라이언트는 webApp 오리진과 포트가 달라 교차 출처가 된다.
    responseHeaders.add("Access-Control-Allow-Origin", "*")
    responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
    responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")
    // 개수도 화면도 계속 바뀌므로 브라우저와 NSURLSession 의 기본 캐시에 남아서는 안 된다.
    responseHeaders.add("Cache-Control", "no-store")

    // -1 이 "본문 없음" 이다. 0 을 주면 chunked 로 열려서 클라이언트가 오지 않는 본문을 기다린다.
    sendResponseHeaders(code, body?.size?.toLong() ?: -1L)
    body?.let { responseBody.use { stream -> stream.write(it) } }
}
