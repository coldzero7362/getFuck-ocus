package com.getfuckocus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.getfuckocus.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * "콘솔 크롬" 디자인 언어 (design/DESIGN.md, 2001년 게임기 웹사이트에서 영감).
 * - 화면 전체가 페리윙클 금속판이고, 모든 영역은 위쪽이 밝고 아래쪽에 남색 그림자 선이 있는 판(plate)이다.
 * - 짙은 카본 남색 판은 "조작 계층": 상단 바, 잠금 화면.
 * - 따뜻한 색은 "누르면 앞으로 간다"는 뜻으로만 쓴다: 시그널 오렌지(진행·제출), 앰버(도구·배지).
 * - 모서리는 기본적으로 날카롭거나 45도로 잘라낸다. 둥근 모양은 원형 버튼과 배지에만 쓴다.
 */
object Chrome {
    // 값은 지금 고른 스킨([SkinState])의 팔레트에서 읽는다. 이름은 역할이라 스킨이 바뀌어도 그대로 쓴다.
    val signal: Color get() = SkinState.palette.signal
    val amber: Color get() = SkinState.palette.amber
    val navGold: Color get() = SkinState.palette.navGold
    val canvas: Color get() = SkinState.palette.canvas
    val periwinkle: Color get() = SkinState.palette.periwinkle
    val sky: Color get() = SkinState.palette.sky
    val lavender: Color get() = SkinState.palette.lavender
    val ice: Color get() = SkinState.palette.ice
    val chromeIndigo: Color get() = SkinState.palette.chromeIndigo
    val mutedIndigo: Color get() = SkinState.palette.mutedIndigo
    val platinum: Color get() = SkinState.palette.platinum
    val white: Color get() = SkinState.palette.white
    val carbon: Color get() = SkinState.palette.carbon
    val hairline: Color get() = SkinState.palette.hairline
    val teal: Color get() = SkinState.palette.teal
    val success: Color get() = SkinState.palette.success
    val warning: Color get() = SkinState.palette.warning
    val warningSoft: Color get() = SkinState.palette.warningSoft
    val red: Color get() = SkinState.palette.red
    val redSoft: Color get() = SkinState.palette.redSoft
    val redInk: Color get() = SkinState.palette.redInk
    val highlight: Color get() = SkinState.palette.highlight
    /** CRT 화면 칸 (카세트 스킨) */
    val screen: Color get() = SkinState.palette.screen
    val phosphor: Color get() = SkinState.palette.phosphor
    val phosphorDim: Color get() = SkinState.palette.phosphorDim
    val phosphorAmber: Color get() = SkinState.palette.phosphorAmber
}

/** 간격·크기 토큰. 화면마다 숫자를 새로 정하지 않고 여기서 고른다. */
object Dimens {
    val screenPadding = 16.dp
    val sectionGap = 16.dp
    val cardGap = 12.dp
    val cardPadding = 16.dp
    val touchTarget = 48.dp
    val topBarHeight = 60.dp
    /** FAB(64 + 그림자 6) + 아래 여백 + 쪽 표시가 겹치지 않게 목록 끝에 두는 여백 */
    val fabClearance = 120.dp
}

/*
 * 과제 상태 색은 역할에 고정한다.
 * - secondaryContainer(앰버): 진행 중, 선택됨
 * - tertiaryContainer(아이스 + 틸 글자): 완료
 * - errorContainer: 놓침 / 잠금
 * - surfaceVariant(스카이): 시작 전 / 꺼짐
 * 하드웨어 외형을 흉내 내는 디자인이라 라이트/다크 모드와 관계없이 같은 색을 쓴다.
 */
private fun chromeColors() = lightColorScheme(
    primary = Chrome.chromeIndigo,
    onPrimary = Chrome.white,
    primaryContainer = Chrome.lavender,
    onPrimaryContainer = Chrome.carbon,
    secondary = Chrome.signal,
    onSecondary = Chrome.carbon,
    secondaryContainer = Chrome.amber,
    onSecondaryContainer = Chrome.carbon,
    tertiary = Chrome.teal,
    onTertiary = Chrome.white,
    tertiaryContainer = Chrome.ice,
    onTertiaryContainer = Chrome.teal,
    error = Chrome.red,
    onError = Chrome.white,
    errorContainer = Chrome.redSoft,
    onErrorContainer = Chrome.redInk,
    background = Chrome.canvas,
    onBackground = Chrome.carbon,
    surface = Chrome.canvas,
    onSurface = Chrome.carbon,
    surfaceVariant = Chrome.sky,
    onSurfaceVariant = Chrome.chromeIndigo,
    surfaceContainerLowest = Chrome.white,
    surfaceContainerLow = Chrome.platinum,
    surfaceContainer = Chrome.periwinkle,
    surfaceContainerHigh = Chrome.white,
    surfaceContainerHighest = Chrome.white,
    outline = Chrome.hairline,
    outlineVariant = Color(0xFFA9B0D2),
    inverseSurface = Chrome.carbon,
    inverseOnSurface = Chrome.sky,
    inversePrimary = Chrome.amber,
)

