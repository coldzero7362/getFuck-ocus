package com.getfuckocus.verify

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.time.ZoneId

/**
 * 사진 선택기로 고른 캡처를 앱 저장소로 복사하고, 캡처 시각·글자·지문을 모은다. 모든 처리는 기기 안에서 끝난다.
 * 복사본은 인증 사진과 같은 폴더에 두어 기록 화면에 보이고 보존 기간이 지나면 함께 지워진다.
 */
class ScreenshotReader(private val context: Context) {

    // 온디바이스 한국어 모델 (영문·숫자도 함께 읽는다). 이미지가 기기 밖으로 나가지 않는다
    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    suspend fun read(uri: Uri): ScreenshotEvidence {
        val (file, hash) = withContext(Dispatchers.IO) { copyWithHash(uri) }
        try {
            val takenAt = withContext(Dispatchers.IO) { takenAtMillis(uri) }
            val result = recognizer.process(InputImage.fromFilePath(context, Uri.fromFile(file))).await()
            // 단어 옆 시각을 찾으려면 줄마다 화면 위치가 필요하다
            val lines = result.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                line.boundingBox?.let { OcrLine(line.text, it.top, it.bottom) }
            }
            return ScreenshotEvidence(file, takenAt, result.text, lines, hash)
        } catch (e: Throwable) {
            file.delete()
            throw e
        }
    }

    private fun copyWithHash(uri: Uri): Pair<File, String> {
        val file = File(EvidenceCollector.evidenceDir(context), "shot_${System.currentTimeMillis()}.img")
        val digest = MessageDigest.getInstance("SHA-256")
        val input = context.contentResolver.openInputStream(uri) ?: error("캡처를 열 수 없어요")
        input.use { src ->
            file.outputStream().use { out ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = src.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                    out.write(buffer, 0, n)
                }
            }
        }
        return file to digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * 캡처한 시각. 사진 선택기가 알려 주는 촬영 시각 → 파일 이름의 시각 → 미디어에 추가된 시각 순으로 본다.
     * 선택기마다 줄 수 있는 칸이 달라서, 칸 하나씩 따로 묻고 없으면 넘어간다.
     */
    private fun takenAtMillis(uri: Uri): Long? {
        queryLong(uri, MediaStore.MediaColumns.DATE_TAKEN)?.takeIf { it > 0 }?.let { return it }
        ScreenshotRules.timeFromFileName(queryString(uri, MediaStore.MediaColumns.DISPLAY_NAME))?.let {
            return it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        queryLong(uri, MediaStore.MediaColumns.DATE_ADDED)?.takeIf { it > 0 }?.let { return it * 1000 }
        return null
    }

    private fun queryLong(uri: Uri, column: String): Long? = runCatching {
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
    }.getOrNull()

    private fun queryString(uri: Uri, column: String): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
        }
    }.getOrNull()
}

/** 올린 캡처 한 장: 앱 저장소의 복사본, 캡처한 시각(모르면 null), 읽은 글자 전체와 줄별 위치, 지문(SHA-256) */
data class ScreenshotEvidence(
    val file: File,
    val takenAtMillis: Long?,
    val text: String,
    val lines: List<OcrLine>,
    val hash: String,
)
