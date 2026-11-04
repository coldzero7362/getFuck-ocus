package com.getfuckocus.safety

/**
 * 안전 상한. 사용자 설정으로도 이 값을 넘길 수 없다.
 * - 잠금은 최대 [MAX_LOCK_DURATION_MS] 뒤 자동으로 풀린다.
 * - 비상 해제는 요청하고 [EMERGENCY_DELAY_MS]를 기다리면 언제든 할 수 있다.
 * - 인증 사진은 [PHOTO_RETENTION_DAYS]가 지나면 삭제된다.
 * - 긴급전화, 전화, 문자, 알람, 설정, 런처는 잠금 중에도 막지 않는다 ([Whitelist]).
 */
object SafetyPolicy {
    const val MAX_LOCK_DURATION_MS = 3L * 60 * 60 * 1000
    const val EMERGENCY_DELAY_MS = 15L * 60 * 1000
    const val PHOTO_RETENTION_DAYS = 7L
}
