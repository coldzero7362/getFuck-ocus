package com.getfuckocus.verify

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ExifReader {
    private val EXIF_DATE_TIME = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

    fun read(file: File): ExifSummary {
        val exif = ExifInterface(file)
        val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { raw ->
            runCatching {
                LocalDateTime.parse(raw, EXIF_DATE_TIME)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
        }
        val latLong = exif.latLong
        return ExifSummary(dateTime, latLong?.get(0), latLong?.get(1))
    }
}
