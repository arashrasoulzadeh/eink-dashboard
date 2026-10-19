package com.arashrasoulzadeh.einkdash.launcher

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.util.Ui

/** Paged app grid; no scrolling, since scrolling ghosts on e-ink. */
class AppDrawerActivity : Activity() {

    private val cols = 4
    private val rows = 5
    private lateinit var apps: List<ResolveInfo>
    private lateinit var grid: LinearLayout
    private lateinit var pageLabel: TextView
    private var page = 0

    private val pageCount get() = maxOf(1, (apps.size + cols * rows - 1) / (cols * rows))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pm = packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        // Nested LinearLayouts rather than GridLayout: weighted GridLayout specs need API 21.
        grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        pageLabel = Ui.text(this, 16f, Ui.bold).apply { gravity = Gravity.CENTER }

        val root = Ui.screen(this, R.string.apps)
        root.addView(grid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(Ui.rule(this))
        root.addView(Ui.gap(this, 6f))
        root.addView(Ui.row(this, Ui.button(this, R.string.prev) { flip(-1) }, pageLabel, Ui.button(this, R.string.next) { flip(1) }))
        render()
    }

    private fun flip(delta: Int) {
        val next = (page + delta).coerceIn(0, pageCount - 1)
        if (next != page) { page = next; render() }
    }

    private fun render() {
        grid.removeAllViews()
        val pm = packageManager
        val perPage = cols * rows
        val pageApps = apps.drop(page * perPage).take(perPage)
        for (r in 0 until rows) {
            val row = LinearLayout(this)
            for (c in 0 until cols) {
                val app = pageApps.getOrNull(r * cols + c)
                val cell = TextView(this).apply {
                    gravity = Gravity.CENTER
                    if (app != null) {
                        text = app.loadLabel(pm)
                        textSize = 14f
                        setTextColor(Ui.INK)
                        maxLines = 2
                        compoundDrawablePadding = Ui.dp(context, 6f)
                        setCompoundDrawables(null, app.loadIcon(pm).toGrayscale(Ui.dp(context, 48f)), null, null)
                        setOnClickListener { launch(app) }
                    }
                }
                row.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            }
            grid.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        pageLabel.text = "${page + 1}/$pageCount"
        Eink.fullRefresh(window.decorView)
    }

    private fun launch(app: ResolveInfo) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.activityInfo.packageName, app.activityInfo.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    // Page-turn hardware keys flip drawer pages.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> { flip(1); true }
        KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_DPAD_LEFT -> { flip(-1); true }
        else -> super.onKeyDown(keyCode, event)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}

/** Uniform-size grayscale icon; color icons are muddy on e-ink anyway. */
private fun android.graphics.drawable.Drawable.toGrayscale(size: Int): android.graphics.drawable.Drawable =
    mutate().apply {
        colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0f) })
        setBounds(0, 0, size, size)
    }
