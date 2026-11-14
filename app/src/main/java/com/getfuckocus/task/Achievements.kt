package com.getfuckocus.task

import com.getfuckocus.data.AchievementDao
import com.getfuckocus.data.AchievementEntity
import com.getfuckocus.data.TaskDao
import com.getfuckocus.data.TaskEntity
import com.getfuckocus.safety.DeviceClock

/** 업적 아이콘에 덧붙이는 장식. 뒤로 갈수록 화려하다. */
enum class AchievementDecor { NONE, STAR, TWO_STARS, AURA, AURA_STAR, CROWN }

/** 연속 달성 단계. 과제마다 단계별로 한 번씩 받는다. */
enum class AchievementTier(val milestone: Int, val title: String, val decor: AchievementDecor) {
    THREE(3, "작심삼일 돌파", AchievementDecor.NONE),
    WEEK(7, "일주일 버팀", AchievementDecor.STAR),
    TWO_WEEKS(14, "습관 냄새가 난다", AchievementDecor.TWO_STARS),
    MONTH(30, "한 달 생존", AchievementDecor.AURA),
    FIFTY(50, "반백 번 출석", AchievementDecor.AURA_STAR),
    HUNDRED(100, "사람 됐다", AchievementDecor.CROWN),
    ;

    companion object {
        fun of(milestone: Int): AchievementTier? = entries.firstOrNull { it.milestone == milestone }

        /** 받은 단계 중 가장 높은 것 */
        fun best(owned: Collection<Int>): AchievementTier? = entries.lastOrNull { it.milestone in owned }

        /** 연속 [streak]회에서 다음에 받을 수 있는 단계. 다 받았으면 null */
        fun next(streak: Int, owned: Collection<Int>): AchievementTier? =
            entries.firstOrNull { it.milestone > streak && it.milestone !in owned }

        /**
         * 연속 [streak]회에서 새로 받을 단계들. 이미 받은 단계([owned])는 빼고, 넘어선 단계는 모두 준다
         * (업적 기능이 생기기 전에 이미 쌓아 둔 연속 기록도 인정한다).
         */
        fun newlyReached(streak: Int, owned: Collection<Int>): List<AchievementTier> =
            entries.filter { it.milestone <= streak && it.milestone !in owned }
    }
}

/** 인증을 통과할 때마다 연속 달성 수를 다시 세서 새 업적을 기록한다. 보여 주는 건 화면 쪽이 [AchievementDao.observeUnseen]으로 한다. */
class AchievementRecorder(
    private val tasks: TaskDao,
    private val achievements: AchievementDao,
    private val clock: DeviceClock,
) {
    suspend fun onPassed(task: TaskEntity) {
        val now = clock.trustedNow()
        val streak = History.streak(task, now, PassIndex(tasks.passRecordsOf(task.id)))
        val reached = AchievementTier.newlyReached(streak, achievements.milestonesOf(task.id))
        val at = now.toInstant().toEpochMilli()
        reached.forEach { achievements.insert(AchievementEntity(task.id, it.milestone, achievedAt = at)) }
    }
}
