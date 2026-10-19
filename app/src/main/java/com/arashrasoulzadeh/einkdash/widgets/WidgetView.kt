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

/**
 * Base view for widgets: a small uppercase section label, then content.
 * The home screen calls [refresh] on its ticker and then does one full e-ink refresh.
 */
abstract class WidgetView(context: Context, titleRes: Int) : LinearLayout(context) {

    protected val dp = resources.displayMetrics.density
    protected val title: TextView = Ui.label(context, titleRes.takeIf { it != 0 } ?: R.string.empty)

    /** Widget type; set by whoever hosts the view so the expand icon knows what to open. */
    var type: String? = null

    /** Header expand icon; hidden when the widget is already full screen. */
    val expand = ExpandIcon(context).apply {
        setOnClickListener {
            type?.let { t -> context.startActivity(Intent(context, FullscreenActivity::class.java).putExtra(FullscreenActivity.EXTRA_TYPE, t)) }
        }
    }

    init {
        orientation = VERTICAL
        val pad = (12 * dp).toInt()
        setPadding(pad, (6 * dp).toInt(), (6 * dp).toInt(), pad)
        val icon = (36 * dp).toInt()
        addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(expand, LayoutParams(icon, icon))
        })
        addView(Ui.gap(context, 2f))
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
        title.text = (if (extra.isNullOrEmpty()) base else base + Fa.sep(context) + extra).uppercase(resources.configuration.locale)
    }

    abstract fun refresh()

    /** How soon this widget needs its next refresh; the home ticker also fires every minute. */
    open fun nextDelayMs(now: Long): Long = Long.MAX_VALUE

    /** For results that arrive between ticks (network). */
    protected fun changed() {
        refresh()
        Eink.fullRefresh(rootView)
    }
}
