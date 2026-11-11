package com.getfuckocus.nfc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.nfc.NfcAdapter
import android.os.Handler
import android.os.Looper
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

/** 디버그 빌드인지. 가짜 태그 버튼은 디버그 빌드에서만 보인다. */
fun Context.isDebuggable(): Boolean = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

/**
 * 이 화면이 앞에 나와 있는 동안에만 NFC 리더 모드를 켜서 태그 고유 ID를 읽는다.
 * 리더 모드가 켜져 있으면 다른 앱이나 시스템이 태그를 가로채지 않는다.
 * 태그 내용은 읽지 않고 ID만 [onTag]로 넘긴다 (메인 스레드에서 부른다).
 * 화면에 돌아올 때마다 NFC가 켜져 있는지 다시 확인해 돌려준다.
 */
@Composable
fun NfcTagScanner(enabled: Boolean, onTag: (uid: String) -> Unit): NfcAvailability {
    val context = LocalContext.current
    val currentOnTag by rememberUpdatedState(onTag)
    var availability by remember { mutableStateOf(nfcAvailability(context)) }

    LifecycleResumeEffect(enabled) {
        availability = nfcAvailability(context)
        val activity = context.findActivity()
        val adapter = NfcAdapter.getDefaultAdapter(context)
        val main = Handler(Looper.getMainLooper())
        val active = enabled && activity != null && adapter != null && availability == NfcAvailability.Ready
        if (active) {
            adapter!!.enableReaderMode(
                activity,
                { tag -> tag.id?.let { id -> main.post { currentOnTag(NfcTags.formatUid(id)) } } },
                READER_FLAGS,
                null,
            )
        }
        onPauseOrDispose {
            if (active) adapter!!.disableReaderMode(activity)
        }
    }
    return availability
}

/** 흔한 태그 방식은 모두 받는다. NDEF 내용을 확인하지 않아 빈 스티커나 교통카드도 바로 읽힌다 */
private const val READER_FLAGS =
    NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or
        NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
