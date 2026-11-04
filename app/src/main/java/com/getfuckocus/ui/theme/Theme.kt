package com.getfuckocus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * 앱 고유 팔레트: 집중을 뜻하는 남색(primary) + 진행·경고를 뜻하는 주황(secondary).
 * 과제 상태 색은 역할에 고정한다.
 * - secondaryContainer: 진행 중
 * - tertiaryContainer: 완료
 * - errorContainer: 놓침 / 잠금
 * - surfaceVariant: 시작 전 / 꺼짐
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF2B3A67),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF0E1A3F),
    secondary = Color(0xFF8A5A00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDDA6),
    onSecondaryContainer = Color(0xFF2B1700),
    tertiary = Color(0xFF2E6B4F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB2F0CF),
    onTertiaryContainer = Color(0xFF002114),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9F9FE),
    onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFF9F9FE),
    onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFE2E2EC),
    onSurfaceVariant = Color(0xFF45464F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3F9),
    surfaceContainer = Color(0xFFEDEDF3),
    surfaceContainerHigh = Color(0xFFE7E7EE),
    surfaceContainerHighest = Color(0xFFE2E2E8),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC6C6D0),
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFF1F0F7),
    inversePrimary = Color(0xFFB8C4FF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8C4FF),
    onPrimary = Color(0xFF1F2D5A),
    primaryContainer = Color(0xFF36457A),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFFFB94C),
    onSecondary = Color(0xFF462A00),
    secondaryContainer = Color(0xFF643F00),
    onSecondaryContainer = Color(0xFFFFDDA6),
    tertiary = Color(0xFF96D4B4),
    onTertiary = Color(0xFF003825),
    tertiaryContainer = Color(0xFF135238),
    onTertiaryContainer = Color(0xFFB2F0CF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE3E2E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE3E2E9),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C6D0),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF45464F),
    inverseSurface = Color(0xFFE3E2E9),
    inverseOnSurface = Color(0xFF2F3036),
    inversePrimary = Color(0xFF2B3A67),
)

/** 잠금 화면은 라이트/다크와 관계없이 같은 짙은 남색으로 보여 "잠긴 상태"가 바로 구분되게 한다. */
object LockColors {
    val background = Color(0xFF151A33)
    val content = Color(0xFFF1F0F7)
    val contentMuted = Color(0xB3F1F0F7)
    val accent = Color(0xFFFFB94C)
    val onAccent = Color(0xFF2B1700)
    val track = Color(0x26F1F0F7)
    val badge = Color(0xFF93000A)
    val onBadge = Color(0xFFFFDAD6)
}

@Composable
fun GetFocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
