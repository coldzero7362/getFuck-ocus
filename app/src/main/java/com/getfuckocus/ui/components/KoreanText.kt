package com.getfuckocus.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/** 줄바꿈을 막는 보이지 않는 문자 (U+2060 WORD JOINER) */
private const val WORD_JOINER = '⁠'

/**
 * 한국어를 어절 단위로만 줄바꿈하게 만든다 (CSS `word-break: keep-all`과 같은 효과).
 *
 * Android 기본 줄바꿈은 한글을 글자마다 끊을 수 있어서 "지나가/요", "문/구가"처럼 낱말이 갈라진다.
 * 한글이 붙은 두 글자 사이에 WORD JOINER를 넣어 띄어쓰기에서만 끊기게 한다.
 * 한 어절이 한 줄보다 길면 줄바꿈 엔진이 알아서 글자 단위로 끊는다.
 */
fun String.keepKoreanWords(): String {
    if (length < 2) return this
    val out = StringBuilder(length + length / 2)
    for (i in indices) {
        val c = this[i]
        out.append(c)
        if (i == lastIndex) break
        val next = this[i + 1]
        if (!c.isWhitespace() && !next.isWhitespace() && (c.isHangul() || next.isHangul())) {
            out.append(WORD_JOINER)
        }
    }
    return out.toString()
}

private fun Char.isHangul(): Boolean =
    this in '가'..'힣' || this in 'ᄀ'..'ᇿ' || this in '㄰'..'㆏'

/**
 * 앱 전체에서 쓰는 글자 부품. material3 Text와 같지만 한국어를 어절 단위로 줄바꿈한다.
 * 화면 파일은 material3 Text 대신 이것을 import 한다.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    val shown = remember(text) { text.keepKoreanWords() }
    androidx.compose.material3.Text(
        text = shown,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}
