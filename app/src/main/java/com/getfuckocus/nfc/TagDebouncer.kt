package com.getfuckocus.nfc

/**
 * 한 번 댄 태그가 여러 번 읽히는 것을 걸러낸다.
 * 태그를 폰에 대고 있거나 살짝 움직이면 폰은 태그를 놓쳤다가 다시 찾으면서 같은 ID를 몇 번이고 넘겨준다.
 * 같은 ID가 마지막으로 보인 뒤 [windowMs]가 지나기 전에 다시 오면 같은 터치로 보고 버린다.
 * 마지막으로 **보인** 시각을 계속 늦추므로, 대고 있는 동안에는 몇 번이 와도 한 번만 넘긴다.
 * 다른 태그는 바로 받는다.
 */
class TagDebouncer(private val windowMs: Long = DEFAULT_WINDOW_MS) {
    private var lastUid: String? = null
    private var lastSeenAt = Long.MIN_VALUE

    /** 이번 읽기를 새 터치로 받을지 */
    fun accept(uid: String, nowMs: Long): Boolean {
        val repeat = uid == lastUid && nowMs - lastSeenAt < windowMs
        lastUid = uid
        lastSeenAt = nowMs
        return !repeat
    }

    companion object {
        /** 떼었다가 다시 대는 데 보통 걸리는 시간보다 짧게. 등록할 때 같은 태그를 두 번 대는 흐름을 막지 않는다 */
        const val DEFAULT_WINDOW_MS = 1_500L
    }
}
