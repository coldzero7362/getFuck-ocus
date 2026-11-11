package com.getfuckocus.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NfcTagsTest {
    private val a = "04:A2:3F:11:22:33:44"
    private val b = "04:99:88:77:66:55:44"

    @Test
    fun `태그 ID는 대문자 16진수를 콜론으로 잇는다`() {
        assertEquals("04:A2:0F:FF", NfcTags.formatUid(byteArrayOf(0x04, 0xA2.toByte(), 0x0F, 0xFF.toByte())))
    }

    @Test
    fun `가짜 태그는 04로 시작하는 7바이트이고 매번 다르다`() {
        val uid = NfcTags.fakeUid(Random(1))
        assertTrue(uid.matches(Regex("04(:[0-9A-F]{2}){6}")))
        assertNotEquals(uid, NfcTags.fakeUid(Random(2)))
    }

    @Test
    fun `같은 태그를 두 번 찍으면 저장할 수 있다`() {
        val first = NfcTags.onScan(ScanState.Waiting(), a, registeredName = null)
        assertEquals(ScanState.Confirming(a), first)
        assertEquals(ScanState.Ready(a), NfcTags.onScan(first, a, registeredName = null))
    }

    @Test
    fun `두 번째에 ID가 다르면 처음부터 다시 찍는다`() {
        val next = NfcTags.onScan(ScanState.Confirming(a), b, registeredName = null)
        assertTrue(next is ScanState.Waiting && next.notice!!.contains("ID가 달라요"))
    }

    @Test
    fun `이미 등록된 태그는 받지 않는다`() {
        val next = NfcTags.onScan(ScanState.Waiting(), a, registeredName = "책상")
        assertEquals(ScanState.Waiting(notice = "이미 '책상'(으)로 등록된 태그예요"), next)
    }

    @Test
    fun `확인이 끝난 뒤 찍힌 태그는 무시한다`() {
        assertEquals(ScanState.Ready(a), NfcTags.onScan(ScanState.Ready(a), b, registeredName = null))
    }
}
