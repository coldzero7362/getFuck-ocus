package com.getfuckocus.safety

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.edit
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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

/**
 * 시계를 뒤로 돌려도 줄어들지 않는 "믿을 수 있는 현재 시각".
 * 지난번에 확인한 시각([anchor])에서 실제로 흐른 시간만큼은 지났다고 본다.
 * - 같은 부팅: anchor + (부팅 후 경과 시간 차이)보다 이르게 나올 수 없다.
 * - 재부팅 후: 꺼져 있던 시간은 알 수 없으므로 anchor보다 이르게만 나오지 않게 한다.
 * 시계를 앞으로 돌리는 것은 그대로 받아들인다. 마감이 앞당겨질 뿐 빠져나가는 데 쓸 수 없다.
 * 단, [MAX_ROLLBACK_MS]보다 크게 뒤로 가면 시계를 잘못 맞췄다가 바로잡은 것으로 보고 그대로 따른다.
 * 그렇지 않으면 실수로 하루 앞당긴 시계를 되돌려도 하루 종일 앞선 시각으로 판정된다.
 */
fun trustedWallMillis(now: Stamp, anchor: Stamp?): Long {
    if (anchor == null) return now.wallMillis
    val floor = if (anchor.bootCount != -1 && anchor.bootCount == now.bootCount) {
        anchor.wallMillis + (now.elapsedMillis - anchor.elapsedMillis)
    } else {
        anchor.wallMillis
    }
    if (floor - now.wallMillis > MAX_ROLLBACK_MS) return now.wallMillis
    return maxOf(now.wallMillis, floor)
}

const val MAX_ROLLBACK_MS = 12L * 60 * 60 * 1000

class DeviceClock(private val context: Context) {
    private val prefs = context.getSharedPreferences("clock", Context.MODE_PRIVATE)

    fun now(): Stamp = Stamp(
        wallMillis = System.currentTimeMillis(),
        elapsedMillis = SystemClock.elapsedRealtime(),
        bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1),
    )

    /** 마감 판정과 인증 시각에 쓴다. 화면 표시용으로는 그냥 현재 시각을 써도 된다. */
    @Synchronized
    fun trustedNowMillis(): Long {
        val now = now()
        val trusted = trustedWallMillis(now, readAnchor())
        prefs.edit {
            putLong(KEY_WALL, trusted)
            putLong(KEY_ELAPSED, now.elapsedMillis)
            putInt(KEY_BOOT, now.bootCount)
        }
        return trusted
    }

    fun trustedNow(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
        Instant.ofEpochMilli(trustedNowMillis()).atZone(zone)

    private fun readAnchor(): Stamp? {
        if (!prefs.contains(KEY_WALL)) return null
        return Stamp(prefs.getLong(KEY_WALL, 0), prefs.getLong(KEY_ELAPSED, 0), prefs.getInt(KEY_BOOT, -1))
    }

    private companion object {
        const val KEY_WALL = "anchor_wall"
        const val KEY_ELAPSED = "anchor_elapsed"
        const val KEY_BOOT = "anchor_boot"
    }
}
