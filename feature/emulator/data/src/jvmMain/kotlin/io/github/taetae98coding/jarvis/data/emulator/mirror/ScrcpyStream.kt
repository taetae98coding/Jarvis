package io.github.taetae98coding.jarvis.data.emulator.mirror

import java.io.DataInputStream

/**
 * scrcpy v4.1 영상 소켓의 헤더·패킷 파서. 형식은 `device/Streamer.java`·`device/DesktopConnection.java`
 * 에서 읽었다. 전부 빅엔디언이다. `DataInputStream` 은 빅엔디언으로 읽는다.
 *
 * 소켓 순서: 더미 1바이트 → 기기 이름 64바이트 → 코덱 id 4바이트 → 세션 헤더 12바이트 → (패킷 헤더 12바이트 + 페이로드)…
 */
internal class ScrcpyStream(
    private val input: DataInputStream,
) {
    /** 코덱 id. h264 는 "h264" 를 4바이트 정수로 본 값이다. */
    var codecId: Int = 0
        private set

    var width: Int = 0
        private set

    var height: Int = 0
        private set

    /** 더미 바이트·기기 이름·코덱 id·첫 세션 헤더까지 읽는다. 여기서 폭·높이가 채워진다. */
    fun readHeader() {
        input.readByte() // send_dummy_byte
        input.skipFully(DeviceNameLength) // send_device_meta

        codecId = input.readInt() // send_stream_meta: 코덱 id

        // 첫 12바이트는 세션 헤더다(최상위 비트가 세션 표시). 폭·높이를 여기서 얻는다.
        val flags = input.readInt()
        require(flags and SessionFlag != 0) { "세션 헤더가 아니다: ${flags.toUInt().toString(16)}" }
        width = input.readInt()
        height = input.readInt()
    }

    /**
     * 다음 요소를 읽는다. 회전으로 크기가 바뀌면 세션 헤더가 다시 오므로 12바이트의 첫 정수 최상위
     * 비트로 세션 헤더와 패킷 헤더를 가른다(패킷의 pts 는 그 비트를 쓰지 않는다).
     */
    fun readNext(): Element {
        val first = input.readInt()

        if (first and SessionFlag != 0) {
            width = input.readInt()
            height = input.readInt()
            return Element.Resized(width, height)
        }

        // 패킷 헤더: [u64 ptsAndFlags][u32 size]. first 는 그 u64 의 상위 32비트다.
        val ptsLow = input.readInt()
        val size = input.readInt()
        val isConfig = first.toLong() and (ConfigFlag ushr 32) != 0L
        val payload = ByteArray(size)
        input.readFully(payload)

        return Element.Packet(payload, isConfig, pts = (first.toLong() shl 32) or (ptsLow.toLong() and 0xFFFFFFFFL))
    }

    sealed interface Element {
        class Packet(
            val payload: ByteArray,
            val isConfig: Boolean,
            val pts: Long,
        ) : Element

        class Resized(
            val width: Int,
            val height: Int,
        ) : Element
    }

    private fun DataInputStream.skipFully(count: Int) {
        var left = count
        while (left > 0) {
            val skipped = skip(left.toLong()).toInt()
            if (skipped <= 0) {
                readByte()
                left--
            } else {
                left -= skipped
            }
        }
    }

    private companion object {
        const val DeviceNameLength = 64

        // Streamer.PACKET_FLAG_SESSION(1L<<63) 을 상위 32비트로 본 값. 세션 헤더의 첫 정수에 선다.
        const val SessionFlag = 1 shl 31

        // Streamer.PACKET_FLAG_CONFIG(1L<<62).
        const val ConfigFlag = 1L shl 62
    }
}
