package com.arashrasoulzadeh.einkdash.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import kotlin.math.min

/** Single-line text whose size grows to fill its box, so big figures never leave cells empty. */
class FitText(context: Context, face: Typeface, private val maxSp: Float = 200f) : TextView(context) {
    init {
        typeface = face
        setTextColor(Ui.INK)
        setSingleLine()
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        refit()
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        refit()
    }

    private fun refit() {
        val w = width - paddingLeft - paddingRight
        val h = height - paddingTop - paddingBottom
        if (w <= 0 || h <= 0 || text.isNullOrEmpty()) return
        val p = Paint(paint)
        val density = resources.displayMetrics.scaledDensity
        var lo = 6f; var hi = maxSp
        while (hi - lo > 0.5f) {
            val mid = (lo + hi) / 2
            p.textSize = mid * density
            val fm = p.fontMetrics
            if (p.measureText(text.toString()) <= w && fm.descent - fm.ascent <= h) lo = mid else hi = mid
        }
        if (kotlin.math.abs(textSize / density - lo) > 0.5f) textSize = lo
    }
}

private fun ink(ctx: Context, strokeDp: Float, fill: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Ui.INK
    strokeWidth = strokeDp * ctx.resources.displayMetrics.density
    style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
    strokeCap = Paint.Cap.BUTT
}

/** Last-N latency bars on a baseline; a failed probe is a short cross. */
class Spark(context: Context) : View(context) {
    var values: List<Int> = emptyList()
        set(v) { field = v; invalidate() }
    var slots = 10
    private val bar = ink(context, 0f, fill = true)
    private val line = ink(context, 1f)
    private val cross = ink(context, 1.5f)

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        canvas.drawLine(0f, h - 0.5f, w, h - 0.5f, line)
        val max = (values.filter { it >= 0 }.maxOrNull() ?: 1).coerceAtLeast(1)
        val slot = w / maxOf(slots, values.size)
        val bw = slot * 0.62f
        val start = slots - values.size
        values.forEachIndexed { i, v ->
            val x = (start + i) * slot + (slot - bw) / 2
            if (v < 0) {
                val c = min(bw, h * 0.3f) / 2; val cx = x + bw / 2; val cy = h - c - 2
                canvas.drawLine(cx - c, cy - c, cx + c, cy + c, cross)
                canvas.drawLine(cx - c, cy + c, cx + c, cy - c, cross)
            } else {
                val bh = (v.toFloat() / max * (h - 2)).coerceAtLeast(2f)
                canvas.drawRect(x, h - bh, x + bw, h, bar)
            }
        }
    }
}

/** Circular progress with a fitted label in the middle. */
class Ring(context: Context) : View(context) {
    var progress = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    var label = ""
        set(v) { field = v; invalidate() }
    /** Small unit line under the label, e.g. "min". */
    var unit = ""
        set(v) { field = v; invalidate() }
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.MUTED; typeface = Ui.regular; textAlign = Paint.Align.CENTER }
    private val thin = ink(context, 4f).apply { color = Ui.TINT }
    private val thick = ink(context, 3.5f).apply { strokeCap = Paint.Cap.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.INK; typeface = Ui.bold; textAlign = Paint.Align.CENTER }
    /** Ink colors for drawing on a dark background. */
    fun inverted() { thin.color = 0xFF444444.toInt(); thick.color = Ui.PAPER; text.color = Ui.PAPER; unitPaint.color = 0xFFBBBBBB.toInt() }

    override fun onDraw(canvas: Canvas) {
        val d = min(width, height).toFloat()
        if (d <= 0) return
        val r = d / 2 - thick.strokeWidth
        val cx = width / 2f; val cy = height / 2f
        canvas.drawCircle(cx, cy, r, thin)
        if (progress > 0f) canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, 360f * progress, false, thick)
        text.textSize = r * 0.5f
        while (text.measureText(label) > r * 1.6f && text.textSize > 8f) text.textSize -= 2f
        val fm = text.fontMetrics
        if (unit.isEmpty()) {
            canvas.drawText(label, cx, cy - (fm.ascent + fm.descent) / 2, text)
        } else {
            unitPaint.textSize = r * 0.17f
            val ufm = unitPaint.fontMetrics
            val labelH = fm.descent - fm.ascent
            val unitH = ufm.descent - ufm.ascent
            val top = cy - (labelH + unitH) / 2
            canvas.drawText(label, cx, top - fm.ascent, text)
            canvas.drawText(unit, cx, top + labelH - ufm.ascent, unitPaint)
        }
    }
}

