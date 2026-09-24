package io.github.taetae98coding.jarvis.data.emulator.mirror

import io.github.taetae98coding.jarvis.domain.emulator.EmulatorFrame
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.global.swscale
import org.bytedeco.ffmpeg.swscale.SwsContext
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.DoublePointer

/**
 * scrcpy 가 보내는 H.264 애넥스-B 를 BGRA 픽셀로 푼다. FFmpeg(bytedeco) 를 쓰는 이유와 버린 후보는
 * docs/platform/jvm.html#device-mirroring 에 있다.
 *
 * 스레드 하나(스트림을 읽는 IO 코루틴)에서만 부른다. 네이티브 자원을 들고 있어 반드시 [close] 한다.
 *
 * 픽셀 버퍼는 [EmulatorFrame.Pixels] 규약대로 4장짜리 링을 돌려 쓴다. 받는 쪽이 받은 자리에서 곧바로
 * 복사하지 않으면 다음 프레임이 덮어쓴다(docs/common/device-mirroring.html#implementation).
 */
internal class H264Decoder : AutoCloseable {
    private val codec = avcodec.avcodec_find_decoder(avcodec.AV_CODEC_ID_H264)
        ?: error("H.264 디코더를 찾지 못했다")

    private val context = avcodec.avcodec_alloc_context3(codec).apply {
        // 지연을 최소로. 프레임 스레딩(FF_THREAD_FRAME=1)은 몇 장을 버퍼링하므로 슬라이스 스레딩
        // (FF_THREAD_SLICE=2)만 쓴다. bytedeco 가 이 상수를 노출하지 않아 값을 직접 쓴다.
        flags(flags() or avcodec.AV_CODEC_FLAG_LOW_DELAY)
        thread_type(2)
    }

    private val packet = avcodec.av_packet_alloc() ?: error("AVPacket 할당 실패")
    private val frame = avutil.av_frame_alloc() ?: error("AVFrame 할당 실패")
    private val bgra = avutil.av_frame_alloc() ?: error("AVFrame 할당 실패")

    private var sws = SwsContext()
    private var bgraWidth = 0
    private var bgraHeight = 0

    private val pool = arrayOfNulls<ByteArray>(FramePoolSize)
    private var poolIndex = 0

    init {
        avutil.av_log_set_level(avutil.AV_LOG_ERROR)
        require(avcodec.avcodec_open2(context, codec, null as org.bytedeco.ffmpeg.avutil.AVDictionary?) == 0) {
            "avcodec_open2 실패"
        }
    }

    /**
     * 패킷 하나를 넣고 프레임이 나오면 BGRA 로 준다. config 패킷(SPS/PPS)이나 아직 프레임이 안 나온
     * 경우 null 이다. 디코더가 지연을 두므로 패킷 하나에 프레임이 0장 또는 1장이다.
     */
    fun decode(payload: ByteArray, size: Int): EmulatorFrame.Pixels? {
        avcodec.av_new_packet(packet, size)
        packet.data().put(payload, 0, size)

        try {
            if (avcodec.avcodec_send_packet(context, packet) < 0) return null
        } finally {
            avcodec.av_packet_unref(packet)
        }

        if (avcodec.avcodec_receive_frame(context, frame) < 0) return null

        return try {
            toBgra()
        } finally {
            avutil.av_frame_unref(frame)
        }
    }

    private fun toBgra(): EmulatorFrame.Pixels {
        val width = frame.width()
        val height = frame.height()

        if (width != bgraWidth || height != bgraHeight) {
            avutil.av_frame_unref(bgra)
            bgra.format(avutil.AV_PIX_FMT_BGRA)
            bgra.width(width)
            bgra.height(height)
            require(avutil.av_frame_get_buffer(bgra, 0) == 0) { "BGRA 프레임 버퍼 할당 실패" }
            bgraWidth = width
            bgraHeight = height
        }

        sws = swscale.sws_getCachedContext(
            sws,
            width, height, frame.format(),
            width, height, avutil.AV_PIX_FMT_BGRA,
            swscale.SWS_BILINEAR, null, null, null as DoublePointer?,
        )
        swscale.sws_scale_frame(sws, bgra, frame)

        val stride = bgra.linesize(0)
        val rowBytes = width * 4
        val out = obtain(rowBytes * height)
        val src: BytePointer = bgra.data(0)

        if (stride == rowBytes) {
            src.position(0).get(out, 0, out.size)
        } else {
            // 행 간격이 폭보다 넓으면(정렬 패딩) 줄마다 폭만큼만 옮긴다.
            for (row in 0 until height) {
                src.position((row.toLong()) * stride).get(out, row * rowBytes, rowBytes)
            }
        }

        return EmulatorFrame.Pixels(width = width, height = height, pixels = out)
    }

    private fun obtain(size: Int): ByteArray {
        val existing = pool[poolIndex]
        val buffer = if (existing != null && existing.size == size) existing else ByteArray(size).also { pool[poolIndex] = it }
        poolIndex = (poolIndex + 1) % FramePoolSize
        return buffer
    }

    override fun close() {
        avcodec.av_packet_free(packet)
        avutil.av_frame_free(frame)
        avutil.av_frame_free(bgra)
        avcodec.avcodec_free_context(context)
        if (!sws.isNull) swscale.sws_freeContext(sws)
    }

    private companion object {
        const val FramePoolSize = 4
    }
}
