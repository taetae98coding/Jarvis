package io.github.taetae98coding.jarvis.domain.devtools

import kotlin.test.Test
import kotlin.test.assertEquals

class HashCalculatorTest {
    @Test
    fun emptyInput() {
        assertHashes(
            "",
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha1 = "da39a3ee5e6b4b0d3255bfef95601890afd80709",
            md5 = "d41d8cd98f00b204e9800998ecf8427e",
        )
    }

    @Test
    fun abc() {
        assertHashes(
            "abc",
            sha256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha1 = "a9993e364706816aba3e25717850c26c9cd0d89d",
            md5 = "900150983cd24fb0d6963f7d28e17f72",
        )
    }

    // 448비트라 패딩이 한 블록을 넘친다.
    @Test
    fun fipsTwoBlockMessage() {
        assertHashes(
            "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq",
            sha256 = "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            sha1 = "84983e441c3bd26ebaae4aa1f95129e5e54670f1",
            md5 = "8215ef0796a20bcaaae116d3876c664a",
        )
    }

    @Test
    fun quickBrownFox() {
        assertHashes(
            "The quick brown fox jumps over the lazy dog",
            sha256 = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592",
            sha1 = "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12",
            md5 = "9e107d9d372bb6826bd81d3542a419d6",
        )
    }

    @Test
    fun koreanUsesUtf8Bytes() {
        assertHashes(
            "안녕하세요",
            sha256 = "2c68318e352971113645cbc72861e1ec23f48d5baa5f9b405fed9dddca893eb4",
            sha1 = "e9a95de0add7606bda402b28a3630cf4b0f8c9b2",
            md5 = "209bebae3eb7363d9b080a66f9e306ef",
        )
    }

    @Test
    fun oneMillionA() {
        assertHashes(
            "a".repeat(1_000_000),
            sha256 = "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
            sha1 = "34aa973cd4c4daa4f61eeb2bdbad27316534016f",
            md5 = "7707d6ae4e027c70eea2a935c2296f21",
        )
    }

    private fun assertHashes(input: String, sha256: String, sha1: String, md5: String) {
        assertEquals(
            listOf(
                DevToolOutput(DevToolOutputKind.SHA256, DevToolValue.Text(sha256)),
                DevToolOutput(DevToolOutputKind.SHA1, DevToolValue.Text(sha1)),
                DevToolOutput(DevToolOutputKind.MD5, DevToolValue.Text(md5)),
            ),
            HashCalculator.convert(input),
        )
    }
}
