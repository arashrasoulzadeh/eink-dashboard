package com.arashrasoulzadeh.einkdash.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import java.util.Calendar
import kotlin.math.min

/**
 * A month drawn on canvas: weekday header, then day cells with the primary calendar's number large
 * and the other calendar's number small. Today is circled; days with due todos get a dot.
 */
class MonthGrid(context: Context) : View(context) {

    class Day(val primary: Int, val secondary: Int, val ymd: Int)

    var weekdays: List<String> = emptyList()
    var days: List<Day> = emptyList()
    /** Column (0 = first weekday) of the 1st of the month. */
    var offset = 0
    var today = 0
    var marked: Set<Int> = emptySet()
    var digits: (String) -> String = { it }
    /** Second-calendar numbers under each day; off in the compact card style. */
    var showSecondary = false

    private val density = resources.displayMetrics.density
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.INK; typeface = Ui.regular; textAlign = Paint.Align.CENTER }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.INK; typeface = Ui.regular; textAlign = Paint.Align.CENTER }
    private val head = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.MUTED; typeface = Ui.regular; textAlign = Paint.Align.CENTER }
    private val todayFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.INK }
    private val todayText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.PAPER; typeface = Ui.bold; textAlign = Paint.Align.CENTER }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.INK; style = Paint.Style.STROKE; strokeWidth = 1.8f * density }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.INK }

    fun update() = invalidate()

    override fun onDraw(canvas: Canvas) {
        val rows = (offset + days.size + 6) / 7
        if (rows == 0) return
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val cw = width / 7f
        val headH = min(height * 0.12f, 22 * density)
        val ch = (height - headH) / rows
        head.textSize = headH * 0.6f
        big.textSize = min(cw, ch) * (if (showSecondary) 0.42f else 0.46f)
        small.textSize = big.textSize * 0.5f
        fun x(col: Int) = (if (rtl) 6 - col else col) * cw + cw / 2
        weekdays.forEachIndexed { i, w -> canvas.drawText(w, x(i), headH * 0.75f, head) }
        days.forEachIndexed { i, d ->
            val cell = offset + i
            val cx = x(cell % 7)
            val top = headH + (cell / 7) * ch
            val cy = top + ch * (if (showSecondary) 0.45f else 0.5f)
            val isToday = d.ymd == today
            // Today: a filled ink disc with paper digits.
            if (isToday) canvas.drawCircle(cx, cy, min(cw, ch) * 0.44f, todayFill)
            todayText.textSize = big.textSize
            canvas.drawText(digits(d.primary.toString()), cx, cy + big.textSize * 0.35f, if (isToday) todayText else big)
            if (showSecondary) canvas.drawText(digits(d.secondary.toString()), cx, cy + big.textSize * 0.35f + small.textSize * 1.25f, small)
            if (d.ymd in marked) canvas.drawCircle(cx + big.textSize * 0.75f, cy - big.textSize * 0.45f, 2.6f * density, dot)
        }
    }

    companion object {
        fun ymd(c: Calendar) = c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
    }
}
