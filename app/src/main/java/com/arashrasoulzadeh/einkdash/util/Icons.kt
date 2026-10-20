package com.arashrasoulzadeh.einkdash.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Line icons drawn on a 24-unit grid, so they stay crisp at any size and render in pure ink on e-ink.
 * [filled] draws the icon white on a black disc (used for primary round buttons).
 */
class Icon(context: Context, var kind: Kind, private val filled: Boolean = false) : View(context) {
    enum class Kind { GEAR, TIMER, CHECK, NOTE, BOOK, HOME, GRID, PERSON, LEAF, TOMATO, CHART, SEARCH, BACK, PAUSE, PLAY, RESET, LOCK, ROTATE, WIFI, MOON, PREV, NEXT, CALENDAR, CLOUD }

    var color = Ui.INK
        set(v) { field = v; invalidate() }

    private val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // The crescent clips to a path, which hardware drawing can't do before Android 4.3.
    init { if (android.os.Build.VERSION.SDK_INT < 18) setLayerType(LAYER_TYPE_SOFTWARE, null) }

    override fun onDraw(canvas: Canvas) {
        val s = min(width, height).toFloat()
        if (s <= 0) return
        val ox = (width - s) / 2; val oy = (height - s) / 2
        var ink = color
        if (filled) {
            fill.color = Ui.INK
            canvas.drawCircle(width / 2f, height / 2f, s / 2, fill)
            ink = Ui.PAPER
        }
        canvas.save()
        canvas.translate(ox, oy)
        val u = s / 24f
        canvas.scale(u, u)
        if (filled) { canvas.translate(12f, 12f); canvas.scale(0.55f, 0.55f); canvas.translate(-12f, -12f) }
        pen.color = ink; pen.strokeWidth = 2.2f
        fill.color = ink
        drawIcon(canvas)
        canvas.restore()
    }

    private fun Canvas.line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(x1, y1, x2, y2, pen)
    private fun Canvas.rrect(l: Float, t: Float, r: Float, b: Float, rad: Float) = drawRoundRect(RectF(l, t, r, b), rad, rad, pen)

