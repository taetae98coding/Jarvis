package io.github.taetae98coding.jarvis.data.emulator.mirror

import io.github.taetae98coding.jarvis.data.emulator.adbBinary
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.random.Random

/**
 * 기기 안에 scrcpy-server 를 올리고 영상·제어 소켓을 연다. 조사(옵션·소켓 순서·실행 방식)는
 * docs/platform/jvm.html#device-mirroring 에 있다.
 */
internal object ScrcpyServer {
    private const val RemotePath = "/data/local/tmp/scrcpy-server.jar"
    private const val ServerClass = "com.genymobile.scrcpy.Server"

    // 리소스의 서버 jar 를 한 번만 꺼내 둔다. 세션마다 다시 push 하지만 파일은 재사용한다.
    private val serverJar: File by lazy {
        val temp = File.createTempFile("scrcpy-server", ".jar").apply { deleteOnExit() }
        val resource = "/scrcpy/scrcpy-server-v$ScrcpyServerVersion"
        (javaClass.getResourceAsStream(resource) ?: error("번들에 $resource 이 없다"))
            .use { input -> temp.outputStream().use(input::copyTo) }
        temp
    }

    /**
     * [serial] 기기에 서버를 올리고 연결한다. 실패하면 예외를 던진다(호출 쪽이 재시도한다).
     * 돌려준 [Connection] 을 닫으면 소켓·프로세스·forward 를 모두 정리한다.
     */
    fun start(sdk: File, serial: String): Connection {
        val adb = adbBinary(sdk)
        val scid = Random.nextInt(1, Int.MAX_VALUE)
        val socketName = "scrcpy_%08x".format(scid)

        push(adb, serial)

        var forwardPort = -1
        var process: Process? = null
        val sockets = mutableListOf<Socket>()

        try {
            forwardPort = forward(adb, serial, socketName)
            process = launch(adb, serial, scid)

            val video = connectWithHeader(forwardPort, sockets)
            // 서버는 tunnel_forward 에서 영상 다음에 제어 소켓을 accept 한다. 두 번째 연결이 제어다.
            val control = connect(forwardPort).also(sockets::add)

            val currentForward = forwardPort
            val currentProcess = process
            return Connection(
                video = video,
                control = control,
                close = {
                    sockets.forEach { runCatching { it.close() } }
                    currentProcess.destroyForcibly()
                    runCatching { runAdb(adb, "-s", serial, "forward", "--remove", "tcp:$currentForward") }
                },
            )
        } catch (t: Throwable) {
            sockets.forEach { runCatching { it.close() } }
            process?.destroyForcibly()
            if (forwardPort > 0) runCatching { runAdb(adb, "-s", serial, "forward", "--remove", "tcp:$forwardPort") }
            throw t
        }
    }

    private fun push(adb: String, serial: String) {
        val process = ProcessBuilder(adb, "-s", serial, "push", serverJar.path, RemotePath)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        process.outputStream.close()
        require(process.waitFor() == 0) { "scrcpy-server push 실패" }
    }

    private fun forward(adb: String, serial: String, socketName: String): Int =
        runAdb(adb, "-s", serial, "forward", "tcp:0", "localabstract:$socketName")
            .trim()
            .toIntOrNull()
            ?: error("adb forward 가 포트를 주지 않았다")

    private fun launch(adb: String, serial: String, scid: Int): Process =
        ProcessBuilder(
            adb, "-s", serial, "shell",
            "CLASSPATH=$RemotePath",
            "app_process", "/", ServerClass, ScrcpyServerVersion,
            "scid=%08x".format(scid),
            "log_level=warn",
            "tunnel_forward=true",
            "audio=false",
            "control=true",
            "cleanup=true",
            "video_codec=h264",
            "max_size=$MirrorMaxSize",
            "video_bit_rate=$MirrorBitRate",
            "max_fps=$MirrorMaxFps",
            "send_device_meta=true",
            "send_stream_meta=true",
            "send_frame_meta=true",
            "send_dummy_byte=true",
        )
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
            .also { it.outputStream.close() }

    // adb forward 는 서버가 아직 리슨하지 않아도 connect 가 성공하고 곧 끊긴다. 서버가 보내는 더미
    // 1바이트를 읽을 수 있을 때까지 다시 연결한다.
    private fun connectWithHeader(port: Int, sockets: MutableList<Socket>): ScrcpyStream {
        val deadline = System.nanoTime() + MirrorConnectTimeout.inWholeNanoseconds

        while (true) {
            val socket = runCatching { connect(port) }.getOrNull()

            if (socket != null) {
                val stream = ScrcpyStream(DataInputStream(BufferedInputStream(socket.getInputStream())))
                if (runCatching { stream.readHeader() }.isSuccess) {
                    sockets.add(socket)
                    return stream
                }
                runCatching { socket.close() }
            }

            require(System.nanoTime() < deadline) { "scrcpy 영상 소켓에 연결하지 못했다" }
            Thread.sleep(100)
        }
    }

    private fun connect(port: Int): Socket =
        Socket().apply {
            tcpNoDelay = true
            connect(InetSocketAddress("127.0.0.1", port), 2000)
        }

    private fun runAdb(vararg command: String): String {
        val process = ProcessBuilder(*command)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        process.outputStream.close()
        val output = process.inputStream.readBytes().decodeToString()
        process.waitFor()
        return output
    }

    class Connection(
        val video: ScrcpyStream,
        control: Socket,
        private val close: () -> Unit,
    ) : AutoCloseable {
        private val controlOutput = control.getOutputStream()

        /** 제어 소켓에 이벤트 한 개를 쓴다. 여러 코루틴이 부르므로 잠근다. */
        @Synchronized
        fun send(message: ByteArray) {
            controlOutput.write(message)
            controlOutput.flush()
        }

        override fun close() = close.invoke()
    }
}
