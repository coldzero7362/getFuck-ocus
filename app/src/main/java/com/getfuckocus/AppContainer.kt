package com.getfuckocus

import android.content.Context
import com.getfuckocus.data.AppDatabase
import com.getfuckocus.lock.LockController
import com.getfuckocus.place.ReverseGeocoder
import com.getfuckocus.safety.DeviceClock
import com.getfuckocus.safety.PhotoRetention
import com.getfuckocus.safety.Whitelist
import com.getfuckocus.task.DeadlineEvaluator
import com.getfuckocus.task.DeadlineScheduler
import com.getfuckocus.verify.EvidenceCollector
import com.getfuckocus.verify.PhotoVerifier
import com.getfuckocus.verify.VerificationFlow

/** 앱 전역 의존성. 규모가 커지면 Hilt 등으로 교체한다. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val deviceClock = DeviceClock(appContext)
    private val database = AppDatabase.build(appContext)
    val taskDao = database.taskDao()
    val nfcTagDao = database.nfcTagDao()
    val lockController = LockController(appContext, deviceClock)
    val whitelist = Whitelist(appContext)
    val deadlineEvaluator = DeadlineEvaluator(taskDao, lockController, deviceClock)
    val deadlineScheduler = DeadlineScheduler(appContext)
    val evidenceCollector = EvidenceCollector(appContext, deviceClock)
    val verificationFlow = VerificationFlow(taskDao, nfcTagDao, evidenceCollector, PhotoVerifier(), lockController, deviceClock)
    val photoRetention = PhotoRetention(taskDao)
    /** 요청 간격 제한과 캐시를 앱 전체에서 함께 쓰도록 하나만 둔다 */
    val reverseGeocoder = ReverseGeocoder(appContext)
}