    private fun drawIcon(c: Canvas) {
        when (kind) {
            Kind.GEAR -> {
                c.drawCircle(12f, 12f, 3.2f, pen)
                val p = Path()
                for (i in 0 until 16) {
                    val a = Math.PI * 2 * i / 16
                    val r = if (i % 2 == 0) 9.5f else 7.2f
                    val x = 12 + r * cos(a).toFloat(); val y = 12 + r * sin(a).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close(); c.drawPath(p, pen)
            }
            Kind.TIMER -> { c.drawCircle(12f, 13f, 8f, pen); c.line(12f, 13f, 12f, 9f); c.line(12f, 13f, 15f, 14.5f); c.line(9.5f, 3f, 14.5f, 3f) }
            Kind.CHECK -> { c.drawCircle(12f, 12f, 9f, pen); c.line(8f, 12.2f, 11f, 15f); c.line(11f, 15f, 16.2f, 9.2f) }
            Kind.NOTE -> { c.rrect(4f, 4f, 18f, 20f, 2f); c.line(7.5f, 9f, 14.5f, 9f); c.line(7.5f, 12.5f, 12.5f, 12.5f); c.line(13f, 16.5f, 20.5f, 9f); c.line(20.5f, 9f, 19f, 7.5f) }
            Kind.BOOK -> {
                val p = Path().apply { moveTo(12f, 6.5f); cubicTo(9f, 4.5f, 5.5f, 4.5f, 3f, 5.5f); lineTo(3f, 19f); cubicTo(5.5f, 18f, 9f, 18f, 12f, 20f)
                    cubicTo(15f, 18f, 18.5f, 18f, 21f, 19f); lineTo(21f, 5.5f); cubicTo(18.5f, 4.5f, 15f, 4.5f, 12f, 6.5f); lineTo(12f, 20f) }
                c.drawPath(p, pen)
            }
            Kind.HOME -> {
                val p = Path().apply { moveTo(3.5f, 11f); lineTo(12f, 3.5f); lineTo(20.5f, 11f); moveTo(5.5f, 9.5f); lineTo(5.5f, 20f); lineTo(18.5f, 20f); lineTo(18.5f, 9.5f) }
                c.drawPath(p, pen); c.rrect(10f, 14f, 14f, 20f, 0.5f)
            }
            Kind.GRID -> { c.rrect(4f, 4f, 10.5f, 10.5f, 1.5f); c.rrect(13.5f, 4f, 20f, 10.5f, 1.5f); c.rrect(4f, 13.5f, 10.5f, 20f, 1.5f); c.rrect(13.5f, 13.5f, 20f, 20f, 1.5f) }
            Kind.PERSON -> { c.drawCircle(12f, 8.5f, 4f, pen); c.drawArc(RectF(4.5f, 13.5f, 19.5f, 28f), 180f, 180f, false, pen) }
            Kind.LEAF -> {
                // A sprig: curved stem with leaves alternating along it, each a pointed almond.
                c.drawPath(Path().apply { moveTo(6f, 21f); quadTo(9f, 12f, 18f, 3.5f) }, pen)
                fun leaf(x: Float, y: Float, angle: Float) {
                    c.save(); c.rotate(angle, x, y)
                    c.drawPath(Path().apply { moveTo(x, y); quadTo(x + 3f, y - 2.6f, x + 6.5f, y); quadTo(x + 3f, y + 2.6f, x, y) }, pen)
                    c.restore()
                }
                leaf(8.4f, 14.5f, -150f); leaf(9.6f, 12.4f, -20f)
                leaf(12f, 9.5f, -160f); leaf(13.4f, 7.8f, -30f)
                leaf(15.8f, 5.6f, -170f)
            }
            Kind.TOMATO -> { c.drawCircle(12f, 13.5f, 7.5f, pen); c.line(12f, 6f, 12f, 3.5f); c.line(9f, 6.5f, 12f, 6f); c.line(15f, 6.5f, 12f, 6f) }
            Kind.CHART -> { c.line(5f, 20f, 5f, 13f); c.line(10f, 20f, 10f, 8f); c.line(15f, 20f, 15f, 11f); c.line(20f, 20f, 20f, 4f) }
            Kind.SEARCH -> { c.drawCircle(10.5f, 10.5f, 6.5f, pen); c.line(15.5f, 15.5f, 20.5f, 20.5f) }
            Kind.BACK, Kind.PREV -> { c.line(15f, 5f, 8f, 12f); c.line(8f, 12f, 15f, 19f) }
            Kind.NEXT -> { c.line(9f, 5f, 16f, 12f); c.line(16f, 12f, 9f, 19f) }
            Kind.PAUSE -> { pen.strokeWidth = 3f; c.line(9f, 6f, 9f, 18f); c.line(15f, 6f, 15f, 18f) }
            Kind.PLAY -> c.drawPath(Path().apply { moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close() }, fill)
            Kind.RESET -> { c.drawArc(RectF(5f, 5f, 19f, 19f), -60f, 300f, false, pen); c.line(15.5f, 4f, 16f, 7.8f); c.line(16f, 7.8f, 12.2f, 8.2f) }
            Kind.LOCK -> { c.rrect(5f, 11f, 19f, 20.5f, 2f); c.drawArc(RectF(8f, 4f, 16f, 13f), 180f, 180f, false, pen); c.line(8f, 8.5f, 8f, 11f); c.line(16f, 8.5f, 16f, 11f) }
            Kind.ROTATE -> { c.rrect(7f, 3.5f, 14f, 15f, 1.5f); c.rrect(3.5f, 15f, 20.5f, 21f, 1.5f); c.drawArc(RectF(15f, 6f, 21f, 12f), -90f, 180f, false, pen) }
            Kind.WIFI -> { c.drawArc(RectF(2f, 5f, 22f, 25f), 225f, 90f, false, pen); c.drawArc(RectF(6f, 9f, 18f, 21f), 225f, 90f, false, pen); c.drawCircle(12f, 18f, 1.4f, fill) }
            Kind.MOON -> {
                // Crescent = disc minus an offset disc. Path.op needs API 19, so clip out the cut instead.
                val cut = Path().apply { addCircle(16f, 9f, 7f, Path.Direction.CW) }
                c.save()
                if (android.os.Build.VERSION.SDK_INT >= 26) c.clipOutPath(cut)
                else @Suppress("DEPRECATION") c.clipPath(cut, android.graphics.Region.Op.DIFFERENCE)
                c.drawCircle(12f, 12f, 8f, fill)
                c.restore()
            }
            Kind.CALENDAR -> { c.rrect(3.5f, 5.5f, 20.5f, 20f, 2f); c.line(3.5f, 10f, 20.5f, 10f); c.line(8f, 3.5f, 8f, 7f); c.line(16f, 3.5f, 16f, 7f) }
            Kind.CLOUD -> {
                val p = Path().apply { moveTo(7f, 18f); cubicTo(2.5f, 18f, 2.5f, 11.5f, 7.5f, 11.5f); cubicTo(8f, 6.5f, 15f, 6f, 16.5f, 10.5f)
                    cubicTo(21.5f, 10.5f, 21.5f, 18f, 17f, 18f); close() }
                c.drawPath(p, pen)
            }
        }
    }
}