private val Sans = FontFamily.SansSerif

/**
 * 포인트 글꼴: 물마루 (Mushsooni, SIL OFL 1.1 — assets/licenses/Mulmaru-OFL.txt).
 * 굵기가 하나뿐인 픽셀 글꼴이라 굵게 지정해도 억지로 두껍게 그리지 않도록 같은 파일을 모든 굵기에 둔다.
 */
private fun pixelFamily(resId: Int) = FontFamily(
    Font(resId, FontWeight.Normal),
    Font(resId, FontWeight.Medium),
    Font(resId, FontWeight.SemiBold),
    Font(resId, FontWeight.Bold),
    Font(resId, FontWeight.ExtraBold),
    Font(resId, FontWeight.Black),
)

/** 제목·큰 글자·흘러가는 문구에 쓰는 픽셀 글꼴 */
val Mulmaru = pixelFamily(R.font.mulmaru)

/** 숫자 점수판에 쓰는 고정폭 픽셀 글꼴 */
val MulmaruMono = pixelFamily(R.font.mulmaru_mono)

/**
 * 제목·큰 글자는 픽셀 글꼴(물마루)로 포인트를 주고, 본문·설명·버튼은 읽기 쉬운 산세리프로 둔다.
 * 라벨은 자간을 살짝 벌린다 (실크스크린으로 찍은 버튼 글씨 느낌).
 */
private val ChromeTypography = Typography().let { base ->
    fun TextStyle.sans(weight: FontWeight? = null, tracking: Float? = null) = copy(
        fontFamily = Sans,
        fontWeight = weight ?: fontWeight,
        letterSpacing = tracking?.sp ?: letterSpacing,
    )
    fun TextStyle.pixel() = copy(fontFamily = Mulmaru, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    // 가이드 크기: 화면 제목 22, 카드 제목 18, 본문 15, 보조 13, 버튼 15sp. 레트로라고 글자를 줄이지 않는다
    Typography(
        displayLarge = base.displayLarge.pixel(),
        displayMedium = base.displayMedium.pixel(),
        displaySmall = base.displaySmall.pixel(),
        headlineLarge = base.headlineLarge.pixel(),
        headlineMedium = base.headlineMedium.pixel(),
        headlineSmall = base.headlineSmall.pixel().copy(fontSize = 24.sp, lineHeight = 32.sp),
        titleLarge = base.titleLarge.pixel().copy(fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = base.titleMedium.pixel().copy(fontSize = 18.sp, lineHeight = 24.sp),
        titleSmall = base.titleSmall.sans(FontWeight.Bold, 0.3f).copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodyLarge = base.bodyLarge.sans().copy(fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = base.bodyMedium.sans().copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodySmall = base.bodySmall.sans().copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = base.labelLarge.sans(FontWeight.Bold, 0.3f).copy(fontSize = 15.sp, lineHeight = 20.sp),
        labelMedium = base.labelMedium.sans(FontWeight.Bold, 0.3f).copy(fontSize = 13.sp, lineHeight = 17.sp),
        labelSmall = base.labelSmall.sans(FontWeight.Bold, 0.3f).copy(fontSize = 12.sp, lineHeight = 16.sp),
    )
}

/** 숫자·시간·짧은 강조에만 쓰는 고정폭 글꼴. 게임 화면의 점수판 느낌을 낸다. */
val DigitStyle = TextStyle(fontFamily = MulmaruMono, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)

/** 잠금 화면: 짙은 카본 판에 앰버 강조. 앱의 "조작 계층"과 같은 색이다. */
object LockColors {
    val background: Color get() = Chrome.carbon
    val content: Color get() = SkinState.palette.lockContent
    val contentMuted: Color get() = Chrome.sky
    /** 정상 해제(인증)가 가장 중요한 행동이라 앱 전체의 주 버튼과 같은 오렌지를 쓴다 */
    val accent: Color get() = Chrome.signal
    val onAccent: Color get() = Chrome.carbon
    /** 자동 해제 링 */
    val ring: Color get() = Chrome.amber
    /** 카본 위에 한 단 올라온 판 (사유 카드, 긴급전화 칸) */
    val raised: Color get() = SkinState.palette.lockRaised
    val track = Color(0x33F1F0F7)
    /** 주 버튼(앰버) 다음가는 버튼: 카본보다 한 단 밝은 남색 판 */
    val secondary: Color get() = Chrome.chromeIndigo
    val badge: Color get() = Chrome.red
    val onBadge: Color get() = Chrome.white
}

@Composable
fun GetFocusTheme(content: @Composable () -> Unit) {
    // 스킨이 바뀌면 Material 색표도 새 팔레트로 다시 만든다
    val skin = SkinState.skin
    val colors = remember(skin) { chromeColors() }
    MaterialTheme(colorScheme = colors, typography = ChromeTypography, content = content)
}
