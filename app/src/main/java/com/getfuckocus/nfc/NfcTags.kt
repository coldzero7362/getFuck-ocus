package com.getfuckocus.nfc

import kotlin.random.Random

/** 태그 고유 ID를 다루는 순수 함수와, 등록할 때 같은 태그를 두 번 찍어 확인하는 단계. */
object NfcTags {
    /** [0x04, 0xA2] → "04:A2" */
    fun formatUid(bytes: ByteArray): String = bytes.joinToString(":") { "%02X".format(it) }

    /**
     * 디버그 빌드에서 실물 태그 없이 등록 흐름을 시험하려고 만드는 가짜 ID.
     * 흔한 NTAG 스티커처럼 7바이트, 첫 바이트는 제조사 코드 04로 둔다.
     */
    fun fakeUid(random: Random = Random.Default): String =
        formatUid(ByteArray(7).also { random.nextBytes(it); it[0] = 0x04 })

    /**
     * 태그를 찍을 때마다 다음 단계를 정한다.
     * 찍을 때마다 ID가 바뀌는 카드(일부 신용카드 등)는 체크에 쓸 수 없으므로 두 번 찍어 같은지 확인한다.
     * [registeredName]은 이 ID로 이미 등록된 태그 이름 (없으면 null).
     */
    fun onScan(state: ScanState, uid: String, registeredName: String?): ScanState = when {
        state is ScanState.Ready -> state
        registeredName != null -> ScanState.Waiting(notice = "이미 '$registeredName'(으)로 등록된 태그예요")
        state is ScanState.Confirming && state.uid == uid -> ScanState.Ready(uid)
        state is ScanState.Confirming -> ScanState.Waiting(
            notice = "처음 찍은 태그와 ID가 달라요. 같은 태그를 처음부터 다시 대 주세요. " +
                "계속 다르면 찍을 때마다 ID가 바뀌는 카드라 쓸 수 없어요",
        )
        else -> ScanState.Confirming(uid)
    }
}

sealed interface ScanState {
    /** 첫 번째로 찍기를 기다린다. [notice]는 바로 전에 실패한 이유 */
    data class Waiting(val notice: String? = null) : ScanState

    /** 한 번 찍었고, 같은 태그를 한 번 더 찍기를 기다린다 */
    data class Confirming(val uid: String) : ScanState

    /** 두 번 모두 같은 ID가 읽혔다. 이름을 붙여 저장하면 된다 */
    data class Ready(val uid: String) : ScanState
}
