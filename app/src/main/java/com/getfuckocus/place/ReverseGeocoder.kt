package com.getfuckocus.place

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** 장소 선택 화면에 보여 줄 주소. [title]이 큰 글자, [detail]은 그 아래 보조 줄 (없을 수 있다). */
data class PlaceAddress(val title: String, val detail: String?)

/**
 * 좌표를 주소로 바꾼다 (OpenStreetMap Nominatim).
 * - 장소 선택 화면에서 지도가 멈춘 위치의 좌표만 보낸다. 과제나 기록 같은 다른 데이터는 보내지 않는다.
 * - Nominatim 이용 조건대로 요청 사이를 1초 이상 띄우고, 앱을 알아볼 수 있는 User-Agent를 보내며, 결과를 캐시한다.
 */
class ReverseGeocoder(context: Context) {
    private val userAgent = "${context.packageName}/${appVersion(context)} (Android)"
    private val mutex = Mutex()
    private var lastRequestAt = 0L
    private val cache = object : LinkedHashMap<String, PlaceAddress>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PlaceAddress>?) = size > CACHE_SIZE
    }

    /** 주소를 찾지 못했거나 네트워크가 안 되면 null. */
    suspend fun lookup(lat: Double, lng: Double): PlaceAddress? = mutex.withLock {
        // 소수 다섯째 자리(약 1m)까지 같으면 같은 곳으로 본다
        val key = "%.5f,%.5f".format(Locale.US, lat, lng)
        cache[key]?.let { return@withLock it }
        val wait = MIN_INTERVAL_MS - (SystemClock.elapsedRealtime() - lastRequestAt)
        if (wait > 0) delay(wait)
        lastRequestAt = SystemClock.elapsedRealtime()
        val result = withContext(Dispatchers.IO) { runCatching { request(lat, lng) }.getOrNull() }
        result?.also { cache[key] = it }
    }

    private fun request(lat: Double, lng: Double): PlaceAddress? {
        val url = URL(
            "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=18&accept-language=ko" +
                "&lat=${"%.6f".format(Locale.US, lat)}&lon=${"%.6f".format(Locale.US, lng)}"
        )
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", userAgent)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (json.has("error")) return null
            return AddressFormat.from(name = json.optString("name"), displayName = json.optString("display_name"))
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val MIN_INTERVAL_MS = 1_100L
        const val TIMEOUT_MS = 8_000
        const val CACHE_SIZE = 50

        fun appVersion(context: Context): String = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0"
    }
}

/** Nominatim 결과를 화면에 맞게 다듬는다. */
object AddressFormat {
    /**
     * display_name은 "건물, 번지, 도로, 동, 구, 시, 우편번호, 대한민국"처럼 작은 단위부터 쉼표로 이어진다.
     * 우편번호와 나라를 빼고 큰 단위부터 읽히게 뒤집는다.
     * 장소 이름이 있으면 그것을 큰 글자로, 주소를 보조 줄로 쓴다.
     */
    fun from(name: String?, displayName: String?): PlaceAddress? {
        val parts = displayName.orEmpty().split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.matches(POSTCODE) && it !in COUNTRIES }
        val placeName = name?.trim()?.takeIf { it.isNotEmpty() }
        val address = parts.filter { it != placeName }.reversed().joinToString(" ").takeIf { it.isNotEmpty() }
        return when {
            placeName != null -> PlaceAddress(placeName, address)
            address != null -> PlaceAddress(address, null)
            else -> null
        }
    }

    private val POSTCODE = Regex("\\d{3}-?\\d{2,3}")
    private val COUNTRIES = setOf("대한민국", "South Korea", "Korea")
}
