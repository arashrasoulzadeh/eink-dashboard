package com.arashrasoulzadeh.einkdash.launcher

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.util.BaseActivity
import com.arashrasoulzadeh.einkdash.util.Ui
import com.arashrasoulzadeh.einkdash.widgets.WidgetRegistry
import com.arashrasoulzadeh.einkdash.widgets.WidgetView

/** One widget across the whole screen, with its own ticker. Back or Close returns home. */
class FullscreenActivity : BaseActivity(), com.arashrasoulzadeh.einkdash.widgets.WidgetHost {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var widget: WidgetView
    private var lastFull = 0L

    private val tick = object : Runnable {
        override fun run() {
            widget.refresh()
            val now = System.currentTimeMillis()
            if (now - lastFull > 59_000 || (now % 60_000 < 1_500 && now - lastFull > 5_000)) { Eink.fullRefresh(window.decorView); lastFull = now }
            handler.postDelayed(this, minOf(60_000 - now % 60_000, widget.nextDelayMs(now)).coerceAtLeast(250))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = intent.getStringExtra(EXTRA_TYPE)
        val w = type?.let { WidgetRegistry.get(it) } ?: return finish()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        widget = w.createView(this).apply { this.type = type; expand.visibility = View.GONE }
        val pad = Ui.dp(this, 16f)
        setContentView(FrameLayout(this).apply {
            fitsSystemWindows = true
            setBackgroundColor(Ui.PAPER)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad, pad, pad)
                addView(widget, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
                addView(Ui.rule(context))
                addView(Ui.gap(context, 8f))
                addView(Ui.button(context, R.string.close) { finish() })
            })
        })
    }

    override fun onResume() {
        super.onResume()
        if (::widget.isInitialized) { handler.removeCallbacks(tick); tick.run() }
    }

    override fun retick() {
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    companion object {
        const val EXTRA_TYPE = "type"
    }
}
