package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Intent
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.launcher.FullscreenActivity
import com.arashrasoulzadeh.einkdash.util.ExpandIcon
import com.arashrasoulzadeh.einkdash.util.Fa
import com.arashrasoulzadeh.einkdash.util.Ui

/** Screens that host widgets and run the refresh ticker. */
interface WidgetHost {
    /** Re-run the ticker now, e.g. after a widget switches to per-second updates. */
    fun retick()
}

/**
 * Base view for widgets: a small uppercase section label, then content.
 * The home screen calls [refresh] on its ticker and then does one full e-ink refresh.
 */
abstract class WidgetView(context: Context, titleRes: Int, icon: com.arashrasoulzadeh.einkdash.util.Icon.Kind? = null) : LinearLayout(context) {

    protected val dp = resources.displayMetrics.density
    protected val title: TextView = Ui.label(context, titleRes.takeIf { it != 0 } ?: R.string.empty)

    /** Widget type; set by whoever hosts the view so the expand icon knows what to open. */
    var type: String? = null

    /** Header expand icon; hidden when the widget is already full screen. */
    val expand = ExpandIcon(context).apply {
        alpha = 0.7f
        setOnClickListener {
            type?.let { t -> context.startActivity(Intent(context, FullscreenActivity::class.java).putExtra(FullscreenActivity.EXTRA_TYPE, t)) }
        }
    }

    init {
        orientation = VERTICAL
        val pad = (14 * dp).toInt()
        setPadding(pad, (8 * dp).toInt(), (8 * dp).toInt(), pad)
        val tap = (40 * dp).toInt()
        addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            // The header is its own touch target: a tap near ⛶ or the title never reaches the widget
            // (which, for the pomodoro, would start or stop the timer).
            isClickable = true
            if (icon != null) addView(com.arashrasoulzadeh.einkdash.util.Icon(context, icon), LayoutParams((20 * dp).toInt(), (20 * dp).toInt()).apply { marginEnd = (8 * dp).toInt() })
            addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(expand, LayoutParams(tap, tap))
        })
        addView(Ui.gap(context, 4f))
    }

    protected fun line(sizeSp: Float, face: Typeface = Ui.regular, center: Boolean = false): TextView =
        Ui.text(context, sizeSp, face).apply {
            if (center) gravity = Gravity.CENTER_HORIZONTAL
        }.also { addView(it) }

    /** Adds a view that takes the remaining height (shared by weight). */
    protected fun <T : View> fill(v: T, weight: Float = 1f): T {
        addView(v, LayoutParams(LayoutParams.MATCH_PARENT, 0, weight))
        return v
    }

    protected fun num(s: String) = Fa.digits(context, s)

    /** Sets text and collapses the line when empty, so prompts don't float mid-cell. */
    protected fun TextView.show(s: CharSequence?) {
        text = s
        visibility = if (s.isNullOrEmpty()) GONE else VISIBLE
    }

    protected fun label(textRes: Int, extra: String? = null) {
        val base = context.getString(textRes)
        title.text = if (extra.isNullOrEmpty()) base else base + Fa.sep(context) + extra
    }

    abstract fun refresh()

    /** How soon this widget needs its next refresh; the home ticker also fires every minute. */
    open fun nextDelayMs(now: Long): Long = Long.MAX_VALUE

    /** Ask the hosting screen to reschedule its ticker (picks up a new [nextDelayMs] right away). */
    protected fun retick() { (context as? WidgetHost)?.retick() }

    /** For results that arrive between ticks (network). */
    protected fun changed() {
        refresh()
        Eink.fullRefresh(rootView)
    }
}
