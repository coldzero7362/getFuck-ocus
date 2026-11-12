package com.getfuckocus.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
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
    /** 1초에 몇 dp씩 지나갈지. 크기와 상관없이 정해서 큰 글자도 빨리 지나갈 수 있다 */
    val speedDpPerSec: Float

    data class Text(
        override val id: Long, val text: String, val sizeSp: Float,
        override val lane: Float, override val startDelayMs: Long, override val speedDpPerSec: Float,
    ) : Flyer

    data class Picture(
        override val id: Long, val path: String, val heightDp: Float,
        override val lane: Float, override val startDelayMs: Long, override val speedDpPerSec: Float,
    ) : Flyer
}

/**
 * 한 번(1분마다 한 번)에 흘려보낼 것들을 정한다.
 * 설정한 문구·사진마다 1~[MAX_COPIES]개씩 복제해서, 같은 문구가 여러 개 동시에 지나갈 수 있다.
 * 복제본도 저마다 크기·높이·출발 시각·속도가 다르다. 크기와 속도는 아래 상한을 넘지 않는다.
 */
object DanmakuPlan {
    const val MAX_COPIES = 5
    const val MIN_TEXT_SIZE_SP = 16f
    const val MAX_TEXT_SIZE_SP = 56f
    const val MIN_PICTURE_HEIGHT_DP = 70f
    const val MAX_PICTURE_HEIGHT_DP = 200f
    const val MIN_SPEED_DP_PER_SEC = 70f
    const val MAX_SPEED_DP_PER_SEC = 360f
    const val MAX_START_DELAY_MS = 10_000L
    /** 화면 맨 위·맨 아래에 붙지 않게 */
    val LANE = 0.04f..0.92f

    fun wave(texts: List<String>, pictures: List<String>, random: Random, idStart: Long = 0): List<Flyer> {
        var id = idStart
        fun copies() = random.nextInt(1, MAX_COPIES + 1)
        fun delay() = random.nextLong(0, MAX_START_DELAY_MS + 1)
        fun speed() = random.nextFloat(MIN_SPEED_DP_PER_SEC, MAX_SPEED_DP_PER_SEC)
        val items = texts.ifEmpty { listOf(DEFAULT_NAG_MESSAGE) }.flatMap { text ->
            List(copies()) {
                Flyer.Text(
                    id = id++, text = text,
                    sizeSp = random.nextFloat(MIN_TEXT_SIZE_SP, MAX_TEXT_SIZE_SP),
                    lane = random.nextFloat(LANE.start, LANE.endInclusive),
                    startDelayMs = delay(),
                    speedDpPerSec = speed(),
                )
            }
        } + pictures.flatMap { path ->
            List(copies()) {
                Flyer.Picture(
                    id = id++, path = path,
                    heightDp = random.nextFloat(MIN_PICTURE_HEIGHT_DP, MAX_PICTURE_HEIGHT_DP),
                    lane = random.nextFloat(LANE.start, LANE.endInclusive),
                    startDelayMs = delay(),
                    speedDpPerSec = speed(),
                )
            }
        }
        return items.shuffled(random)
    }

    private fun Random.nextFloat(min: Float, max: Float) = min + nextFloat() * (max - min)
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
                    }
                }
                delay(DANMAKU_INTERVAL_MS)
            }
        }
        active.forEach { flyer ->
            key(flyer.id) { FlyingItem(flyer, widthPx, heightPx, onGone = { active -= flyer }) }
        }
    }
}

/** 정해진 속도로 왼쪽 화면 밖에서 오른쪽 화면 밖까지 지나가고, 다 빠져나가면 [onGone]을 부른다 */
@Composable
private fun FlyingItem(flyer: Flyer, containerWidth: Int, containerHeight: Int, onGone: () -> Unit) {
    val speedPx = with(LocalDensity.current) { flyer.speedDpPerSec.dp.toPx() }
    // 지나온 거리(px). 크기를 재기 전에도 계속 늘어나고, 화면 밖으로 다 나가면 멈춘다
    var travelled by remember { mutableFloatStateOf(0f) }
    var itemWidth by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            travelled = (now - start) / 1_000_000_000f * speedPx
            if (itemWidth > 0 && travelled > containerWidth + itemWidth) break
        }
        onGone()
    }
    // 위치는 그리기 직전(배치 단계)에만 읽어 화면을 다시 짜지 않는다
    val flight = Modifier.layout { measurable, _ ->
        val placeable = measurable.measure(Constraints())
        itemWidth = placeable.width
        layout(placeable.width, placeable.height) {
            val x = -placeable.width + travelled
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
