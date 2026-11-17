package com.getfuckocus.devtools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.getfuckocus.nfc.NfcTags
import com.getfuckocus.ui.components.FocusButton
import com.getfuckocus.ui.components.FocusButtonStyle
import com.getfuckocus.ui.components.Text
import com.getfuckocus.ui.theme.Chrome
import kotlin.random.Random

/*
 * 디버그 빌드 전용 시험 도구 (src/debug).
 * 배포 빌드에는 같은 이름의 빈 구현(src/release)이 들어가서, 이 파일의 코드와 문구는 배포 APK에 아예 없다.
 * 실물 NFC 태그나 NFC가 없는 에뮬레이터에서 태그 흐름을 시험하는 데 쓴다.
 */

object DevTools {
    /** 시험 도구를 켤지. 배포 빌드에서는 false라 이 값으로 감싼 코드는 컴파일 때 빠진다 */
    const val ENABLED = true

    /** 실물 태그 없이 쓰는 가짜 ID. 흔한 NTAG 스티커처럼 7바이트, 첫 바이트는 제조사 코드 04 */
    fun fakeUid(random: Random = Random.Default): String =
        NfcTags.formatUid(ByteArray(7).also { random.nextBytes(it); it[0] = 0x04 })
}

/**
 * 체크 화면 아래 시험 버튼: 이 과제에 고른 태그 중 하나를 찍은 것처럼, 또는 다른 태그를 찍은 것처럼.
 * @param chosenUids 이 과제에 고른 태그 ID들
 */
@Composable
fun CheckInTestButtons(chosenUids: List<String>, onScan: (String?) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FocusButton(
                // 여러 개면 아무거나 하나를 찍은 것처럼 (어느 것이든 통과하는지 시험)
                "고른 태그 찍기", onClick = { onScan(chosenUids.randomOrNull()) },
                modifier = Modifier.weight(1f), style = FocusButtonStyle.Outline, height = 44.dp,
            )
            FocusButton(
                "다른 태그 찍기", onClick = { onScan(DevTools.fakeUid()) },
                modifier = Modifier.weight(1f), style = FocusButtonStyle.Outline, height = 44.dp,
            )
        }
        Text(
            "디버그 빌드에서만 보이는 시험용 버튼이에요",
            style = MaterialTheme.typography.bodySmall,
            color = Chrome.chromeIndigo,
        )
    }
}

/**
 * 태그 등록 화면의 시험 버튼. 두 번째로 누르면 첫 번째와 같은 ID를 찍은 것처럼 해서 등록을 마칠 수 있다.
 * NFC가 없는 에뮬레이터에서 쓰는 도구라 NFC 미지원이어도 보인다.
 * @param confirmingUid 한 번 찍고 확인을 기다리는 ID (없으면 null)
 */
@Composable
fun TagAddTestButton(confirmingUid: String?, onScan: (String) -> Unit) {
    FocusButton(
        "가짜 태그 찍기 (디버그 빌드 전용)",
        onClick = { onScan(confirmingUid ?: DevTools.fakeUid()) },
        modifier = Modifier.fillMaxWidth(),
        style = FocusButtonStyle.Outline,
    )
}
