package com.getfuckocus.lock

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.getfuckocus.ui.DanmakuLayer
import com.getfuckocus.ui.theme.GetFocusTheme

/**
 * 억제 화면 모드: 쓰던 앱 위로 설정한 문구·사진이 1분마다 흘러가는 투명한 층.
 * 화면을 막지 않는다 — 터치를 받지 않아(FLAG_NOT_TOUCHABLE) 아래 앱을 그대로 쓸 수 있고,
 * 포커스도 가져가지 않아 키보드 입력도 막지 않는다.
 * 접근성 서비스의 오버레이 창이라 "다른 앱 위에 표시" 권한을 따로 받지 않는다.
 */
class NagOverlay(private val service: AccessibilityService) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var view: ComposeView? = null
    private var owner: OverlayLifecycleOwner? = null
    private var texts by mutableStateOf(emptyList<String>())
    private var pictures by mutableStateOf(emptyList<String>())

    val isShowing: Boolean get() = view != null

    /** 처음이면 창을 붙이고, 이미 있으면 흘려보낼 내용만 바꾼다 */
    fun show(texts: List<String>, pictures: List<String>) {
        this.texts = texts
        this.pictures = pictures
        if (view != null) return
        val lifecycleOwner = OverlayLifecycleOwner().also { it.start() }
        val compose = ComposeView(service).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                GetFocusTheme {
                    DanmakuLayer(this@NagOverlay.texts, this@NagOverlay.pictures, Modifier.fillMaxSize())
                }
            }
        }
        runCatching { windowManager.addView(compose, layoutParams()) }
            .onSuccess {
                view = compose
                owner = lifecycleOwner
            }
            .onFailure { lifecycleOwner.stop() }
    }

    /** 전화·긴급전화·설정 화면 위에서는 잠깐 숨긴다 */
    fun setHidden(hidden: Boolean) {
        view?.visibility = if (hidden) View.GONE else View.VISIBLE
    }

    fun dismiss() {
        view?.let { runCatching { windowManager.removeView(it) } }
        owner?.stop()
        view = null
        owner = null
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    )
}

/** 서비스에 붙인 Compose 창이 쓸 생명주기 (액티비티가 없어서 직접 둔다) */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    fun start() {
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
