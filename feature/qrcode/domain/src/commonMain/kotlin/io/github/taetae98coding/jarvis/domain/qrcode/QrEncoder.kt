package io.github.taetae98coding.jarvis.domain.qrcode

sealed interface QrEncodeResult {
    data class Success(val code: QrCode) : QrEncodeResult

    /** 버전 40 에도 들어가지 않는다. [limit]·[length] 의 단위는 [mode] 의 문자 수 표시자와 같다(바이트 모드면 UTF-8 바이트). */
    data class TooLong(val mode: QrMode, val limit: Int, val length: Int) : QrEncodeResult
}

/**
 * QR 코드 모델 2(ISO/IEC 18004) 인코더.
 *
 * Project Nayuki 의 QR Code generator library(MIT, https://www.nayuki.io/page/qr-code-generator-library)
 * 의 구조를 Kotlin 으로 옮겼다. 사용자가 고른 오류 정정 단계를 그대로 보이려고, 남는 자리에서 단계를 올리는
 * Nayuki 의 boostEcl 은 뺐다.
 */
object QrEncoder {
    /** [mask] 가 null 이면 여덟 마스크의 벌점을 모두 매겨 가장 낮은 것을 고른다. */
    fun encode(text: String, errorCorrection: QrErrorCorrection, mask: Int? = null): QrEncodeResult {
        require(mask == null || mask in 0..7)
        val segment = QrSegment.of(text)
        val version = (QrTables.MinVersion..QrTables.MaxVersion).firstOrNull { version ->
            val used = segment.totalBits(version)
            used != null && used <= QrTables.dataCodewords(version, errorCorrection) * 8
        } ?: return QrEncodeResult.TooLong(segment.mode, maxLength(segment.mode, errorCorrection), segment.length)

        val data = dataCodewords(segment, version, errorCorrection)
        return QrEncodeResult.Success(QrSymbolBuilder(version, errorCorrection).build(data, mask, segment.mode))
    }

    /** 버전 40 에 [mode] 한 세그먼트로 담을 수 있는 최대 길이. */
    fun maxLength(mode: QrMode, errorCorrection: QrErrorCorrection): Int {
        val version = QrTables.MaxVersion
        val available = QrTables.dataCodewords(version, errorCorrection) * 8 - 4 - mode.charCountBits(version)
        return when (mode) {
            QrMode.NUMERIC -> available / 10 * 3 + when (available % 10) {
                in 7..9 -> 2
                in 4..6 -> 1
                else -> 0
            }

            QrMode.ALPHANUMERIC -> available / 11 * 2 + if (available % 11 >= 6) 1 else 0
            QrMode.BYTE -> available / 8
        }
    }

    internal fun dataCodewords(segment: QrSegment, version: Int, errorCorrection: QrErrorCorrection): IntArray {
        val capacity = QrTables.dataCodewords(version, errorCorrection) * 8
        val buffer = BitBuffer()
        segment.appendTo(buffer, version)
        buffer.append(0, minOf(4, capacity - buffer.size))
        buffer.append(0, (8 - buffer.size % 8) % 8)

        val result = IntArray(capacity / 8)
        for (i in 0 until buffer.size) {
            if (buffer[i]) result[i ushr 3] = result[i ushr 3] or (1 shl (7 - (i and 7)))
        }
        var pad = 0xEC
        for (i in buffer.size / 8 until result.size) {
            result[i] = pad
            pad = pad xor 0xEC xor 0x11
        }
        return result
    }

    /** 블록마다 ECC 를 붙이고 ISO/IEC 18004 7.6 의 순서로 섞는다. */
    internal fun addEccAndInterleave(data: IntArray, version: Int, errorCorrection: QrErrorCorrection): IntArray {
        require(data.size == QrTables.dataCodewords(version, errorCorrection))
        val blockCount = QrTables.errorCorrectionBlocks(version, errorCorrection)
        val eccLength = QrTables.eccCodewordsPerBlock(version, errorCorrection)
        val rawCodewords = QrTables.rawDataModules(version) / 8
        val shortBlocks = blockCount - rawCodewords % blockCount
        val shortBlockLength = rawCodewords / blockCount

        val divisor = ReedSolomon.divisor(eccLength)
        var offset = 0
        val blocks = List(blockCount) { i ->
            val dataLength = shortBlockLength - eccLength + if (i < shortBlocks) 0 else 1
            val blockData = data.copyOfRange(offset, offset + dataLength)
            offset += dataLength
            // 짧은 블록도 긴 블록과 같은 길이로 두고 데이터 끝 한 칸을 비워 둔다. 섞을 때 그 칸을 건너뛴다.
            val block = blockData.copyOf(shortBlockLength + 1)
            ReedSolomon.remainder(blockData, divisor).copyInto(block, destinationOffset = block.size - eccLength)
            block
        }

        val result = IntArray(rawCodewords)
        var k = 0
        for (i in 0 until shortBlockLength + 1) {
            for (j in blocks.indices) {
                if (i != shortBlockLength - eccLength || j >= shortBlocks) result[k++] = blocks[j][i]
            }
        }
        return result
    }
}
