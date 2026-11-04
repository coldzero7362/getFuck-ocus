package com.getfuckocus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

/** 화면에 띄워 둔 동안 [intervalMillis]마다 갱신되는 현재 시각. 초 경계에 맞춰 갱신한다. */
@Composable
fun rememberNow(intervalMillis: Long = 1_000): ZonedDateTime {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(intervalMillis) {
        while (true) {
            now = ZonedDateTime.now()
            delay(intervalMillis - System.currentTimeMillis() % intervalMillis)
        }
    }
    return now
}
