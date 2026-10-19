package com.arashrasoulzadeh.einkdash.launcher

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink

/** Paged app grid; no scrolling, since scrolling ghosts on e-ink. */
class AppDrawerActivity : Activity() {

    private val cols = 4
    private val rows = 5
    private lateinit var apps: List<ResolveInfo>
    private lateinit var grid: GridLayout
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

        grid = GridLayout(this).apply { columnCount = cols; rowCount = rows }
        pageLabel = TextView(this).apply { textSize = 20f; gravity = Gravity.CENTER }

        val nav = LinearLayout(this).apply {
            addView(navButton(R.string.prev) { flip(-1) }, LinearLayout.LayoutParams(0, 144, 1f))
            addView(pageLabel, LinearLayout.LayoutParams(0, 144, 1f))
            addView(navButton(R.string.next) { flip(1) }, LinearLayout.LayoutParams(0, 144, 1f))
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            addView(grid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(nav)
        })
        render()
    }

    private fun navButton(text: Int, onClick: () -> Unit) =
        Button(this).apply { setText(text); textSize = 24f; setOnClickListener { onClick() } }

    private fun flip(delta: Int) {
        val next = (page + delta).coerceIn(0, pageCount - 1)
        if (next != page) { page = next; render() }
    }

    private fun render() {
        grid.removeAllViews()
        val pm = packageManager
        val perPage = cols * rows
        apps.drop(page * perPage).take(perPage).forEachIndexed { i, app ->
            val cell = TextView(this).apply {
                text = app.loadLabel(pm)
                textSize = 16f
                gravity = Gravity.CENTER
                maxLines = 2
                setCompoundDrawablesWithIntrinsicBounds(null, app.loadIcon(pm).toGrayscale(), null, null)
                setOnClickListener { launch(app) }
            }
            grid.addView(cell, GridLayout.LayoutParams(
                GridLayout.spec(i / cols, 1f), GridLayout.spec(i % cols, 1f)
            ).apply { width = 0; height = 0 })
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

private fun android.graphics.drawable.Drawable.toGrayscale(): android.graphics.drawable.Drawable =
    mutate().apply {
        colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0f) })
        setBounds(0, 0, 96, 96)
    }
