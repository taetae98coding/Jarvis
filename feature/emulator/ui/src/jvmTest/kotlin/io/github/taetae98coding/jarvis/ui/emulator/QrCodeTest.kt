package io.github.taetae98coding.jarvis.ui.emulator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QrCodeTest {
    // 이 그림을 macOS CoreImage(CIDetector) 로 디코딩해 원문이 그대로 나오는 것을 확인했다. 인코더를
    // 고쳐서 이 기대값이 바뀌면 같은 방법으로 다시 확인한 뒤에 바꾼다.
    @Test
    fun encodesAPairingPayloadAsVerified() {
        val qr = QrCode.encode("WIFI:T:ADB;S:jarvis-aB3dE5gH7j;P:Zx9Yw8Vu7Ts6;;")

        assertEquals(PairingPayloadModules, qr.render())
    }

    // 버전 n 의 한 변은 4n + 17 칸이다. 오류 정정 M 에서 버전 1 은 14바이트, 4 는 62바이트, 10 은
    // 213바이트까지 담는다.
    @Test
    fun versionGrowsWithTheText() {
        assertEquals(21, QrCode.encode("x".repeat(14)).size)
        assertEquals(25, QrCode.encode("x".repeat(15)).size)
        assertEquals(33, QrCode.encode("x".repeat(62)).size)
        assertEquals(57, QrCode.encode("x".repeat(213)).size)
    }

    @Test
    fun rejectsTextBeyondVersionTen() {
        assertFailsWith<IllegalArgumentException> { QrCode.encode("x".repeat(214)) }
    }

    private fun QrCode.render(): List<String> =
        (0 until size).map { y -> (0 until size).joinToString("") { x -> if (this[x, y]) "#" else "." } }

    private companion object {
        val PairingPayloadModules = listOf(
            "#######....#....#.#.##.#..#######",
            "#.....#........##.#.#.#.#.#.....#",
            "#.###.#.####..#.#....#.##.#.###.#",
            "#.###.#.#.##...#..#....#..#.###.#",
            "#.###.#.##...####...#.###.#.###.#",
            "#.....#.#.#..#...###......#.....#",
            "#######.#.#.#.#.#.#.#.#.#.#######",
            "........#.##....#.####.##........",
            "#.#####..#.###..####..#...#####..",
            "#......#.##..##..#.##..##......#.",
            "########.....###.#..#.....######.",
            "###.#...#..#.#.#.##.#####..####.#",
            "......#.##..#.#.#..##....#..#.#.#",
            "##..#....##.##.#..#..###.#.#.....",
            "#.#.#.#.#.##.######.##..#...#..#.",
            ".#.###.#########.....##..#...####",
            "#..#..#.##..####.#....#..#..##.#.",
            "#.##.#.##..######.##..#....#.####",
            "..#######....#.##.#.#....#.#.....",
            ".###.#.####..#..#..#.####.#..##..",
            "#.##..#...#..###..#..#.#.#..#.#..",
            "#..#.#.#.#..#..##...##...##....#.",
            "#.#...##.#..###....#.....#....##.",
            "#.#..#....##..#.#.####.#...##.###",
            "#..##.##.#...#.....##.#.######.##",
            "........#..#.##.#####.###...#..#.",
            "#######..##....###..#.###.#.####.",
            "#.....#.#.####.####.....#...#.###",
            "#.###.#.#..#..###..####.######..#",
            "#.###.#.#.##...#..#..#...#......#",
            "#.###.#.##...####...#.#.#....##..",
            "#.....#...#.#.##.....#.#.#.####..",
            "#######.###.#######...#.##.#####.",
        )
    }
}
