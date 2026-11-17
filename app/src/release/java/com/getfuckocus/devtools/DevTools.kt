package com.getfuckocus.devtools

import androidx.compose.runtime.Composable

/*
 * 배포 빌드용 빈 구현 (src/release).
 * 디버그 빌드의 시험 도구(src/debug)와 이름·모양만 같고 아무것도 하지 않는다.
 * 그래서 가짜 태그를 만드는 코드와 시험용 문구가 배포 APK에 들어가지 않는다.
 */

object DevTools {
    /** 배포 빌드에서는 늘 꺼짐. 이 값으로 감싼 코드는 컴파일 때 빠진다 */
    const val ENABLED = false
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun CheckInTestButtons(chosenUids: List<String>, onScan: (String?) -> Unit) = Unit

@Suppress("UNUSED_PARAMETER")
@Composable
fun TagAddTestButton(confirmingUid: String?, onScan: (String) -> Unit) = Unit
