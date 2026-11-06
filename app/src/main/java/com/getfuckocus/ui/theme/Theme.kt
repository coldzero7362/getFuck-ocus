package com.getfuckocus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * "콘솔 크롬" 디자인 언어 (design/DESIGN.md, 2001년 게임기 웹사이트에서 영감).
 * - 화면 전체가 페리윙클 금속판이고, 모든 영역은 위쪽이 밝고 아래쪽에 남색 그림자 선이 있는 판(plate)이다.
 * - 짙은 카본 남색 판은 "조작 계층": 상단 바, 잠금 화면.
 * - 따뜻한 색은 "누르면 앞으로 간다"는 뜻으로만 쓴다: 시그널 오렌지(진행·제출), 앰버(도구·배지).
 * - 모서리는 기본적으로 날카롭거나 45도로 잘라낸다. 둥근 모양은 원형 버튼과 배지에만 쓴다.
 */
object Chrome {
    val signal = Color(0xFFF68D1F)       // 앞으로 가기, 제출, 인증
    val amber = Color(0xFFECAB37)        // 도구 버튼, 선택된 칩, 배지
    val navGold = Color(0xFFE48600)      // 상단 바 제목
    val canvas = Color(0xFF7A8ABA)       // 앱 바탕 금속판
    val periwinkle = Color(0xFF8BA1D4)   // 한 단 올라온 판
    val sky = Color(0xFF9FBEE7)          // 보조 띠, 연한 판
    val lavender = Color(0xFFACACE7)     // 요약 카드
    val ice = Color(0xFFC0D5E6)          // 완료 배지 바탕
    val chromeIndigo = Color(0xFF3D4F97) // 판 아래 그림자 선, 보조 글자
    val mutedIndigo = Color(0xFF60619C)
    val platinum = Color(0xFFDEDEDE)     // 내용 판
    val white = Color(0xFFFFFFFF)
    val carbon = Color(0xFF21242E)       // 조작 계층, 본문 글자
    val hairline = Color(0xFF5A5F8C)
    val teal = Color(0xFF206479)         // 완료
    val red = Color(0xFFE60012)          // 오류, 위험, 놓침
    val highlight = Color(0x99FFFFFF)    // 판 위쪽 밝은 테두리
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
    errorContainer = Color(0xFFFFD6D2),
    onErrorContainer = Color(0xFF7A0009),
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

/** 굵은 산세리프 + 라벨은 자간을 살짝 벌린다 (실크스크린으로 찍은 버튼 글씨 느낌). */
private val ChromeTypography = Typography().let { base ->
    fun TextStyle.sans(weight: FontWeight? = null, tracking: Float? = null) = copy(
        fontFamily = Sans,
        fontWeight = weight ?: fontWeight,
        letterSpacing = tracking?.sp ?: letterSpacing,
    )
    Typography(
        displayLarge = base.displayLarge.sans(FontWeight.Black),
        displayMedium = base.displayMedium.sans(FontWeight.Black),
        displaySmall = base.displaySmall.sans(FontWeight.Black),
        headlineLarge = base.headlineLarge.sans(FontWeight.Black),
        headlineMedium = base.headlineMedium.sans(FontWeight.Black),
        headlineSmall = base.headlineSmall.sans(FontWeight.Bold),
        titleLarge = base.titleLarge.sans(FontWeight.Bold),
        titleMedium = base.titleMedium.sans(FontWeight.Bold, 0.5f),
        titleSmall = base.titleSmall.sans(FontWeight.Bold, 0.5f),
        bodyLarge = base.bodyLarge.sans(),
        bodyMedium = base.bodyMedium.sans(),
        bodySmall = base.bodySmall.sans(),
        labelLarge = base.labelLarge.sans(FontWeight.Bold, 0.5f),
        labelMedium = base.labelMedium.sans(FontWeight.Bold, 0.5f),
        labelSmall = base.labelSmall.sans(FontWeight.Bold, 0.5f),
    )
}

/** 잠금 화면: 짙은 카본 판에 앰버 강조. 앱의 "조작 계층"과 같은 색이다. */
object LockColors {
    val background = Chrome.carbon
    val content = Color(0xFFF1F0F7)
    val contentMuted = Chrome.sky
    val accent = Chrome.amber
    val onAccent = Chrome.carbon
    val track = Color(0x33F1F0F7)
    val badge = Chrome.red
    val onBadge = Chrome.white
}

@Composable
fun GetFocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ChromeColors, typography = ChromeTypography, content = content)
}
