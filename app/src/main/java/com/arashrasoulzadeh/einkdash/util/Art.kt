package com.arashrasoulzadeh.einkdash.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

/**
 * Illustrations drawn in code (no bitmaps): sharp at any size, pure ink and grays for e-ink,
 * and nearly free in APK size.
 */
object ArtPaths {
    /** A mountain range: list of (x, y) peaks in 0..1 coordinates, left to right. */
    fun ridge(w: Float, h: Float, peaks: List<Pair<Float, Float>>): Path = Path().apply {
        moveTo(0f, h)
        peaks.forEach { (x, y) -> lineTo(x * w, y * h) }
        lineTo(w, h); close()
    }
}

/** Engraved-style mountains for the motivation card: hatched slopes, ink ridges. */
class MountainArt(context: Context) : View(context) {
    private val ridge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.INK; style = Paint.Style.STROKE; strokeWidth = 1.4f * resources.displayMetrics.density }
    private val hatch = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8A8A8A.toInt(); strokeWidth = 0.7f * resources.displayMetrics.density }
    private val paper = Paint().apply { color = Ui.PAPER }

    // Hardware drawing can't clip to a path before Android 4.3; draw this view in software there.
    init { if (android.os.Build.VERSION.SDK_INT < 18) setLayerType(LAYER_TYPE_SOFTWARE, null) }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        // Back range first, front range painted over it.
        val ranges = listOf(
            listOf(0f to 0.9f, 0.12f to 0.7f, 0.22f to 0.76f, 0.36f to 0.45f, 0.46f to 0.55f, 0.6f to 0.15f, 0.7f to 0.4f, 0.8f to 0.33f, 1f to 0.8f),
            listOf(0f to 1f, 0.2f to 0.82f, 0.32f to 0.88f, 0.5f to 0.68f, 0.66f to 0.8f, 0.85f to 0.7f, 1f to 0.95f),
        )
        val step = 4.5f * resources.displayMetrics.density
        ranges.forEachIndexed { i, peaks ->
            val p = ArtPaths.ridge(w, h, peaks)
            canvas.drawPath(p, paper)
            canvas.save(); canvas.clipPath(p)
            // Shadow side: diagonal hatching, denser on the front range.
            var x = -h
            val gap = if (i == 0) step * 1.6f else step
            while (x < w) { canvas.drawLine(x, h, x + h * 0.9f, 0f, hatch); x += gap }
            canvas.restore()
            canvas.drawPath(Path().apply { peaks.forEachIndexed { j, (px, py) -> if (j == 0) moveTo(px * w, py * h) else lineTo(px * w, py * h) } }, ridge)
        }
    }
}

/** Night scene for the sleep face: stars, a lake with a moon path, and mountain silhouettes. */
class NightScene(context: Context) : View(context) {
    private val d = resources.displayMetrics.density
    private val sky = Paint().apply { color = 0xFF0E0E0E.toInt() }
    private val far = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2C2C2C.toInt() }
    private val near = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1C1C1C.toInt() }
    private val water = Paint().apply { color = 0xFF151515.toInt() }
    private val glint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFBDBDBD.toInt(); strokeWidth = 1.2f * d }
    private val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFDDDDDD.toInt() }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        canvas.drawRect(0f, 0f, w, h, sky)
        val rnd = Random(42)
        repeat(26) { canvas.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.45f, (0.6f + rnd.nextFloat()) * d, star) }
        val horizon = h * 0.62f
        canvas.drawPath(ArtPaths.ridge(w, horizon, listOf(0f to 0.7f, 0.15f to 0.45f, 0.32f to 0.62f, 0.55f to 0.38f, 0.75f to 0.6f, 1f to 0.42f)), far)
        canvas.drawPath(ArtPaths.ridge(w, horizon, listOf(0f to 0.85f, 0.22f to 0.7f, 0.5f to 0.9f, 0.82f to 0.72f, 1f to 0.88f)), near)
        canvas.drawRect(0f, horizon, w, h, water)
        // Moonlight on the water: short glints narrowing toward the viewer.
        var y = horizon + 4 * d
        var i = 0
        while (y < h) {
            val spread = (y - horizon) / (h - horizon)
            val half = (w * 0.04f + w * 0.12f * spread) * (0.6f + 0.4f * sin(i * 1.7f))
            canvas.drawLine(w / 2 - half, y, w / 2 + half, y, glint)
            y += 5 * d; i++
        }
        // Reeds at the near shore corners.
        val reed = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3A3A3A.toInt(); strokeWidth = 1.2f * d }
        for (k in 0 until 9) {
            val x = w * 0.03f + k * 4 * d
            canvas.drawLine(x, h, x + (k % 3 - 1) * 5 * d, h - (18 + k * 3) * d, reed)
            canvas.drawLine(w - x, h, w - x - (k % 3 - 1) * 5 * d, h - (16 + k * 3) * d, reed)
        }
    }
}
