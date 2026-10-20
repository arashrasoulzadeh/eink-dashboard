package com.arashrasoulzadeh.einkdash.widgets

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.util.Icon
import com.arashrasoulzadeh.einkdash.util.MountainArt
import com.arashrasoulzadeh.einkdash.util.Ui

/** A short motto of the day beside an engraved mountain range. */
class MottoWidget : DashboardWidget {
    override val type = "motto"
    override val titleRes = R.string.widget_motto
    override val defaultW = 4
    override val defaultH = 2

    override fun createView(context: Context): WidgetView = object : WidgetView(context, 0) {
        val motto = Ui.text(context, 18f).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.25f) }

        init {
            removeAllViews()
            setPadding((14 * dp).toInt(), (10 * dp).toInt(), 0, 0)
            addView(LinearLayout(context).apply {
                // Motto on the start side (right in Persian), art toward the end, like the mockup.
                addView(LinearLayout(context).apply {
                    orientation = VERTICAL
                    gravity = Gravity.CENTER
                    addView(motto)
                    addView(Icon(context, Icon.Kind.LEAF).apply { color = Ui.MUTED }, LayoutParams((20 * dp).toInt(), (20 * dp).toInt()).apply { topMargin = (6 * dp).toInt() })
                }, LayoutParams(0, LayoutParams.MATCH_PARENT, 0.8f))
                addView(MountainArt(context), LayoutParams(0, LayoutParams.MATCH_PARENT, 1.2f))
            }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }

        override fun refresh() {
            val all = context.resources.getStringArray(R.array.mottos)
            val day = System.currentTimeMillis() / 86_400_000L
            motto.text = all[(day % all.size).toInt()]
        }
    }
}
