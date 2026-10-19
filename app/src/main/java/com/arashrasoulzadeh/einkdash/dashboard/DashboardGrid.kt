package com.arashrasoulzadeh.einkdash.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.arashrasoulzadeh.einkdash.widgets.WidgetRegistry
import com.arashrasoulzadeh.einkdash.widgets.WidgetView

/**
 * Lays widget views out on a cols x rows grid. Thin dividers only; no shadows or fills.
 * In edit mode it swallows all touches and reports taps as grid cells.
 */
class DashboardGrid(context: Context) : ViewGroup(context) {

    var page: Page? = null
        set(value) { field = value; rebuild() }

    var editMode = false
        set(value) { field = value; invalidate() }

    var selectedId: Long? = null
        set(value) { field = value; invalidate() }

    var onLongPress: (() -> Unit)? = null
    var onCellTap: ((col: Int, row: Int) -> Unit)? = null
    /** Swipe on a stacked spot: +1 = next widget, -1 = previous (already mirrored for RTL). */
    var onStackSwipe: ((Placement, Int) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val thin = Paint().apply { color = 0xFF000000.toInt(); strokeWidth = 1f * density; style = Paint.Style.STROKE }
    private val thick = Paint().apply { color = 0xFF000000.toInt(); strokeWidth = 4f * density; style = Paint.Style.STROKE }
    private val gap = (6 * density).toInt()

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onLongPress(e: MotionEvent) {
            swallow = true
            onLongPress?.invoke()
        }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (editMode || e1 == null) return false
            val dx = e2.x - e1.x
            if (kotlin.math.abs(dx) < 40 * density || kotlin.math.abs(dx) < kotlin.math.abs(e2.y - e1.y)) return false
            val forward = if (isRtl()) dx > 0 else dx < 0
            val item = itemAt(e1.x, e1.y)?.takeIf { it.stack.size > 1 } ?: return false
            onStackSwipe?.invoke(item, if (forward) 1 else -1)
            swallow = true
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (!editMode) return false
            val p = page ?: return false
            val col = (e.x / (width.toFloat() / p.cols)).toInt().coerceIn(0, p.cols - 1)
            val row = (e.y / (height.toFloat() / p.rows)).toInt().coerceIn(0, p.rows - 1)
            // RTL: column 0 is on the right.
            val logicalCol = if (isRtl()) p.cols - 1 - col else col
            onCellTap?.invoke(logicalCol, row)
            return true
        }
    })

    private val ids = HashMap<View, Long>()
    /** Set by a long-press or a stack swipe: the child must not also see a tap. */
    private var swallow = false

    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF000000.toInt() }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF000000.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.2f * density }

    init { setWillNotDraw(false) }

    /** Room kept free at the bottom of a stacked cell for its position dots. */
    private fun stackInset(item: Placement) = if (item.stack.size > 1) (12 * density).toInt() else 0

    private fun itemAt(x: Float, y: Float): Placement? {
        val p = page ?: return null
        val col = (x / (width.toFloat() / p.cols)).toInt().coerceIn(0, p.cols - 1)
        val row = (y / (height.toFloat() / p.rows)).toInt().coerceIn(0, p.rows - 1)
        return p.at(if (isRtl()) p.cols - 1 - col else col, row)
    }

    /** Stack position dots, drawn over the widget at the bottom of its cell. */
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val r = IntArray(4)
        page?.items?.filter { it.stack.size > 1 }?.forEach { item ->
            cellRect(item, r)
            val step = 12 * density
            val cy = r[3] - 7 * density
            val x0 = (r[0] + r[2]) / 2f - step * (item.stack.size - 1) / 2
            item.stack.indices.forEach { i ->
                val idx = if (isRtl()) item.stack.size - 1 - i else i
                if (idx == item.active) canvas.drawCircle(x0 + i * step, cy, 3.5f * density, dot)
                else canvas.drawCircle(x0 + i * step, cy, 3f * density, ring)
            }
        }
    }

    /** [onlyFast]: refresh just the widgets that asked for sub-minute updates (e.g. a running pomodoro). */
    fun refreshWidgets(onlyFast: Boolean = false, now: Long = System.currentTimeMillis()) {
        for (i in 0 until childCount) {
            val w = getChildAt(i) as? WidgetView ?: continue
            if (!onlyFast || w.nextDelayMs(now) < 60_000) w.refresh()
        }
    }

    fun nextDelayMs(now: Long): Long =
        (0 until childCount).mapNotNull { getChildAt(it) as? WidgetView }.minOfOrNull { it.nextDelayMs(now) } ?: Long.MAX_VALUE

    fun rebuild() {
        removeAllViews()
        ids.clear()
        page?.items?.forEach { item ->
            val view = WidgetRegistry.get(item.type)?.createView(context)?.also { it.type = item.type; it.refresh() } ?: View(context)
            ids[view] = item.id
            addView(view)
        }
        requestLayout()
        invalidate()
    }

    private fun isRtl() = layoutDirection == View.LAYOUT_DIRECTION_RTL

    private fun cellRect(p: Placement, out: IntArray, totalW: Int = width, totalH: Int = height) {
        val pg = page ?: return
        val cw = totalW.toFloat() / pg.cols
        val ch = totalH.toFloat() / pg.rows
        val left = if (isRtl()) (pg.cols - p.col - p.w) * cw else p.col * cw
        out[0] = left.toInt() + gap
        out[1] = (p.row * ch).toInt() + gap
        out[2] = (left + p.w * cw).toInt() - gap
        out[3] = ((p.row + p.h) * ch).toInt() - gap
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(w, h)
        val r = IntArray(4)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val item = itemFor(child) ?: continue
            cellRect(item, r, w, h)
            child.measure(
                MeasureSpec.makeMeasureSpec(maxOf(0, r[2] - r[0]), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(maxOf(0, r[3] - r[1] - stackInset(item)), MeasureSpec.EXACTLY),
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val rect = IntArray(4)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val item = itemFor(child) ?: continue
            cellRect(item, rect)
            child.layout(rect[0], rect[1], rect[2], rect[3] - stackInset(item))
        }
    }

    override fun onDraw(canvas: Canvas) {
        val pg = page ?: return
        if (editMode) {
            val cw = width.toFloat() / pg.cols
            val ch = height.toFloat() / pg.rows
            for (c in 1 until pg.cols) canvas.drawLine(c * cw, 0f, c * cw, height.toFloat(), thin)
            for (r in 1 until pg.rows) canvas.drawLine(0f, r * ch, width.toFloat(), r * ch, thin)
        }
        val rect = IntArray(4)
        pg.items.forEach { item ->
            cellRect(item, rect)
            val l = rect[0].toFloat(); val t = rect[1].toFloat(); val r = rect[2].toFloat(); val b = rect[3].toFloat()
            if (editMode) {
                canvas.drawRect(l, t, r, b, if (item.id == selectedId) thick else thin)
            } else {
                // Paper look: a rule under each widget and a hairline between side-by-side ones.
                if (item.row + item.h < pg.rows) canvas.drawLine(l, b + gap, r, b + gap, thin)
                if (item.col > 0) {
                    val x = if (isRtl()) r + gap else l - gap
                    canvas.drawLine(x, t + gap, x, b - gap, thin)
                }
            }
        }
    }

    private fun itemFor(child: View): Placement? = page?.items?.firstOrNull { it.id == ids[child] }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) swallow = false
        gestures.onTouchEvent(ev)
        if (editMode) return true
        // Fling is detected on UP, before the child sees it; send CANCEL instead so the widget doesn't open.
        if (swallow) {
            if (ev.actionMasked == MotionEvent.ACTION_UP) {
                super.dispatchTouchEvent(MotionEvent.obtain(ev).apply { action = MotionEvent.ACTION_CANCEL })
            }
            return true
        }
        return super.dispatchTouchEvent(ev) || true
    }
}
