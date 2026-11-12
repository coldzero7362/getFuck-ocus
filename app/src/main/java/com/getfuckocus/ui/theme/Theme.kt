package com.getfuckocus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    // 리디자인 팔레트 (2001 Nintendo Newtro 가이드). 이름은 역할로 유지하고 값만 맞춘다.
    val signal = Color(0xFFF49A24)       // 닌텐도 오렌지: 앞으로 가기, 제출, 인증
    val amber = Color(0xFFF2B54A)        // 도구 버튼, 선택된 칩, 배지
    val navGold = Color(0xFFF49A24)      // 상단 바 제목
    val canvas = Color(0xFF8094C5)       // 메인 배경 블루 라벤더
    val periwinkle = Color(0xFF94A6D3)   // 한 단 올라온 판
    val sky = Color(0xFFA5BCE5)          // 패널 헤더 라이트 블루, 보조 띠
    val lavender = Color(0xFFB8B7E7)     // 보조 강조, 요약 카드
    val ice = Color(0xFFCFEBDD)          // 성공 배지 바탕
    val chromeIndigo = Color(0xFF34477F) // 판 아래 그림자 선, 보조 글자
    val mutedIndigo = Color(0xFF5E6496)
    val platinum = Color(0xFFE2E0DE)     // 카드 배경 웜 그레이
    val white = Color(0xFFFFFFFF)
    val carbon = Color(0xFF171E35)       // 헤더 딥 네이비, 기본 글자
    val hairline = Color(0xFF5A5F8C)
    val teal = Color(0xFF1F7A58)         // 성공 글자 (그린 #299B71보다 한 단 짙게 해서 대비 확보)
    val success = Color(0xFF299B71)      // 성공
    val warning = Color(0xFFE9B94A)      // 경고
    val warningSoft = Color(0xFFF7E7BE)  // 경고 바탕
    val red = Color(0xFFD94B4B)          // 실패, 위험, 놓침
    val redSoft = Color(0xFFF6D5D2)      // 실패 바탕
    val redInk = Color(0xFF8A1F1F)       // 실패 바탕 위 글자
    val highlight = Color(0x99FFFFFF)    // 판 위쪽 밝은 테두리
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
private val ChromeColors = lightColorScheme(
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
    val background = Chrome.carbon
    val content = Color(0xFFF1F0F7)
    val contentMuted = Chrome.sky
    /** 정상 해제(인증)가 가장 중요한 행동이라 앱 전체의 주 버튼과 같은 오렌지를 쓴다 */
    val accent = Chrome.signal
    val onAccent = Chrome.carbon
    /** 자동 해제 링 */
    val ring = Chrome.amber
    /** 카본 위에 한 단 올라온 판 (사유 카드, 긴급전화 칸) */
    val raised = Color(0xFF232C4A)
    val track = Color(0x33F1F0F7)
    /** 주 버튼(앰버) 다음가는 버튼: 카본보다 한 단 밝은 남색 판 */
    val secondary = Chrome.chromeIndigo
    val badge = Chrome.red
    val onBadge = Chrome.white
}

@Composable
fun GetFocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ChromeColors, typography = ChromeTypography, content = content)
}
