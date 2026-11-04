package com.getfuckocus.ui

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
