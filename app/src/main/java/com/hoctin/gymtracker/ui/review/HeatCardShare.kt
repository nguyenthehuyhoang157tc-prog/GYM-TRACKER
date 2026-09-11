package com.hoctin.gymtracker.ui.review

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.PathParser
import com.hoctin.gymtracker.domain.muscleheat.MuscleHeatUiModel
import com.hoctin.gymtracker.ui.muscleheat.BODY_BACK_OUTLINE
import com.hoctin.gymtracker.ui.muscleheat.BODY_BACK_PATHS
import com.hoctin.gymtracker.ui.muscleheat.BODY_FRONT_OUTLINE
import com.hoctin.gymtracker.ui.muscleheat.BODY_FRONT_PATHS
import com.hoctin.gymtracker.ui.muscleheat.BodyMapSide
import com.hoctin.gymtracker.ui.muscleheat.BodyMapSlugs
import com.hoctin.gymtracker.ui.muscleheat.MuscleHeatPalette
import com.hoctin.gymtracker.ui.muscleheat.normalizeSvgArcFlags
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val HEAT_CARD_SHARE_DIR = "shared_heat_cards"
private const val HEAT_CARD_SHARE_MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L
private const val HEAT_CARD_EXPORT_WIDTH = 1080
private const val HEAT_CARD_EXPORT_BASE_HEIGHT_WITH_DURATION = 1240
private const val HEAT_CARD_EXPORT_BASE_HEIGHT_WITHOUT_DURATION = 1110
private const val HEAT_CARD_EXPORT_FOOTNOTE_LINE_HEIGHT = 34

internal data class HeatCardShareContent(
    val heat: MuscleHeatUiModel?,
    val topMuscles: List<String>,
    val durationText: String?,
)

internal fun buildHeatCardShareFileName(sessionId: Long, nowMillis: Long): String =
    "auron_heat_session_${sessionId}_${nowMillis}.png"

internal fun heatCardExportHeight(
    hasDuration: Boolean,
    footnoteLineCount: Int,
): Int {
    val base = if (hasDuration) {
        HEAT_CARD_EXPORT_BASE_HEIGHT_WITH_DURATION
    } else {
        HEAT_CARD_EXPORT_BASE_HEIGHT_WITHOUT_DURATION
    }
    return base + footnoteLineCount.coerceAtLeast(0) * HEAT_CARD_EXPORT_FOOTNOTE_LINE_HEIGHT
}

internal fun cleanupOldHeatCardShares(
    directory: File,
    nowMillis: Long,
    maxAgeMillis: Long = HEAT_CARD_SHARE_MAX_AGE_MILLIS,
) {
    directory.listFiles()
        ?.filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
        ?.filter { nowMillis - it.lastModified() > maxAgeMillis }
        ?.forEach { it.delete() }
}

internal suspend fun shareHeatCard(
    context: Context,
    content: HeatCardShareContent,
    sessionId: Long,
): Uri {
    val bitmap = withContext(Dispatchers.Default) {
        renderHeatCardShareBitmap(content)
    }
    try {
        val nowMillis = System.currentTimeMillis()
        val uri = withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, HEAT_CARD_SHARE_DIR).apply {
                if (!exists() && !mkdirs()) {
                    error("Could not create heat-card share cache.")
                }
            }
            cleanupOldHeatCardShares(directory, nowMillis)
            val file = File(directory, buildHeatCardShareFileName(sessionId, nowMillis))
            FileOutputStream(file).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    error("Could not encode heat-card image.")
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share heat card")
        if (context.findActivity() == null) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return uri
    } finally {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}

