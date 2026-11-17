package com.getfuckocus.devtools

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 디버그 빌드 전용 시험 도구 테스트 (src/testDebug: 디버그 빌드의 단위 테스트에서만 돈다) */
class DevToolsTest {

    @Test
    fun `가짜 태그는 04로 시작하는 7바이트이고 매번 다르다`() {
        val uid = DevTools.fakeUid(Random(1))
        assertTrue(uid.matches(Regex("04(:[0-9A-F]{2}){6}")))
        assertNotEquals(uid, DevTools.fakeUid(Random(2)))
    }
}
