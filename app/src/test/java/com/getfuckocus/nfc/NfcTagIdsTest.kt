package com.getfuckocus.nfc

import com.getfuckocus.data.NfcTagIds
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.data.nfcTagIdList
import com.getfuckocus.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcTagIdsTest {

    @Test
    fun `쉼표 목록을 읽고 다시 쓴다`() {
        assertEquals(listOf(3L, 7L), NfcTagIds.parse("3,7"))
        assertEquals("3,7", NfcTagIds.join(listOf(3L, 7L)))
    }

    @Test
    fun `빈칸·잘못된 값·중복은 버린다`() {
        assertEquals(emptyList<Long>(), NfcTagIds.parse(""))
        assertEquals(emptyList<Long>(), NfcTagIds.parse(null))
        assertEquals(listOf(3L, 7L), NfcTagIds.parse(" 3 , x, 7,3, "))
        assertEquals("3,7", NfcTagIds.join(listOf(3L, 7L, 3L)))
    }

    @Test
    fun `과제에서 태그 목록을 꺼낸다`() {
        val t = task().copy(verifyMethod = VerifyMethod.NFC, nfcTagIds = "2,9")
        assertEquals(listOf(2L, 9L), t.nfcTagIdList())
    }

    @Test
    fun `태그를 지울 때 그 태그만 쓰는 과제가 있으면 막는다`() {
        val only = task(id = 1).copy(verifyMethod = VerifyMethod.NFC, nfcTagIds = "4")
        val plan = NfcTagIds.planRemoval(listOf(only), 4)
        assertEquals(listOf(only.title), plan.blockedBy)
        assertTrue(plan.updated.isEmpty())
    }

    @Test
    fun `다른 태그도 쓰는 과제는 그 태그만 빼고 남긴다`() {
        val shared = task(id = 1).copy(verifyMethod = VerifyMethod.NFC, nfcTagIds = "4,6")
        val other = task(id = 2).copy(verifyMethod = VerifyMethod.NFC, nfcTagIds = "8")
        val plan = NfcTagIds.planRemoval(listOf(shared, other), 4)
        assertTrue(plan.blockedBy.isEmpty())
        assertEquals(listOf("6"), plan.updated.map { it.nfcTagIds })
        assertEquals(listOf(shared.id), plan.updated.map { it.id })
    }
}
