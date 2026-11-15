package com.getfuckocus

import android.app.Application
import android.content.Context
import com.getfuckocus.lock.NotificationHelper
import com.getfuckocus.ui.theme.SkinState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GetFocusApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 메인 화면·잠금 화면·억제 오버레이가 모두 같은 스킨을 쓰도록 맨 먼저 읽는다
        SkinState.load(this)
        NotificationHelper.createChannel(this)
        appScope.launch { container.photoRetention.purgeExpired() }
        // 과제가 추가·수정·삭제되거나 "바로 시작"할 때마다 다음 마감 알람을 다시 건다
        appScope.launch { container.taskDao.observeAll().collect { container.deadlineScheduler.schedule(it) } }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as GetFocusApp).container
