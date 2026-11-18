package com.swifts.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val MAX_IMAGE_EDGE_PX = 1600
const val MAX_VIDEO_BYTES = 50L * 1024 * 1024
const val MAX_ATTACHMENT_TOTAL_BYTES = 60L * 1024 * 1024

/** A file read from the gallery and ready to upload. Images are already resized and converted to JPEG. */
class PickedMedia(val kind: String, val mime: String, val ext: String, val bytes: ByteArray)

/** Reads a picked photo or video. Never throws: failures come back as a failed Result. */
suspend fun readPickedMedia(context: Context, uri: Uri): Result<PickedMedia> = withContext(Dispatchers.IO) {
    runCatching {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        if (mime.startsWith("video/")) readVideo(context, uri, mime) else readImage(context, uri)
    }
}

private fun readVideo(context: Context, uri: Uri, mime: String): PickedMedia {
    val declared = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
    require(declared <= MAX_VIDEO_BYTES) { "Video is larger than 50 MB" }
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Cannot read video")
    require(bytes.size <= MAX_VIDEO_BYTES) { "Video is larger than 50 MB" }
    val ext = when (mime) {
        "video/quicktime" -> "mov"
        "video/webm" -> "webm"
        else -> "mp4"
    }
    return PickedMedia("video", mime, ext, bytes)
}

private fun readImage(context: Context, uri: Uri): PickedMedia {
    val decoded: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // Android 9+: ImageDecoder applies the photo's rotation, so pictures come out upright.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val scale = minOf(1f, MAX_IMAGE_EDGE_PX.toFloat() / maxOf(info.size.width, info.size.height))
            decoder.setTargetSize(
                (info.size.width * scale).toInt().coerceAtLeast(1),
                (info.size.height * scale).toInt().coerceAtLeast(1),
            )
        }
    } else {
        // Android 8: sample the image down while decoding. This path ignores photo rotation, so a few photos may show sideways.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_IMAGE_EDGE_PX * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("Cannot read image")
    }

    val fitted = fitWithin(decoded)
    // JPEG has no transparency, so draw onto white first. Transparent areas would otherwise turn black.
    val flat = Bitmap.createBitmap(fitted.width, fitted.height, Bitmap.Config.ARGB_8888)
    Canvas(flat).apply {
        drawColor(android.graphics.Color.WHITE)
        drawBitmap(fitted, 0f, 0f, null)
    }
    val out = ByteArrayOutputStream()
    flat.compress(Bitmap.CompressFormat.JPEG, 85, out)
    if (fitted !== decoded) fitted.recycle()
    decoded.recycle()
    flat.recycle()
    return PickedMedia("image", "image/jpeg", "jpg", out.toByteArray())
}

private fun fitWithin(bitmap: Bitmap): Bitmap {
    val longest = maxOf(bitmap.width, bitmap.height)
    if (longest <= MAX_IMAGE_EDGE_PX) return bitmap
    val s = MAX_IMAGE_EDGE_PX.toFloat() / longest
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * s).toInt().coerceAtLeast(1),
        (bitmap.height * s).toInt().coerceAtLeast(1),
        true,
    )
}
