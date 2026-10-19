package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.latency.LatencySettingsActivity
import com.arashrasoulzadeh.einkdash.latency.LatencyStore
import com.arashrasoulzadeh.einkdash.util.Bg

class LatencyWidget : DashboardWidget {
    override val type = "latency"
    override val titleRes = R.string.widget_latency
    override val defaultW = 2
    override val defaultH = 1

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val store = LatencyStore(context)
        val body = line(14f).apply { setLineSpacing(0f, 1.25f) }
        var probing = false

        init { setOnClickListener { open(context) } }

        override fun refresh() {
            val urls = store.urls
            if (urls.isEmpty()) { body.setText(R.string.latency_tap_setup); return }
            body.text = urls.joinToString("\n") { url ->
                val h = store.history(url)
                val host = Uri.parse(if ("://" in url) url else "https://$url").host ?: url
                val last = h.lastOrNull()
                val status = when {
                    last == null -> "…"
                    last < 0 -> context.getString(R.string.latency_down)
                    else -> num("$last ms")
                }
                val up = h.count { it >= 0 }
                "$host  $status" + if (h.isNotEmpty()) num("  ($up/${h.size})") else ""
            }
            if (!probing && store.isStale(System.currentTimeMillis())) {
                probing = true
                Bg.run({ store.probeAll() }) { probing = false; changed() }
            }
        }
    }

    override fun configure(activity: Activity): Boolean { open(activity); return true }

    private fun open(context: Context) = context.startActivity(Intent(context, LatencySettingsActivity::class.java))
}
