package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.latency.LatencySettingsActivity
import com.arashrasoulzadeh.einkdash.latency.LatencyStore
import com.arashrasoulzadeh.einkdash.util.Bg
import com.arashrasoulzadeh.einkdash.util.FitText
import com.arashrasoulzadeh.einkdash.util.Spark
import com.arashrasoulzadeh.einkdash.util.Ui

/** One row per endpoint: name and uptime, a bar chart of the last 10 probes, and the latest ms large. */
class LatencyWidget : DashboardWidget {
    override val type = "latency"
    override val titleRes = R.string.widget_latency
    override val defaultW = 4
    override val defaultH = 2

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val store = LatencyStore(context)
        val rows = fill(LinearLayout(context).apply { orientation = LinearLayout.VERTICAL })
        var probing = false
        var shownUrls: List<String> = emptyList()

        init { setOnClickListener { open(context) } }

        override fun refresh() {
            val urls = store.urls.take(4)
            if (urls != shownUrls) build(urls)
            urls.forEachIndexed { i, url -> bind(rows.getChildAt(i * 2) as LinearLayout, url) }
            val lastRun = store.ranAt
            label(R.string.widget_latency, if (lastRun > 0) num(android.text.format.DateFormat.format("HH:mm", lastRun).toString()) else null)
            if (urls.isNotEmpty() && !probing && store.isStale(System.currentTimeMillis())) {
                probing = true
                Bg.run({ store.probeAll() }) { probing = false; changed() }
            }
        }

        private fun build(urls: List<String>) {
            shownUrls = urls
            rows.removeAllViews()
            if (urls.isEmpty()) {
                rows.addView(Ui.text(context, 16f).apply { setText(R.string.latency_tap_setup); gravity = Gravity.CENTER },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
                return
            }
            urls.forEachIndexed { i, _ ->
                if (i > 0) rows.addView(Ui.rule(context).apply { alpha = 1f })
                val name = Ui.text(context, 14f, Ui.bold).apply { setSingleLine() }
                val status = Ui.text(context, 12f)
                val left = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(name); addView(status)
                }
                val spark = Spark(context)
                val ms = FitText(context, Ui.light, 40f).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.END }
                val pad = (6 * dp).toInt()
                rows.addView(LinearLayout(context).apply {
                    setPadding(0, pad, 0, pad)
                    addView(left, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.1f))
                    addView(spark, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.4f).apply {
                        marginStart = pad * 2; marginEnd = pad * 2
                    })
                    addView(ms, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.9f))
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            }
        }

        private fun bind(row: LinearLayout, url: String) {
            val left = row.getChildAt(0) as LinearLayout
            val spark = row.getChildAt(1) as Spark
            val ms = row.getChildAt(2) as FitText
            val h = store.history(url)
            (left.getChildAt(0) as android.widget.TextView).text =
                Uri.parse(if ("://" in url) url else "https://$url").host?.removePrefix("www.") ?: url
            val up = h.count { it >= 0 }
            (left.getChildAt(1) as android.widget.TextView).text = when {
                h.isEmpty() -> context.getString(R.string.latency_waiting)
                h.last() < 0 -> context.getString(R.string.latency_down) + num("  $up/${h.size}")
                else -> context.getString(R.string.latency_up) + num("  $up/${h.size}")
            }
            spark.values = h
            ms.text = when {
                h.isEmpty() -> "—"
                h.last() < 0 -> "✕"
                else -> num("${h.last()}") + " ms"
            }
        }
    }

    override fun configure(activity: Activity): Boolean { open(activity); return true }

    private fun open(context: Context) = context.startActivity(Intent(context, LatencySettingsActivity::class.java))
}
