package com.hoctin.gymtracker.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val THUMBNAIL_CACHE_BYTES = 12 * 1024 * 1024

private object ExerciseAssetBitmapCache : LruCache<String, Bitmap>(THUMBNAIL_CACHE_BYTES) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
}

/**
 * Compact, reusable catalog-image thumbnail. It falls back to the app's existing purple dumbbell
 * treatment for custom, unmatched, or unreadable image references.
 */
@Composable
fun ExerciseThumbnail(
    imageRef: String?,
    exerciseName: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val bitmap = rememberExerciseAssetImageBitmap(imageRef = imageRef, targetSize = size)
    if (bitmap == null) {
        IconThumbnail(
            icon = Icons.Outlined.FitnessCenter,
            modifier = modifier,
            size = size.value.roundToInt(),
        )
        return
    }

    val shape = RoundedCornerShape((size.value / 3.2f).dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(gymContainerBorder(alpha = 0.10f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "$exerciseName exercise image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
    }
}

/**
 * Reads a bundled exercise asset off the main thread and reuses a bounded bitmap cache. A null result
 * is intentional: callers render the existing fallback when an asset reference is unavailable.
 */
@Composable
fun rememberExerciseAssetImageBitmap(
    imageRef: String?,
    targetSize: Dp,
): ImageBitmap? {
    val context = LocalContext.current
    val targetPx = with(LocalDensity.current) { targetSize.roundToPx().coerceAtLeast(1) }
    val bitmap by produceState<Bitmap?>(initialValue = null, imageRef, targetPx) {
        value = if (imageRef.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                val cacheKey = "$imageRef@$targetPx"
                ExerciseAssetBitmapCache.get(cacheKey) ?: runCatching {
                    decodeAssetBitmap(context, imageRef, targetPx)
                }.getOrNull()?.also { decoded ->
                    ExerciseAssetBitmapCache.put(cacheKey, decoded)
                }
            }
        }
    }
    return bitmap?.asImageBitmap()
}

private fun decodeAssetBitmap(context: android.content.Context, imageRef: String, targetPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.assets.open(imageRef).use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = thumbnailSampleSize(bounds.outWidth, bounds.outHeight, targetPx)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return context.assets.open(imageRef).use { stream -> BitmapFactory.decodeStream(stream, null, options) }
}

private fun thumbnailSampleSize(width: Int, height: Int, targetPx: Int): Int {
    var sample = 1
    while (width / (sample * 2) >= targetPx && height / (sample * 2) >= targetPx) {
        sample *= 2
    }
    return sample
}
