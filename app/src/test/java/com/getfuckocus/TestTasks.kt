package com.getfuckocus

import com.getfuckocus.data.RepeatDays
import com.getfuckocus.data.TaskEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
val DAY: LocalDate = LocalDate.of(2026, 11, 5)

fun at(hour: Int, minute: Int = 0, date: LocalDate = DAY): ZonedDateTime =
    ZonedDateTime.of(date, LocalTime.of(hour, minute), ZONE)

/** 기본: 매일 07:00부터 60분, 오래전에 만든 과제 */
fun task(
    id: Long = 1,
    startHour: Int = 7,
    startMinute: Int = 0,
    durationMinutes: Int = 60,
    requiredLabel: String? = null,
    enabled: Boolean = true,
    createdAt: Long = at(0, date = DAY.minusDays(30)).toInstant().toEpochMilli(),
    startedAt: Long? = null,
    repeatDays: Int = RepeatDays.EVERY_DAY,
) = TaskEntity(
    id = id,
    title = "과제$id",
    windowStartMinute = startHour * 60 + startMinute,
    durationMinutes = durationMinutes,
    requiredLabel = requiredLabel,
    enabled = enabled,
    createdAt = createdAt,
    startedAt = startedAt,
    repeatDays = repeatDays,
)
