package com.getfuckocus

import android.app.Application
import android.content.Context
import com.getfuckocus.lock.NotificationHelper
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
        NotificationHelper.createChannel(this)
        appScope.launch { container.photoRetention.purgeExpired() }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as GetFocusApp).container
