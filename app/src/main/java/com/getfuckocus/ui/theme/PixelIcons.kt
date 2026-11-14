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

    /** 완료 체크 표시 */
    val Check = pixelIcon(
        "Check",
        "............",
        "............",
        "..........##",
        ".........###",
        "........###.",
        "##.....###..",
        "###...###...",
        ".###.###....",
        "..#####.....",
        "...###......",
        "....#.......",
        "............",
    )

    /** 꽉 찬 원에 체크 모양을 뚫은 완료 배지. 체크는 위아래 가운데(3~8번째 줄)에 둔다 */
    val CheckCircle = pixelIcon(
        "CheckCircle",
        "...######...",
        ".##########.",
        ".##########.",
        "########..##",
        "#######..###",
        "#..###..####",
        "##..#..#####",
        "###...######",
        "####.#######",
        ".##########.",
        ".##########.",
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

private fun pixelIcon(name: String, vararg rows: String): ImageVector = pixelVector("Pixel.$name", rows.toList(), '#')

/**
 * 채움과 외곽선을 따로 그리는 아이콘. 채움만 tint로 칠하면 지금 [PixelIcons]처럼 한 색으로 쓸 수 있고,
 * 외곽선까지 깔면 밝은 바탕에서도 모양이 또렷하다.
 */
class TwoToneIcon(val fill: ImageVector, val outline: ImageVector) {
    /** 가로:세로 비율 (정사각형이 아닌 장식용) */
    val aspectRatio: Float = fill.viewportWidth / fill.viewportHeight
}

/** '#'은 채움, '+'는 외곽선 */
internal fun twoToneIcon(name: String, rows: List<String>): TwoToneIcon =
    TwoToneIcon(fill = pixelVector("$name.fill", rows, '#'), outline = pixelVector("$name.outline", rows, '+'))

/** [mark] 글자가 있는 칸을 칠한 격자 아이콘 */
private fun pixelVector(name: String, rows: List<String>, mark: Char): ImageVector {
    val width = rows.maxOf { it.length }.toFloat()
    val height = rows.size.toFloat()
    return ImageVector.Builder(
        name = name,
        defaultWidth = (24 * width / height).dp,
        defaultHeight = 24.dp,
        viewportWidth = width,
        viewportHeight = height,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            rows.forEachIndexed { y, row ->
                // 한 줄 안에서 이어진 칸은 사각형 하나로 묶어 경로 수를 줄인다
                var x = 0
                while (x < row.length) {
                    if (row[x] == mark) {
                        val start = x
                        while (x < row.length && row[x] == mark) x++
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
