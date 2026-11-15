package com.getfuckocus.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * 화면 스킨. 색과 몇몇 부품 모양만 바뀌고, 화면 구조·동작은 같다.
 * 이름([name])을 설정에 그대로 저장하므로 바꾸지 않는다.
 */
enum class Skin(val label: String, val caption: String) {
    NEWTRO("뉴트로", "2001년 게임기 메뉴 · 블루 라벤더"),
    CASSETTE("카세트 퓨처리즘", "70~80년대 단말기 · 베이지 몸체와 CRT 화면"),
}

/** 한 스킨의 색. [Chrome]의 역할 이름을 그대로 따른다. */
data class Palette(
    val signal: Color,
    val amber: Color,
    val navGold: Color,
    val canvas: Color,
    val periwinkle: Color,
    val sky: Color,
    val lavender: Color,
    val ice: Color,
    val chromeIndigo: Color,
    val mutedIndigo: Color,
    val platinum: Color,
    val white: Color,
    val carbon: Color,
    val hairline: Color,
    val teal: Color,
    val success: Color,
    val warning: Color,
    val warningSoft: Color,
    val red: Color,
    val redSoft: Color,
    val redInk: Color,
    val highlight: Color,
    /** CRT 화면 바탕과 형광 글자 (카세트 스킨의 화면 칸에서 쓴다) */
    val screen: Color,
    val phosphor: Color,
    val phosphorDim: Color,
    val phosphorAmber: Color,
    /** 잠금 화면 */
    val lockContent: Color,
    val lockRaised: Color,
)

internal val NewtroPalette = Palette(
    signal = Color(0xFFF49A24),
    amber = Color(0xFFF2B54A),
    navGold = Color(0xFFF49A24),
    canvas = Color(0xFF8094C5),
    periwinkle = Color(0xFF94A6D3),
    sky = Color(0xFFA5BCE5),
    lavender = Color(0xFFB8B7E7),
    ice = Color(0xFFCFEBDD),
    chromeIndigo = Color(0xFF34477F),
    mutedIndigo = Color(0xFF5E6496),
    platinum = Color(0xFFE2E0DE),
    white = Color(0xFFFFFFFF),
    carbon = Color(0xFF171E35),
    hairline = Color(0xFF5A5F8C),
    teal = Color(0xFF1F7A58),
    success = Color(0xFF299B71),
    warning = Color(0xFFE9B94A),
    warningSoft = Color(0xFFF7E7BE),
    red = Color(0xFFD94B4B),
    redSoft = Color(0xFFF6D5D2),
    redInk = Color(0xFF8A1F1F),
    highlight = Color(0x99FFFFFF),
    screen = Color(0xFF171E35),
    phosphor = Color(0xFFF49A24),
    phosphorDim = Color(0xFFA5BCE5),
    phosphorAmber = Color(0xFFF2B54A),
    lockContent = Color(0xFFF1F0F7),
    lockRaised = Color(0xFF232C4A),
)

/** 카세트 퓨처리즘: 퍼티 베이지 몸체, 짙은 갈색 패널, 검은 CRT에 형광 녹·앰버 글자, 번트 오렌지 키 */
internal val CassettePalette = Palette(
    signal = Color(0xFFE0662A),
    amber = Color(0xFFF2B632),
    navGold = Color(0xFFFFB23F),
    canvas = Color(0xFFD6CDB8),
    periwinkle = Color(0xFFCFC5AE),
    sky = Color(0xFFE6DECB),
    lavender = Color(0xFFE9E1CD),
    ice = Color(0xFFC9E8CF),
    chromeIndigo = Color(0xFF5A5043),
    mutedIndigo = Color(0xFF7E7462),
    platinum = Color(0xFFF4EFE3),
    white = Color(0xFFFFFDF7),
    carbon = Color(0xFF2B2620),
    hairline = Color(0xFF8F8574),
    teal = Color(0xFF1E6B3A),
    success = Color(0xFF3DBB6A),
    warning = Color(0xFFF2B632),
    warningSoft = Color(0xFFF1E3B0),
    red = Color(0xFFC8442A),
    redSoft = Color(0xFFF3D3C6),
    redInk = Color(0xFF8A2B12),
    highlight = Color(0x99FFFFFF),
    screen = Color(0xFF0E120F),
    phosphor = Color(0xFF7DFF9E),
    phosphorDim = Color(0xFF5FC57E),
    phosphorAmber = Color(0xFFFFB23F),
    lockContent = Color(0xFFE9DFC8),
    lockRaised = Color(0xFF3A332B),
)

/** 형광 글자처럼 살짝 번지게 한다 (CRT 칸 안의 글자에만) */
fun phosphorGlow(color: Color) = androidx.compose.ui.graphics.Shadow(color = color.copy(alpha = 0.6f), blurRadius = 14f)

/**
 * 지금 고른 스킨. 앱 전체(메인 화면, 잠금 화면, 억제 오버레이)가 같은 프로세스 상태를 본다.
 * Compose 상태라서 바꾸는 즉시 [Chrome] 색을 읽는 화면이 다시 그려진다.
 */
object SkinState {
    private const val PREFS = "ui"
    private const val KEY_SKIN = "skin"

    var skin: Skin by mutableStateOf(Skin.NEWTRO)
        private set

    val palette: Palette
        get() = when (skin) {
            Skin.NEWTRO -> NewtroPalette
            Skin.CASSETTE -> CassettePalette
        }

    val isCassette: Boolean get() = skin == Skin.CASSETTE

    /** 앱 시작 때 한 번 저장된 스킨을 읽는다 */
    fun load(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SKIN, null)
        skin = Skin.entries.firstOrNull { it.name == saved } ?: Skin.NEWTRO
    }

    fun set(context: Context, value: Skin) {
        skin = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SKIN, value.name).apply()
    }
}
