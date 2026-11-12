package com.getfuckocus.task

import com.getfuckocus.DAY
import com.getfuckocus.at
import com.getfuckocus.data.MissAction
import com.getfuckocus.data.NfcTagEntity
import com.getfuckocus.data.RepeatDays
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.data.allowedMissActions
import com.getfuckocus.data.enforcesOnMiss
import com.getfuckocus.data.saveProblem
import com.getfuckocus.lock.NagPolicy
import com.getfuckocus.task
import com.getfuckocus.verify.ConditionChecks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 기본 과제: 매일 07:00~08:00. DAY(2026-11-05)는 목요일. */
class VerifyMethodTest {
    private val tapTask = task().copy(verifyMethod = VerifyMethod.TAP, missAction = MissAction.NOTIFY)

    @Test
    fun `알림만인 과제는 놓쳐도 덮거나 잠그지 않는다`() {
        assertFalse(tapTask.enforcesOnMiss())
        assertTrue(task().copy(missAction = MissAction.NAG).enforcesOnMiss())
        assertTrue(task().copy(missAction = MissAction.LOCK).enforcesOnMiss())
    }

    @Test
    fun `탭 체크는 앱 잠금을 고를 수 없고 나머지는 모두 고를 수 있다`() {
        assertEquals(listOf(MissAction.NOTIFY, MissAction.NAG), VerifyMethod.TAP.allowedMissActions())
        listOf(VerifyMethod.PHOTO, VerifyMethod.LOCATION, VerifyMethod.NFC).forEach {
            assertEquals(MissAction.entries, it.allowedMissActions())
        }
    }

    @Test
    fun `저장 전 확인 - 위치 인증은 장소, NFC 인증은 태그가 있어야 한다`() {
        assertNotNull(saveProblem(VerifyMethod.LOCATION, hasGeoFence = false, nfcTagId = null, MissAction.LOCK))
        assertNull(saveProblem(VerifyMethod.LOCATION, hasGeoFence = true, nfcTagId = null, MissAction.LOCK))
        assertNotNull(saveProblem(VerifyMethod.NFC, hasGeoFence = false, nfcTagId = null, MissAction.NAG))
        assertNull(saveProblem(VerifyMethod.NFC, hasGeoFence = false, nfcTagId = 3, MissAction.NAG))
        assertNotNull(saveProblem(VerifyMethod.TAP, hasGeoFence = false, nfcTagId = null, MissAction.LOCK))
        assertNull(saveProblem(VerifyMethod.PHOTO, hasGeoFence = false, nfcTagId = null, MissAction.NOTIFY))
    }

    @Test
    fun `억제 화면은 5분에 한 번만 다시 띄운다`() {
        assertTrue(NagPolicy.due(lastShownElapsed = null, nowElapsed = 1_000))
        assertFalse(NagPolicy.due(lastShownElapsed = 1_000, nowElapsed = 1_000 + NagPolicy.INTERVAL_MS - 1))
        assertTrue(NagPolicy.due(lastShownElapsed = 1_000, nowElapsed = 1_000 + NagPolicy.INTERVAL_MS))
        // 재부팅으로 경과 시간이 처음부터 다시 세어지면 바로 띄운다
        assertTrue(NagPolicy.due(lastShownElapsed = 50_000, nowElapsed = 1_000))
    }

    @Test
    fun `NFC 판정 - 고른 태그만 통과한다`() {
        val desk = NfcTagEntity(id = 3, uid = "04:AA:BB", name = "책상")
        assertNull(ConditionChecks.tagReason(desk, "04:AA:BB"))
        assertTrue(ConditionChecks.tagReason(desk, "04:CC:DD")!!.contains("'책상' 태그가 아니에요"))
        assertNotNull(ConditionChecks.tagReason(desk, null))
        assertNotNull(ConditionChecks.tagReason(null, "04:AA:BB"))
    }

    @Test
    fun `시간창 안의 체크는 그 회차에 제때 기록된다`() {
        assertEquals(CheckInTarget(DAY, late = false), TaskSchedule.checkInOccurrence(tapTask, at(7, 30)))
    }

    @Test
    fun `마감 뒤 체크는 그날 회차에 늦은 체크로 기록된다`() {
        assertEquals(CheckInTarget(DAY, late = true), TaskSchedule.checkInOccurrence(tapTask, at(12)))
    }

    @Test
    fun `오늘 회차 시작 전에는 어제 회차에 늦은 체크로 기록된다`() {
        assertEquals(CheckInTarget(DAY.minusDays(1), late = true), TaskSchedule.checkInOccurrence(tapTask, at(6)))
    }

    @Test
    fun `반복 요일이 아니고 어제도 회차가 없으면 기록할 회차가 없다`() {
        // 토요일(DAY+2) 오전, 평일만 반복. 금요일 회차는 지났으니 늦은 체크, 일요일(DAY+3)엔 어제(토)도 없음
        val weekdays = tapTask.copy(repeatDays = RepeatDays.WEEKDAYS)
        assertEquals(CheckInTarget(DAY.plusDays(1), late = true), TaskSchedule.checkInOccurrence(weekdays, at(10, date = DAY.plusDays(2))))
        assertNull(TaskSchedule.checkInOccurrence(weekdays, at(10, date = DAY.plusDays(3))))
    }

    @Test
    fun `과제를 만들기 전 회차로는 기록하지 않는다`() {
        val fresh = tapTask.copy(createdAt = at(9).toInstant().toEpochMilli())
        assertNull(TaskSchedule.checkInOccurrence(fresh, at(10)))
    }

    @Test
    fun `시간창 판정 문구`() {
        assertNull(ConditionChecks.windowReason(tapTask, inWindow = true))
        assertTrue(ConditionChecks.windowReason(tapTask, inWindow = false)!!.contains("인증 가능 시간"))
    }

    @Test
    fun `장소 조건이 없으면 장소 판정을 하지 않는다`() {
        assertTrue(ConditionChecks.placeReasons(tapTask, location = null, atMillis = 0).isEmpty())
    }
}
