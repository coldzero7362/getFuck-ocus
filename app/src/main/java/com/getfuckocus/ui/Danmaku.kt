package com.getfuckocus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfuckocus.ui.components.OutlinedDisplayText
import com.getfuckocus.ui.theme.Chrome
import com.getfuckocus.ui.theme.ChromeShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

/** 흘러가는 글자·사진이 다시 지나가는 간격 */
const val DANMAKU_INTERVAL_MS = 60_000L

/** 한 번 흘러갈 것 하나. 크기·높이·출발 시각·속도를 무작위로 정해 둔다. */
sealed interface Flyer {
    val id: Long
    /** 화면 높이 중 어디쯤 지나갈지 (0 위 ~ 1 아래) */
    val lane: Float
    val startDelayMs: Long
    val durationMs: Long

    data class Text(
        override val id: Long, val text: String, val sizeSp: Float,
        override val lane: Float, override val startDelayMs: Long, override val durationMs: Long,
    ) : Flyer

    data class Picture(
        override val id: Long, val path: String, val heightDp: Float,
        override val lane: Float, override val startDelayMs: Long, override val durationMs: Long,
    ) : Flyer
}

/** 한 번(1분마다 한 번)에 흘려보낼 것들을 정한다. 설정한 문구와 사진이 모두 한 번씩, 크기는 제각각으로. */
object DanmakuPlan {
    val TEXT_SIZE_SP = 18f..52f
    val PICTURE_HEIGHT_DP = 80f..190f
    val DURATION_MS = 6_000L..11_000L
    const val MAX_START_DELAY_MS = 8_000L
    /** 화면 맨 위·맨 아래에 붙지 않게 */
    val LANE = 0.04f..0.92f

    fun wave(texts: List<String>, pictures: List<String>, random: Random, idStart: Long = 0): List<Flyer> {
        var id = idStart
        val items = texts.ifEmpty { listOf(DEFAULT_NAG_MESSAGE) }.map { text ->
            Flyer.Text(
                id = id++, text = text,
                sizeSp = random.nextFloat(TEXT_SIZE_SP),
                lane = random.nextFloat(LANE),
                startDelayMs = random.nextLong(0, MAX_START_DELAY_MS + 1),
                durationMs = random.nextLong(DURATION_MS.first, DURATION_MS.last + 1),
            )
        } + pictures.map { path ->
            Flyer.Picture(
                id = id++, path = path,
                heightDp = random.nextFloat(PICTURE_HEIGHT_DP),
                lane = random.nextFloat(LANE),
                startDelayMs = random.nextLong(0, MAX_START_DELAY_MS + 1),
                durationMs = random.nextLong(DURATION_MS.first, DURATION_MS.last + 1),
            )
        }
        return items.shuffled(random)
    }

    private fun Random.nextFloat(range: ClosedFloatingPointRange<Float>) =
        range.start + nextFloat() * (range.endInclusive - range.start)
}

/**
 * 동영상 댓글처럼 글자와 사진이 화면을 왼쪽에서 오른쪽으로 가로질러 흘러가는 층.
 * 화면이 열리면 바로 한 번, 그 뒤 [DANMAKU_INTERVAL_MS]마다 다시 흘려보낸다.
 * 터치는 받지 않아 아래 버튼(인증, 비상 해제, 긴급전화)을 가리지 않는다.
 */
@Composable
fun DanmakuLayer(texts: List<String>, pictures: List<String>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        val active = remember { mutableStateListOf<Flyer>() }
        LaunchedEffect(texts, pictures) {
            val random = Random(System.nanoTime())
            var nextId = 0L
            while (true) {
                val wave = DanmakuPlan.wave(texts, pictures, random, nextId)
                nextId += wave.size
                wave.forEach { flyer ->
                    launch {
                        delay(flyer.startDelayMs)
                        active += flyer
                        delay(flyer.durationMs + 200)
                        active -= flyer
                    }
                }
                delay(DANMAKU_INTERVAL_MS)
            }
        }
        active.forEach { flyer ->
            key(flyer.id) { FlyingItem(flyer, widthPx, heightPx) }
        }
    }
}

@Composable
private fun FlyingItem(flyer: Flyer, containerWidth: Int, containerHeight: Int) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(flyer.durationMs.toInt(), easing = LinearEasing))
    }
    // 왼쪽 화면 밖에서 시작해 오른쪽 화면 밖으로 나간다. 위치는 그리기 직전(배치 단계)에만 읽어 다시 짜지 않는다
    val flight = Modifier.layout { measurable, _ ->
        val placeable = measurable.measure(Constraints())
        layout(placeable.width, placeable.height) {
            val x = -placeable.width + (containerWidth + placeable.width) * progress.value
            val y = (containerHeight - placeable.height).coerceAtLeast(0) * flyer.lane
            placeable.place(x.roundToInt(), y.roundToInt())
        }
    }
    when (flyer) {
        is Flyer.Text -> OutlinedDisplayText(
            flyer.text,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = flyer.sizeSp.sp),
            modifier = flight,
            outlineWidth = (flyer.sizeSp / 9f).coerceIn(2f, 6f).dp,
            shadowOffset = (flyer.sizeSp / 14f).coerceIn(1.5f, 4f).dp,
        )
        is Flyer.Picture -> {
            val image = rememberNagImage(flyer.path, maxSide = 480) ?: return
            val height = flyer.heightDp.dp
            Box(flight) {
                Image(
                    image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(height)
                        .width(height * (image.width.toFloat() / image.height).coerceIn(0.6f, 1.8f))
                        .clip(ChromeShapes.control)
                        .border(3.dp, Chrome.carbon, ChromeShapes.control),
                )
            }
        }
    }
}
