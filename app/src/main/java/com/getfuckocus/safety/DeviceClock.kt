package com.getfuckocus.safety

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/** 어느 순간의 벽시계 시각 + 부팅 후 경과 시간 + 부팅 횟수. */
data class Stamp(
    val wallMillis: Long,
    val elapsedMillis: Long,
    val bootCount: Int,
) {
    /**
     * 이 시점부터 지난 시간. 같은 부팅이면 elapsedRealtime을 쓰기 때문에
     * 기기 시계를 앞으로 돌려도 늘어나지 않는다. 재부팅 뒤에는 벽시계로 대신한다.
     */
    fun elapsedSince(clock: DeviceClock): Long {
        val now = clock.now()
        return if (bootCount != -1 && now.bootCount == bootCount) {
            now.elapsedMillis - elapsedMillis
        } else {
            now.wallMillis - wallMillis
        }
    }
}

class DeviceClock(private val context: Context) {
    fun now(): Stamp = Stamp(
        wallMillis = System.currentTimeMillis(),
        elapsedMillis = SystemClock.elapsedRealtime(),
        bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1),
    )
}
