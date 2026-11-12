package com.getfuckocus.lock

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.core.content.edit
import com.getfuckocus.data.MissAction
import com.getfuckocus.safety.DeviceClock
import com.getfuckocus.safety.SafetyPolicy
import com.getfuckocus.safety.Stamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class LockState(
    val taskId: Long,
    val taskTitle: String,
    val dayKey: String,
    val lockedAt: Stamp,
    val emergencyRequestedAt: Stamp?,
    /** [MissAction.LOCK]이면 허용 앱 말고는 막고, [MissAction.NAG]면 몇 분마다 억제 화면으로 덮기만 한다. */
    val mode: MissAction = MissAction.LOCK,
)

/** 억제 화면을 얼마나 자주 다시 띄울지. */
object NagPolicy {
    const val INTERVAL_MS = 5 * 60_000L

    /** 마지막으로 띄운 뒤 [INTERVAL_MS]가 지났으면(또는 아직 안 띄웠으면) 다시 띄울 때다. 둘 다 부팅 후 경과 시간. */
    fun due(lastShownElapsed: Long?, nowElapsed: Long): Boolean =
        lastShownElapsed == null || nowElapsed - lastShownElapsed !in 0 until INTERVAL_MS
}

/**
 * 잠금 상태 저장소. 경과 시간은 [Stamp.elapsedSince]로 재기 때문에
 * 같은 부팅 안에서는 기기 시계를 바꿔도 자동 해제나 비상 해제가 앞당겨지지 않는다.
 */
class LockController(context: Context, private val clock: DeviceClock) {
    private val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<LockState?> = _state.asStateFlow()

    /** 지금 잠겨 있으면 그 상태를 돌려준다. 최대 잠금 시간이 지났으면 자동 해제하고 null. */
    fun current(): LockState? {
        val s = _state.value ?: return null
        if (autoReleaseRemaining(s) <= 0) {
            unlock()
            return null
        }
        return s
    }

    fun autoReleaseRemaining(s: LockState): Long =
        SafetyPolicy.MAX_LOCK_DURATION_MS - s.lockedAt.elapsedSince(clock)

    /** 비상 해제까지 남은 시간. 아직 요청하지 않았으면 null. */
    fun emergencyRemaining(s: LockState): Long? =
        s.emergencyRequestedAt?.let { SafetyPolicy.EMERGENCY_DELAY_MS - it.elapsedSince(clock) }

    @Synchronized
    fun lock(taskId: Long, taskTitle: String, dayKey: String, mode: MissAction = MissAction.LOCK) {
        markHandled(taskId, dayKey)
        lastNagShownElapsed = null
        write(LockState(taskId, taskTitle, dayKey, clock.now(), emergencyRequestedAt = null, mode = mode))
    }

    /** 억제 화면을 마지막으로 띄운 때. 앱이 다시 시작되면 바로 한 번 더 띄워도 되므로 저장하지 않는다. */
    private var lastNagShownElapsed: Long? = null

    /** 억제 화면을 띄울 때면 띄운 것으로 기록하고 true. */
    @Synchronized
    fun claimNagSlot(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (!NagPolicy.due(lastNagShownElapsed, now)) return false
        lastNagShownElapsed = now
        return true
    }

    @Synchronized
    fun requestEmergencyUnlock() {
        val s = current() ?: return
        if (s.emergencyRequestedAt == null) write(s.copy(emergencyRequestedAt = clock.now()))
    }

    /** 비상 해제 대기 시간이 지났으면 해제하고 true. */
    @Synchronized
    fun completeEmergencyUnlock(): Boolean {
        val s = current() ?: return true
        val remaining = emergencyRemaining(s) ?: return false
        if (remaining > 0) return false
        unlock()
        return true
    }

    /** 잠금 원인이 된 과제를 늦게라도 인증하면 해제한다. */
    fun releaseIfVerified(taskId: Long) {
        if (_state.value?.taskId == taskId) unlock()
    }

