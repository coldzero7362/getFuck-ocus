package com.getfuckocus.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.exifinterface.media.ExifInterface
import com.getfuckocus.data.MissAction
import com.getfuckocus.data.VerifyMethod
import com.getfuckocus.ui.theme.PixelIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** 인증 방식마다 화면에 쓰는 이름·아이콘·행동 문구 */
val VerifyMethod.title: String
    get() = when (this) {
        VerifyMethod.TAP -> "탭 체크"
        VerifyMethod.PHOTO -> "사진 인증"
        VerifyMethod.LOCATION -> "위치 인증"
        VerifyMethod.NFC -> "NFC 인증"
    }

val VerifyMethod.icon: ImageVector
    get() = when (this) {
        VerifyMethod.TAP -> Icons.Default.ThumbUp
        VerifyMethod.PHOTO -> PixelIcons.Camera
        VerifyMethod.LOCATION -> Icons.Default.LocationOn
        VerifyMethod.NFC -> PixelIcons.Nfc
    }

/** 인증 버튼 문구 */
val VerifyMethod.actionLabel: String
    get() = when (this) {
        VerifyMethod.TAP -> "완료 체크"
        VerifyMethod.PHOTO -> "촬영해서 인증"
        VerifyMethod.LOCATION -> "여기서 체크"
        VerifyMethod.NFC -> "태그 찍어 체크"
    }

/** 인증 버튼 아이콘. 탭 체크는 체크 표시를, 나머지는 방식 아이콘을 쓴다 */
val VerifyMethod.actionIcon: ImageVector
    get() = if (this == VerifyMethod.TAP) Icons.Default.Check else icon

val MissAction.title: String
    get() = when (this) {
        MissAction.NOTIFY -> "알림만"
        MissAction.NAG -> "억제 화면"
        MissAction.LOCK -> "앱 잠금"
    }

val MissAction.icon: ImageVector
    get() = when (this) {
        MissAction.NOTIFY -> Icons.Default.Notifications
        MissAction.NAG -> Icons.Default.Warning
        MissAction.LOCK -> Icons.Default.Lock
    }

/** 과제 카드에 붙는 짧은 표시 ("놓치면 잠금" 등). 알림만이면 null */
val MissAction.pillText: String?
    get() = when (this) {
        MissAction.NOTIFY -> null
        MissAction.NAG -> "놓치면 억제 화면"
        MissAction.LOCK -> "놓치면 잠금"
    }

/** 억제 화면에 문구를 적지 않았을 때 쓰는 기본 문구 */
const val DEFAULT_NAG_MESSAGE = "지금 안 하면 내일도 안 한다"

/** 억제 화면 이미지 폴더. 앱 내부 저장소라 다른 앱이 읽을 수 없고 백업에도 나가지 않는다 */
private fun nagImageDir(context: Context) = File(context.filesDir, "nag").apply { mkdirs() }

/** 이미지가 앱 안에 저장할 만큼 크면 긴 변을 이 크기로 줄인다 */
private const val NAG_IMAGE_MAX_SIDE = 1440

/**
 * 고른 이미지를 앱 내부 저장소로 옮긴다. 사진 방향(EXIF)을 바로잡고 크기를 줄여 JPEG로 저장한다.
 * 실패하면 null.
 */
suspend fun importNagImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= NAG_IMAGE_MAX_SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@runCatching null
        val rotation = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        val bitmap = if (rotation == 0) decoded else Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation.toFloat()) }, true,
        )
        val file = File(nagImageDir(context), "${UUID.randomUUID()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        file.absolutePath
    }.getOrNull()
}

/** 앱이 만든 억제 화면 이미지만 지운다 (다른 경로는 건드리지 않는다) */
fun deleteNagImage(context: Context, path: String?) {
    val file = path?.let(::File) ?: return
    if (file.parentFile?.canonicalPath == nagImageDir(context).canonicalPath) file.delete()
}

/** 억제 화면 이미지를 화면 크기에 맞게 읽는다. 없거나 못 읽으면 null */
@Composable
fun rememberNagImage(path: String?, maxSide: Int = 1080): ImageBitmap? {
    val image by produceState<ImageBitmap?>(null, path) {
        value = path?.let { p ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(p, bounds)
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
                    BitmapFactory.decodeFile(p, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return image
}
