package com.getfuckocus.ui

import com.getfuckocus.task.TaskSchedule
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** 오늘이면 "45분 뒤 시작", 내일이면 "내일 07:00 시작", 그 뒤면 "목요일 07:00 시작" */
fun formatStartsIn(now: ZonedDateTime, startsAt: ZonedDateTime): String {
    val time = TaskSchedule.formatMinute(startsAt.hour * 60 + startsAt.minute)
    return when (ChronoUnit.DAYS.between(now.toLocalDate(), startsAt.toLocalDate())) {
        0L -> "${formatMinutesLeft(Duration.between(now, startsAt).toMillis())} 뒤 시작"
        1L -> "내일 $time 시작"
        else -> "${TaskSchedule.dayName(startsAt.dayOfWeek)}요일 $time 시작"
    }
}

fun formatRemaining(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0) + 999) / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d시간 %02d분".format(hours, minutes) else "%d분 %02d초".format(minutes, seconds)
}

/** 카드용 대략적인 남은 시간: "1시간 5분", "32분", "1분 미만" */
fun formatMinutesLeft(ms: Long): String {
    val minutes = ms.coerceAtLeast(0) / 60_000
    return when {
        minutes == 0L -> "1분 미만"
        minutes < 60 -> "${minutes}분"
        minutes % 60 == 0L -> "${minutes / 60}시간"
        else -> "${minutes / 60}시간 ${minutes % 60}분"
    }
}
