package com.getfuckocus.ui

import com.getfuckocus.ui.components.keepKoreanWords
import org.junit.Assert.assertEquals
import org.junit.Test

class KoreanTextTest {
    private fun visible(s: String) = s.replace("⁠", "|")

    @Test
    fun `한글 어절 안에만 줄바꿈 금지 문자를 넣는다`() {
        assertEquals("지|나|가|요 문|구", visible("지나가요 문구".keepKoreanWords()))
    }

    @Test
    fun `영문과 숫자만 있는 부분은 그대로 둔다`() {
        assertEquals("TIP. 14:00 ok", visible("TIP. 14:00 ok".keepKoreanWords()))
    }

    @Test
    fun `한글과 붙은 영문·숫자·기호는 함께 묶는다`() {
        assertEquals("1|분|마|다 NFC|가", visible("1분마다 NFC가".keepKoreanWords()))
    }

    @Test
    fun `빈 문자열과 한 글자는 그대로`() {
        assertEquals("", "".keepKoreanWords())
        assertEquals("가", "가".keepKoreanWords())
    }
}
