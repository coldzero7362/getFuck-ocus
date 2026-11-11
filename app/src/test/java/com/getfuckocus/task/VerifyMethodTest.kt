package com.getfuckocus.task

import com.getfuckocus.DAY
import com.getfuckocus.at
import com.getfuckocus.data.RepeatDays
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.data.locksOnMiss
import com.getfuckocus.task
import com.getfuckocus.verify.ConditionChecks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 기본 과제: 매일 07:00~08:00. DAY(2026-11-05)는 목요일. */
class VerifyMethodTest {
    private val tapTask = task().copy(verifyMethod = VerifyMethod.TAP)

    @Test
    fun `사진 인증 과제만 놓쳤을 때 잠근다`() {
        assertTrue(task().locksOnMiss())
        assertFalse(tapTask.locksOnMiss())
    }

    @Test
    fun `시간창 안의 탭 체크는 그 회차에 제때 기록된다`() {
        assertEquals(CheckInTarget(DAY, late = false), TaskSchedule.checkInOccurrence(tapTask, at(7, 30)))
    }

    @Test
    fun `마감 뒤 탭 체크는 그날 회차에 늦은 체크로 기록된다`() {
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