/** Thin outlined bar filled to [progress]; used for "how much of the day is gone". */
class Meter(context: Context) : View(context) {
    var progress = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    private val outline = ink(context, 1f)
    private val fill = ink(context, 0f, fill = true)

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0.5f, 0.5f, w - 0.5f, h - 0.5f, outline)
        val fw = (w - 2) * progress
        if (layoutDirection == LAYOUT_DIRECTION_RTL) canvas.drawRect(w - 1 - fw, 1f, w - 1, h - 1, fill)
        else canvas.drawRect(1f, 1f, 1 + fw, h - 1, fill)
    }
}

/** Four corner brackets: the "open full screen" affordance. */
class ExpandIcon(context: Context) : View(context) {
    private val pen = ink(context, 2.3f)

    override fun onDraw(canvas: Canvas) {
        val s = min(width, height) * 0.5f
        val l = (width - s) / 2; val t = (height - s) / 2; val r = l + s; val b = t + s; val k = s * 0.36f
        canvas.drawLine(l, t, l + k, t, pen); canvas.drawLine(l, t, l, t + k, pen)
        canvas.drawLine(r, t, r - k, t, pen); canvas.drawLine(r, t, r, t + k, pen)
        canvas.drawLine(l, b, l + k, b, pen); canvas.drawLine(l, b, l, b - k, pen)
        canvas.drawLine(r, b, r - k, b, pen); canvas.drawLine(r, b, r, b - k, pen)
    }
}

/** Rotate affordance: a portrait outline with a landscape outline behind it. */
class RotateIcon(context: Context) : View(context) {
    private val pen = ink(context, 1.8f)

    override fun onDraw(canvas: Canvas) {
        val u = min(width, height) / 16f
        val cx = width / 2f; val cy = height / 2f
        canvas.drawRoundRect(RectF(cx - 2 * u, cy - 3.2f * u, cx + 2 * u, cy + 3.2f * u), u * 0.6f, u * 0.6f, pen)
        canvas.drawRoundRect(RectF(cx - 3.2f * u, cy - 0.4f * u, cx + 3.2f * u, cy + 3.6f * u), u * 0.6f, u * 0.6f, pen)
    }
}

/** Padlock: shackle arc over a body. */
class LockIcon(context: Context) : View(context) {
    private val pen = ink(context, 1.8f)

    override fun onDraw(canvas: Canvas) {
        val u = min(width, height) / 16f
        val cx = width / 2f; val cy = height / 2f
        canvas.drawRoundRect(RectF(cx - 3.2f * u, cy - 0.6f * u, cx + 3.2f * u, cy + 3.8f * u), u * 0.6f, u * 0.6f, pen)
        canvas.drawArc(RectF(cx - 2f * u, cy - 4.4f * u, cx + 2f * u, cy - 0.4f * u), 180f, 180f, false, pen)
        canvas.drawLine(cx - 2f * u, cy - 2.4f * u, cx - 2f * u, cy - 0.6f * u, pen)
        canvas.drawLine(cx + 2f * u, cy - 2.4f * u, cx + 2f * u, cy - 0.6f * u, pen)
    }
}

/** Power symbol: an open ring with a bar through the gap. */
class PowerIcon(context: Context) : View(context) {
    private val pen = ink(context, 4f).apply { strokeCap = Paint.Cap.ROUND }

    override fun onDraw(canvas: Canvas) {
        val r = min(width, height) * 0.36f
        val cx = width / 2f; val cy = height / 2f
        canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -60f, 300f, false, pen)
        canvas.drawLine(cx, cy - r * 1.15f, cx, cy - r * 0.15f, pen)
    }
}
