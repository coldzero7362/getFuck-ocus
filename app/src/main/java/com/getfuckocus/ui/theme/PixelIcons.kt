package com.getfuckocus.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 초기 게임기 메뉴처럼 점을 찍어 만든 아이콘. 12×12 격자에서 '#' 칸을 칠한다.
 * Material 기본 아이콘에 없는 모양(카메라, 시계, NFC 등)과 게임 느낌을 살릴 곳에 쓴다.
 * 색은 Icon의 tint로 바뀌므로 여기서는 검은색으로만 칠한다.
 */
object PixelIcons {
    val Camera = pixelIcon(
        "Camera",
        "............",
        "....####....",
        ".##########.",
        "############",
        "####....####",
        "###......###",
        "###..##..###",
        "###..##..###",
        "###......###",
        "####....####",
        "############",
        "............",
    )

    val Clock = pixelIcon(
        "Clock",
        "....####....",
        "..##....##..",
        ".#........#.",
        ".#...#....#.",
        "#....#.....#",
        "#....#.....#",
        "#....####..#",
        "#..........#",
        ".#........#.",
        ".#........#.",
        "..##....##..",
        "....####....",
    )

    val Hourglass = pixelIcon(
        "Hourglass",
        "############",
        ".#........#.",
        ".#........#.",
        "..#.####.#..",
        "...#.##.#...",
        "....#..#....",
        "....#..#....",
        "...#....#...",
        "..#..##..#..",
        ".#.######.#.",
        ".#.######.#.",
        "############",
    )

    val Nfc = pixelIcon(
        "Nfc",
        "............",
        ".#........#.",
        "#..#....#..#",
        "#.#..##..#.#",
        "#.#.#..#.#.#",
        "#.#.#..#.#.#",
        "#.#.#..#.#.#",
        "#.#.#..#.#.#",
        "#.#..##..#.#",
        "#..#....#..#",
        ".#........#.",
        "............",
    )

    val Flag = pixelIcon(
        "Flag",
        "##..........",
        "########....",
        "##########..",
        "############",
        "##########..",
        "########....",
        "##..........",
        "##..........",
        "##..........",
        "##..........",
        "##..........",
        "####........",
    )

    val Shield = pixelIcon(
        "Shield",
        "############",
        "#..........#",
        "#....##....#",
        "#....##....#",
        "#..######..#",
        "#..######..#",
        "#....##....#",
        "#....##....#",
        ".#........#.",
        "..#......#..",
        "...##..##...",
        ".....##.....",
    )

    val Apps = pixelIcon(
        "Apps",
        "............",
        ".####..####.",
        ".####..####.",
        ".####..####.",
        ".####..####.",
        "............",
        "............",
        ".####..####.",
        ".####..####.",
        ".####..####.",
        ".####..####.",
        "............",
    )

    val Bell = pixelIcon(
        "Bell",
        ".....##.....",
        "....####....",
        "...######...",
        "..########..",
        "..########..",
        "..########..",
        "..########..",
        ".##########.",
        "############",
        "............",
        "....####....",
        ".....##.....",
    )

    val Info = pixelIcon(
        "Info",
        "...######...",
        "..#......#..",
        ".#...##...#.",
        "#..........#",
        "#...###....#",
        "#....##....#",
        "#....##....#",
        "#....##....#",
        "#...####...#",
        ".#........#.",
        "..#......#..",
        "...######...",
    )

    val Sparkle = pixelIcon(
        "Sparkle",
        ".....##.....",
        ".....##.....",
        "....####....",
        "....####....",
        "############",
        ".##########.",
        "..########..",
        "...######...",
        "...##..##...",
        "..##....##..",
        "..#......#..",
        "............",
    )
}

private fun pixelIcon(name: String, vararg rows: String): ImageVector {
    val size = rows.size.toFloat()
    return ImageVector.Builder(
        name = "Pixel.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = size,
        viewportHeight = size,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            rows.forEachIndexed { y, row ->
                // 한 줄 안에서 이어진 칸은 사각형 하나로 묶어 경로 수를 줄인다
                var x = 0
                while (x < row.length) {
                    if (row[x] == '#') {
                        val start = x
                        while (x < row.length && row[x] == '#') x++
                        rect(start.toFloat(), y.toFloat(), x.toFloat(), y + 1f)
                    } else {
                        x++
                    }
                }
            }
        }
    }.build()
}

private fun PathBuilder.rect(left: Float, top: Float, right: Float, bottom: Float) {
    moveTo(left, top)
    lineTo(right, top)
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}
