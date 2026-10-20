package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.notes.NotesDb
import com.arashrasoulzadeh.einkdash.util.Icon
import com.arashrasoulzadeh.einkdash.util.Ui
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.min

/** Today's habits: each habit with the last five days as dots (filled = done). Tap today's dot to tick it. */
class HabitsWidget : DashboardWidget {
    override val type = "habits"
    override val titleRes = R.string.widget_habits
    override val icon = Icon.Kind.CHART
    override val defaultW = 2
    override val defaultH = 2

    companion object {
        const val DAYS = 5
        private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("habits", Context.MODE_PRIVATE)

        fun names(ctx: Context): List<String> = prefs(ctx).getString("names", null)?.lines()?.filter { it.isNotBlank() }
            ?: ctx.resources.getStringArray(R.array.default_habits).toList()

        private fun done(ctx: Context): JSONObject = runCatching { JSONObject(prefs(ctx).getString("done", "{}")!!) }.getOrDefault(JSONObject())

        /** yyyyMMdd for [back] days ago. */
        fun day(back: Int) = NotesDb.ymd(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -back) })

        fun isDone(ctx: Context, habit: String, ymd: Int): Boolean {
            val arr = done(ctx).optJSONArray(habit) ?: return false
            return (0 until arr.length()).any { arr.getInt(it) == ymd }
        }

        fun toggle(ctx: Context, habit: String, ymd: Int) {
            val all = done(ctx)
            val days = (all.optJSONArray(habit)?.let { a -> (0 until a.length()).map { a.getInt(it) } } ?: emptyList()).toMutableSet()
            if (!days.remove(ymd)) days.add(ymd)
            // Keep a month of history; older ticks don't show anywhere.
            all.put(habit, JSONArray(days.filter { it >= day(31) }.sorted()))
            prefs(ctx).edit().putString("done", all.toString()).apply()
        }
    }

    /** Five dots, oldest first. Filled = done; today has a heavier ring. */
    class Dots(context: Context) : View(context) {
        var done = BooleanArray(DAYS)
        private val d = resources.displayMetrics.density
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.3f * d; color = Ui.MUTED }
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.INK }

        override fun onDraw(canvas: Canvas) {
            val slot = width / DAYS.toFloat()
            val r = min(slot * 0.36f, height * 0.36f)
            val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
            for (i in 0 until DAYS) {
                val col = if (rtl) DAYS - 1 - i else i
                val cx = col * slot + slot / 2; val cy = height / 2f
                if (done[i]) canvas.drawCircle(cx, cy, r, fill)
                ring.color = if (i == DAYS - 1) Ui.INK else Ui.MUTED
                canvas.drawCircle(cx, cy, r, ring)
            }
        }
    }

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes, icon) {
        val rows = fill(LinearLayout(context).apply { orientation = VERTICAL })

        override fun refresh() {
            rows.removeAllViews()
            names(context).take(5).forEach { habit ->
                val dots = Dots(context).apply {
                    done = BooleanArray(DAYS) { isDone(context, habit, day(DAYS - 1 - it)) }
                    // Tap anywhere on the row's dots to tick today.
                    Ui.onTap(this) { toggle(context, habit, day(0)); this@apply.done[DAYS - 1] = !this@apply.done[DAYS - 1]; invalidate(); Eink.fullRefresh(this) }
                }
                rows.addView(LinearLayout(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(Ui.text(context, 16f).apply { text = habit; setSingleLine() }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f))
                    addView(dots, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.1f))
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            }
        }
    }

    override fun configure(activity: Activity): Boolean {
        val input = Ui.input(activity, R.string.habits_hint).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 4; gravity = Gravity.TOP or Gravity.START
            setText(names(activity).joinToString("\n"))
        }
        AlertDialog.Builder(activity).setTitle(R.string.habits_edit).setView(LinearLayout(activity).apply {
            val p = Ui.dp(context, 16f); setPadding(p, p, p, 0); addView(input)
        }).setPositiveButton(R.string.save) { _, _ ->
            prefs(activity).edit().putString("names", input.text.toString().lines().map { it.trim() }.filter { it.isNotEmpty() }.take(5).joinToString("\n")).apply()
        }.setNegativeButton(android.R.string.cancel, null).show()
        return true
    }
}