    @Synchronized
    fun unlock() {
        prefs.edit {
            remove(KEY_TASK_ID)
            remove(KEY_TASK_TITLE)
            remove(KEY_DAY_KEY)
            remove(KEY_MODE)
            putStamp(PREFIX_LOCKED, null)
            putStamp(PREFIX_EMERGENCY, null)
        }
        _state.value = null
    }

    /** 같은 과제로는 하루에 한 번만 잠근다. 해제한 뒤 같은 실패로 다시 잠기지 않게 한다. */
    fun isHandled(taskId: Long, dayKey: String): Boolean =
        handledKey(taskId, dayKey) in prefs.getStringSet(KEY_HANDLED, null).orEmpty()

    private fun markHandled(taskId: Long, dayKey: String) {
        // 자정을 넘는 회차가 있으므로 이틀치만 남겨 목록이 계속 커지지 않게 한다
        val cutoff = LocalDate.parse(dayKey).minusDays(1)
        val kept = prefs.getStringSet(KEY_HANDLED, null).orEmpty()
            .filter { entry ->
                runCatching { !LocalDate.parse(entry.substringBefore('#')).isBefore(cutoff) }.getOrDefault(false)
            }
            .toSet()
        prefs.edit { putStringSet(KEY_HANDLED, kept + handledKey(taskId, dayKey)) }
    }

    private fun handledKey(taskId: Long, dayKey: String) = "$dayKey#$taskId"

    private fun read(): LockState? {
        if (!prefs.contains(KEY_TASK_ID)) return null
        val lockedAt = readStamp(PREFIX_LOCKED) ?: return null
        return LockState(
            taskId = prefs.getLong(KEY_TASK_ID, 0),
            taskTitle = prefs.getString(KEY_TASK_TITLE, null).orEmpty(),
            dayKey = prefs.getString(KEY_DAY_KEY, null).orEmpty(),
            lockedAt = lockedAt,
            emergencyRequestedAt = readStamp(PREFIX_EMERGENCY),
            // 모드가 없던 때 저장된 잠금은 잠금으로 본다
            mode = prefs.getString(KEY_MODE, null)
                ?.let { runCatching { MissAction.valueOf(it) }.getOrNull() }
                ?: MissAction.LOCK,
        )
    }

    private fun write(s: LockState) {
        prefs.edit {
            putLong(KEY_TASK_ID, s.taskId)
            putString(KEY_MODE, s.mode.name)
            putString(KEY_TASK_TITLE, s.taskTitle)
            putString(KEY_DAY_KEY, s.dayKey)
            putStamp(PREFIX_LOCKED, s.lockedAt)
            putStamp(PREFIX_EMERGENCY, s.emergencyRequestedAt)
        }
        _state.value = s
    }

    private fun readStamp(prefix: String): Stamp? {
        if (!prefs.contains("${prefix}_wall")) return null
        return Stamp(
            wallMillis = prefs.getLong("${prefix}_wall", 0),
            elapsedMillis = prefs.getLong("${prefix}_elapsed", 0),
            bootCount = prefs.getInt("${prefix}_boot", -1),
        )
    }

    private fun SharedPreferences.Editor.putStamp(prefix: String, stamp: Stamp?) {
        if (stamp == null) {
            remove("${prefix}_wall")
            remove("${prefix}_elapsed")
            remove("${prefix}_boot")
        } else {
            putLong("${prefix}_wall", stamp.wallMillis)
            putLong("${prefix}_elapsed", stamp.elapsedMillis)
            putInt("${prefix}_boot", stamp.bootCount)
        }
    }

    private companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_DAY_KEY = "day_key"
        const val KEY_MODE = "mode"
        const val KEY_HANDLED = "handled"
        const val PREFIX_LOCKED = "locked"
        const val PREFIX_EMERGENCY = "emergency"
    }
}
