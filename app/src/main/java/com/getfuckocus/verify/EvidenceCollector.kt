package com.getfuckocus.verify

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.getfuckocus.hasPermission
import com.getfuckocus.safety.DeviceClock
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 앱 카메라로 촬영하고 시각·위치·이미지 라벨을 모은다. 모든 처리는 기기 안에서 끝난다. */
class EvidenceCollector(
    private val context: Context,
    private val clock: DeviceClock,
) {
    private val fused = LocationServices.getFusedLocationProviderClient(context)

    // 온디바이스 기본 모델. 이미지가 기기 밖으로 나가지 않는다.
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? {
        if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return null
        return runCatching {
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
        }.getOrNull()
    }

    suspend fun capture(imageCapture: ImageCapture, location: Location?): Evidence {
        val file = File(evidenceDir(context), "${System.currentTimeMillis()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file)
            .setMetadata(ImageCapture.Metadata().apply { this.location = location })
            .build()

        val capturedAt = clock.now()
        imageCapture.takePictureTo(options, ContextCompat.getMainExecutor(context))

        val labels = labeler.process(InputImage.fromFilePath(context, Uri.fromFile(file)))
            .await()
            .map { DetectedLabel(it.text, it.confidence) }

        return Evidence(file, capturedAt, location, labels, ExifReader.read(file))
    }

    companion object {
        fun evidenceDir(context: Context): File =
            File(context.filesDir, "evidence").apply { mkdirs() }
    }
}

private suspend fun ImageCapture.takePictureTo(
    options: ImageCapture.OutputFileOptions,
    executor: Executor,
): Unit = suspendCancellableCoroutine { cont ->
    takePicture(options, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) = cont.resume(Unit)
        override fun onError(exception: ImageCaptureException) = cont.resumeWithException(exception)
    })
}
