package com.tvmusic.app.ui.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tvmusic.app.R
import com.tvmusic.app.media.metadata.LyricLine
import kotlin.math.abs

class LyricsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var lyrics: List<LyricLine> = emptyList()
    private var currentIndex: Int = -1
    private var scrollOffset: Float = 0f
    private var targetScrollOffset: Float = 0f

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 42f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = 0xFF1A1A2E.toInt()
    }

    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 34f
        typeface = Typeface.DEFAULT
        color = 0xFF888888.toInt()
    }

    private val lineHeight = 80f
    private val animInterpolation = 0.15f

    fun setLyrics(lines: List<LyricLine>) {
        lyrics = lines
        currentIndex = -1
        scrollOffset = 0f
        targetScrollOffset = 0f
        invalidate()
    }

    fun updatePosition(positionMs: Long) {
        if (lyrics.isEmpty()) return

        val newIndex = lyrics.indexOfLast { it.timeMs <= positionMs }
        if (newIndex != currentIndex) {
            currentIndex = newIndex
            // Target scroll: center active line
            targetScrollOffset = if (currentIndex >= 0) {
                currentIndex * lineHeight
            } else 0f
        }

        // Smooth scroll animation
        val diff = targetScrollOffset - scrollOffset
        if (abs(diff) > 1f) {
            scrollOffset += diff * animInterpolation
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (lyrics.isEmpty()) return

        val cx = width / 2f
        val cy = height / 2f

        lyrics.forEachIndexed { i, line ->
            val yBase = cy + (i * lineHeight) - scrollOffset
            // Only draw lines that are in view
            if (yBase < -lineHeight || yBase > height + lineHeight) return@forEachIndexed

            val paint = if (i == currentIndex) activePaint else inactivePaint

            // Fade lines that are far from center
            val distFromCenter = abs(yBase - cy)
            val maxDist = height / 2f
            paint.alpha = when {
                i == currentIndex -> 255
                distFromCenter < maxDist * 0.4f -> 200
                distFromCenter < maxDist * 0.7f -> 140
                else -> 80
            }

            // Slightly scale active line
            if (i == currentIndex) {
                canvas.save()
                canvas.scale(1.05f, 1.05f, cx, yBase)
                canvas.drawText(line.text, cx, yBase, paint)
                canvas.restore()
            } else {
                canvas.drawText(line.text, cx, yBase, paint)
            }
        }
    }
}
