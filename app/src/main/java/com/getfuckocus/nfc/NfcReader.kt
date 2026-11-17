package com.getfuckocus.nfc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

enum class NfcAvailability { Unsupported, Off, Ready }

fun nfcAvailability(context: Context): NfcAvailability {
    val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcAvailability.Unsupported
    return if (adapter.isEnabled) NfcAvailability.Ready else NfcAvailability.Off
}

/**
 * 이 화면이 앞에 나와 있는 동안에만 NFC 리더 모드를 켜서 태그 고유 ID를 읽는다.
 * 리더 모드가 켜져 있으면 다른 앱이나 시스템이 태그를 가로채지 않는다.
 * 태그 내용은 읽지 않고 ID만 [onTag]로 넘긴다 (메인 스레드에서 부른다).
 * 한 번 댄 태그가 여러 번 읽혀도 [TagDebouncer]로 걸러 한 번만 넘긴다.
 * 화면에 돌아올 때마다 NFC가 켜져 있는지 다시 확인해 돌려준다.
 */
@Composable
fun NfcTagScanner(enabled: Boolean, onTag: (uid: String) -> Unit): NfcAvailability {
    val context = LocalContext.current
    val currentOnTag by rememberUpdatedState(onTag)
    var availability by remember { mutableStateOf(nfcAvailability(context)) }
    // 화면이 다시 앞에 와도 이어서 쓴다 (켜고 끄는 사이에 같은 터치가 두 번 들어오지 않게)
    val debouncer = remember { TagDebouncer() }

    LifecycleResumeEffect(enabled) {
        availability = nfcAvailability(context)
        val activity = context.findActivity()
        val adapter = NfcAdapter.getDefaultAdapter(context)
        val main = Handler(Looper.getMainLooper())
        // 켤 수 있을 때만 (어댑터, 화면) 짝을 잡아 두고, 같은 짝으로 끈다
        val reader = if (enabled && availability == NfcAvailability.Ready && activity != null && adapter != null) {
            adapter to activity
        } else {
            null
        }
        reader?.let { (nfc, screen) ->
            nfc.enableReaderMode(
                screen,
                { tag ->
                    tag.id?.let { id ->
                        val uid = NfcTags.formatUid(id)
                        main.post {
                            // 같은 터치의 반복 읽기는 버린다 (메인 스레드에서만 다뤄 순서가 섞이지 않는다)
                            if (debouncer.accept(uid, SystemClock.elapsedRealtime())) currentOnTag(uid)
                        }
                    }
                },
                READER_FLAGS,
                READER_EXTRAS,
            )
        }
        onPauseOrDispose {
            reader?.let { (nfc, screen) -> nfc.disableReaderMode(screen) }
        }
    }
    return availability
}

/** 흔한 태그 방식은 모두 받는다. NDEF 내용을 확인하지 않아 빈 스티커나 교통카드도 바로 읽힌다 */
private const val READER_FLAGS =
    NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or
        NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

/** 태그가 아직 있는지 확인하는 간격을 늘려, 살짝 흔들릴 때 놓쳤다 다시 찾는 일을 줄인다 */
private val READER_EXTRAS = Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_DELAY_MS) }
private const val PRESENCE_CHECK_DELAY_MS = 500

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