private fun renderHeatCardShareBitmap(content: HeatCardShareContent): Bitmap {
    val footnotes = content.footnotes()
    val height = heatCardExportHeight(
        hasDuration = content.durationText != null,
        footnoteLineCount = footnotes.size,
    )
    val bitmap = Bitmap.createBitmap(HEAT_CARD_EXPORT_WIDTH, height, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(AndroidColor.TRANSPARENT)
    val canvas = Canvas(bitmap)
    val card = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
    val corner = 74f

    val clip = Path().apply { addRoundRect(card, corner, corner, Path.Direction.CW) }
    canvas.save()
    canvas.clipPath(clip)
    canvas.drawRoundRect(card, corner, corner, paint(fill = 0xFF0B0C0E.toInt()))
    drawSubtleCardGradient(canvas, card)

    var y = 82f
    if (content.durationText != null) {
        y = drawCenteredText(canvas, "Duration", y, 34f, 0xFF9BA1AB.toInt(), Typeface.BOLD) + 20f
        y = drawCenteredText(
            canvas = canvas,
            text = content.durationText.uppercase(),
            baseline = y + 76f,
            textSize = 78f,
            color = 0xFFF2F3F5.toInt(),
            typefaceStyle = Typeface.BOLD,
        ) + 56f
    }

    y = drawCenteredText(canvas, "Top Muscles", y, 34f, 0xFF9BA1AB.toInt(), Typeface.BOLD) + 20f
    val topMuscleText = content.topMuscles
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
        ?.uppercase()
        ?: "-"
    y = drawCenteredWrappedText(
        canvas = canvas,
        text = topMuscleText,
        baseline = y + 58f,
        textSize = 58f,
        color = 0xFFF2F3F5.toInt(),
        typefaceStyle = Typeface.BOLD,
        maxWidth = bitmap.width - 150f,
        lineGap = 12f,
    ) + 52f

    if (content.heat != null) {
        val mapTop = y
        val mapHeight = 690f
        val figureWidth = 405f
        val gap = 44f
        val totalWidth = figureWidth * 2f + gap
        val left = (bitmap.width - totalWidth) / 2f
        drawBodyMapFigure(canvas, BodyMapSide.FRONT, content.heat, RectF(left, mapTop, left + figureWidth, mapTop + mapHeight))
        drawBodyMapFigure(
            canvas,
            BodyMapSide.BACK,
            content.heat,
            RectF(left + figureWidth + gap, mapTop, left + figureWidth * 2f + gap, mapTop + mapHeight),
        )
        y = mapTop + mapHeight + 44f
    } else {
        y = drawCenteredWrappedText(
            canvas = canvas,
            text = "Muscle map unavailable for this session.",
            baseline = y + 64f,
            textSize = 32f,
            color = 0xFF9BA1AB.toInt(),
            typefaceStyle = Typeface.NORMAL,
            maxWidth = bitmap.width - 170f,
            lineGap = 8f,
        ) + 86f
    }

    footnotes.forEach { note ->
        y = drawCenteredWrappedText(
            canvas = canvas,
            text = note,
            baseline = y,
            textSize = 26f,
            color = 0xFF9BA1AB.toInt(),
            typefaceStyle = Typeface.NORMAL,
            maxWidth = bitmap.width - 150f,
            lineGap = 6f,
        ) + 10f
    }

    val wordmarkBaseline = (bitmap.height - 74f).coerceAtLeast(y + 48f)
    drawLetterSpacedCenteredText(
        canvas = canvas,
        text = "AURON",
        baseline = wordmarkBaseline,
        textSize = 42f,
        color = 0xFFF2F3F5.toInt(),
        letterSpacingPx = 25f,
    )
    canvas.restore()

    canvas.drawRoundRect(
        RectF(1.5f, 1.5f, bitmap.width - 1.5f, bitmap.height - 1.5f),
        corner,
        corner,
        paint(fill = null, stroke = 0x1AFFFFFF, strokeWidth = 3f),
    )
    return bitmap
}

private fun HeatCardShareContent.footnotes(): List<String> = buildList {
    heat?.summaryOnly.orEmpty().forEach { add("${it.displayName}: ${it.reason}") }
    val unattributed = heat?.unattributedExercises.orEmpty()
    if (unattributed.isNotEmpty()) {
        add("Not on the map (custom exercises): ${unattributed.joinToString(", ")}")
    }
}

private fun drawSubtleCardGradient(canvas: Canvas, rect: RectF) {
    val glow = android.graphics.RadialGradient(
        rect.centerX(),
        0f,
        rect.width() * 0.85f,
        intArrayOf(0x332B155D, 0x00000000),
        floatArrayOf(0f, 1f),
        android.graphics.Shader.TileMode.CLAMP,
    )
    canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = glow })
}

