package com.arashrasoulzadeh.einkdash.launcher

import android.app.Activity
import com.arashrasoulzadeh.einkdash.util.BaseActivity
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
class AppDrawerActivity : BaseActivity() {

    private val cols = 4
    private val rows = 5
    private lateinit var all: List<ResolveInfo>
    private var apps: List<ResolveInfo> = emptyList()
    private lateinit var grid: LinearLayout
    private lateinit var pageLabel: TextView
    private var page = 0

    private val pageCount get() = maxOf(1, (apps.size + cols * rows - 1) / (cols * rows))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pm = packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        all = pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        apps = all

        // Nested LinearLayouts rather than GridLayout: weighted GridLayout specs need API 21.
        grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        pageLabel = Ui.text(this, 16f).apply { gravity = Gravity.CENTER; setOnClickListener { flip(if (page == pageCount - 1) -page else 1) } }

        // Header like the mockup: back · title · search. Search filters by name (type on the reader or a keyboard).
        val search = Ui.input(this, R.string.apps_search).apply {
            visibility = View.GONE; setSingleLine()
            addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(e: android.text.Editable?) {
                    val q = e.toString().trim().lowercase()
                    apps = if (q.isEmpty()) all else all.filter { it.loadLabel(packageManager).toString().lowercase().contains(q) }
                    page = 0; render()
                }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        val pad = Ui.dp(this, 16f)
        val tap = Ui.dp(this, 44f)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, Ui.dp(context, 8f), pad, pad)
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(com.arashrasoulzadeh.einkdash.util.Icon(context, if (com.arashrasoulzadeh.einkdash.util.Fa.isFa(context)) com.arashrasoulzadeh.einkdash.util.Icon.Kind.NEXT else com.arashrasoulzadeh.einkdash.util.Icon.Kind.BACK).apply {
                    setOnClickListener { finish() } }, LinearLayout.LayoutParams(tap, tap))
                addView(Ui.text(context, 20f, Ui.bold).apply { setText(R.string.apps); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(com.arashrasoulzadeh.einkdash.util.Icon(context, com.arashrasoulzadeh.einkdash.util.Icon.Kind.SEARCH).apply {
                    setOnClickListener { search.visibility = if (search.visibility == View.VISIBLE) View.GONE else View.VISIBLE; if (search.visibility == View.VISIBLE) search.requestFocus() }
                }, LinearLayout.LayoutParams(tap, tap))
            })
            addView(View(context).apply { setBackgroundColor(Ui.LINE) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(context, 1f)).apply { bottomMargin = Ui.dp(context, 10f) })
            addView(search)
            addView(grid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(pageLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(context, 32f)))
        }
        setContentView(android.widget.FrameLayout(this).apply {
            fitsSystemWindows = true
            setBackgroundColor(Ui.PAPER)
            addView(root)
        })
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
                // Tile: the app's icon (grayscale) in a rounded outline, name underneath.
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    if (app != null) {
                        setOnClickListener { launch(app) }
                        addView(android.widget.ImageView(context).apply {
                            setImageDrawable(app.loadIcon(pm).toGrayscale(Ui.dp(context, 34f)))
                            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                            background = Ui.outline(context, 14f)
                            val p = Ui.dp(context, 10f); setPadding(p, p, p, p)
                        }, LinearLayout.LayoutParams(Ui.dp(context, 58f), Ui.dp(context, 58f)))
                        addView(Ui.text(context, 13f).apply {
                            // Always two lines tall, so tiles line up whether the name wraps or not.
                            text = app.loadLabel(pm); gravity = Gravity.CENTER or Gravity.TOP; setLines(2)
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            setPadding(0, Ui.dp(context, 5f), 0, 0)
                        })
                    }
                }
                row.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            }
            grid.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        pageLabel.text = if (pageCount < 2) "" else (0 until pageCount).joinToString("  ") { if (it == page) "●" else "○" }
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
}

/** Uniform-size grayscale icon; color icons are muddy on e-ink anyway. */
private fun android.graphics.drawable.Drawable.toGrayscale(size: Int): android.graphics.drawable.Drawable =
    mutate().apply {
        colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0f) })
        setBounds(0, 0, size, size)
    }