private fun drawBodyMapFigure(
    canvas: Canvas,
    side: BodyMapSide,
    heat: MuscleHeatUiModel,
    bounds: RectF,
) {
    val source = if (side == BodyMapSide.FRONT) androidFrontPaths else androidBackPaths
    val outline = if (side == BodyMapSide.FRONT) androidFrontOutline else androidBackOutline
    val slugTokens = BodyMapSlugs.slugTokens(side, heat.regions)
    val viewportW = 724f
    val viewportH = 1448f
    val scale = minOf(bounds.width() / viewportW, bounds.height() / viewportH)
    val offsetX = bounds.left + (bounds.width() - viewportW * scale) / 2f
    val offsetY = bounds.top + (bounds.height() - viewportH * scale) / 2f

    canvas.save()
    canvas.translate(offsetX, offsetY)
    canvas.scale(scale, scale)
    if (side == BodyMapSide.BACK) {
        canvas.translate(-viewportW, 0f)
    }
    canvas.drawPath(outline, paint(fill = null, stroke = 0xFF9BA1AB.toInt(), strokeWidth = 3f))
    source.forEach { (slug, paths) ->
        val fill = slugTokens[slug]
            ?.let(MuscleHeatPalette::heatArgb)
            ?.toInt()
            ?: 0xFF3B3E44.toInt()
        paths.forEach { path ->
            canvas.drawPath(path, paint(fill = fill))
            canvas.drawPath(path, paint(fill = null, stroke = 0xFF14161A.toInt(), strokeWidth = 1.6f))
        }
    }
    canvas.restore()
}

private val androidFrontPaths: Map<String, List<Path>> by lazy {
    BODY_FRONT_PATHS.mapValues { (_, paths) -> paths.map(::androidPathOf) }
}

private val androidBackPaths: Map<String, List<Path>> by lazy {
    BODY_BACK_PATHS.mapValues { (_, paths) -> paths.map(::androidPathOf) }
}

private val androidFrontOutline: Path by lazy { androidPathOf(BODY_FRONT_OUTLINE) }
private val androidBackOutline: Path by lazy { androidPathOf(BODY_BACK_OUTLINE) }

private fun androidPathOf(pathData: String): Path =
    PathParser.createPathFromPathData(normalizeSvgArcFlags(pathData))

private fun drawCenteredText(
    canvas: Canvas,
    text: String,
    baseline: Float,
    textSize: Float,
    color: Int,
    typefaceStyle: Int,
): Float {
    val textPaint = textPaint(textSize, color, typefaceStyle)
    canvas.drawText(text, canvas.width / 2f, baseline, textPaint)
    return baseline
}

private fun drawCenteredWrappedText(
    canvas: Canvas,
    text: String,
    baseline: Float,
    textSize: Float,
    color: Int,
    typefaceStyle: Int,
    maxWidth: Float,
    lineGap: Float,
): Float {
    val textPaint = textPaint(textSize, color, typefaceStyle)
    var y = baseline
    wrapText(text, textPaint, maxWidth).forEach { line ->
        canvas.drawText(line, canvas.width / 2f, y, textPaint)
        y += textSize + lineGap
    }
    return y - textSize - lineGap
}

private fun wrapText(text: String, textPaint: Paint, maxWidth: Float): List<String> {
    val words = text.split(" ").filter { it.isNotBlank() }
    if (words.isEmpty()) return listOf(text)
    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (textPaint.measureText(candidate) <= maxWidth || current.isEmpty()) {
            current = candidate
        } else {
            lines += current
            current = word
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines
}

private fun drawLetterSpacedCenteredText(
    canvas: Canvas,
    text: String,
    baseline: Float,
    textSize: Float,
    color: Int,
    letterSpacingPx: Float,
) {
    val textPaint = textPaint(textSize, color, Typeface.BOLD)
    val glyphWidths = text.map { textPaint.measureText(it.toString()) }
    val totalWidth = glyphWidths.sum() + letterSpacingPx * (text.length - 1)
    var x = (canvas.width - totalWidth) / 2f
    text.forEachIndexed { index, char ->
        canvas.drawText(char.toString(), x, baseline, textPaint.apply { textAlign = Paint.Align.LEFT })
        x += glyphWidths[index] + letterSpacingPx
    }
}

private fun textPaint(textSize: Float, color: Int, typefaceStyle: Int): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = textSize
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, typefaceStyle)
    }

private fun paint(fill: Int? = null, stroke: Int? = null, strokeWidth: Float = 0f): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        when {
            fill != null -> {
                color = fill
                style = Paint.Style.FILL
            }
            stroke != null -> {
                color = stroke
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
            }
        }
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
