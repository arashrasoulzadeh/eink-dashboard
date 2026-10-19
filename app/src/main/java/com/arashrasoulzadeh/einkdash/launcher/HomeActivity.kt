package com.arashrasoulzadeh.einkdash.launcher

import android.app.Activity
import com.arashrasoulzadeh.einkdash.util.BaseActivity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.dashboard.DashboardGrid
import com.arashrasoulzadeh.einkdash.dashboard.DashboardLayout
import com.arashrasoulzadeh.einkdash.dashboard.LayoutStore
import com.arashrasoulzadeh.einkdash.dashboard.Placement
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.util.Ui
import com.arashrasoulzadeh.einkdash.settings.AppPrefs
import com.arashrasoulzadeh.einkdash.settings.SettingsActivity
import android.view.MotionEvent
import com.arashrasoulzadeh.einkdash.widgets.WidgetRegistry

class HomeActivity : BaseActivity() {

    private lateinit var store: LayoutStore
    private lateinit var layout: DashboardLayout
    private lateinit var grid: DashboardGrid
    private lateinit var pageLabel: TextView
    private lateinit var status: TextView
    private lateinit var normalBar: LinearLayout
    private lateinit var editBar: LinearLayout
    private var pageIndex = 0
    private var editing = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastFull = 0L
    private lateinit var appPrefs: AppPrefs
    private var startLanguage = ""

    /** Always-on display: after a quiet spell the frontlight goes off and the sleep face takes over. */
    private val dimNow = Runnable {
        setBrightness(0f)
        if (appPrefs.sleepFace) com.arashrasoulzadeh.einkdash.face.FaceActivity.show(this, com.arashrasoulzadeh.einkdash.face.FaceActivity.SLEEP)
    }
    private var startNight = false

    /** Morning summary once a day, giant clock while charging at night. */
    private fun timeOfDay() {
        com.arashrasoulzadeh.einkdash.face.Night.dueFace(this)?.let { com.arashrasoulzadeh.einkdash.face.FaceActivity.show(this, it) }
    }

    private fun setBrightness(value: Float) {
        window.attributes = window.attributes.apply { screenBrightness = value }
    }

    private fun scheduleDim() {
        handler.removeCallbacks(dimNow)
        com.arashrasoulzadeh.einkdash.util.DeviceControls.applyBrightness(this)
        if (com.arashrasoulzadeh.einkdash.face.Night.isNight(this)) setBrightness(com.arashrasoulzadeh.einkdash.face.Night.BRIGHTNESS)
        val min = appPrefs.dimAfterMin
        var delay = if (min > 0) min * 60_000L else Long.MAX_VALUE
        // Battery saver: put the sleep face up just before Android turns the screen off,
        // so that is the image the e-ink panel holds while the device sleeps.
        if (!appPrefs.keepScreenOn && appPrefs.sleepFace) {
            delay = minOf(delay, (com.arashrasoulzadeh.einkdash.util.DeviceControls.screenTimeoutMs(this) - 8_000).coerceAtLeast(10_000))
        }
        if (delay != Long.MAX_VALUE) handler.postDelayed(dimNow, delay)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) scheduleDim()
        return super.dispatchTouchEvent(ev)
    }

    /** One ticker for every widget: aligned to the minute, faster only when a widget asks. */
    private val tick = object : Runnable {
        override fun run() {
            // Crossing into or out of the night window changes text size, which needs a fresh activity.
            if (com.arashrasoulzadeh.einkdash.face.Night.isNight(this@HomeActivity) != startNight) { recreate(); return }
            grid.refreshWidgets()
            updateStatus()
            timeOfDay()
            val now = System.currentTimeMillis()
            // Seconds-level ticks (running pomodoro) redraw partially; the anti-ghosting flash runs once a minute.
            if (now % 60_000 < 1_500 || now - lastFull > 60_000) { Eink.fullRefresh(window.decorView); lastFull = now }
            val delay = minOf(60_000 - now % 60_000, grid.nextDelayMs(now)).coerceAtLeast(250)
            handler.postDelayed(this, delay)
        }
    }

    private val orientation get() = resources.configuration.orientation
    private val page get() = layout.pages[pageIndex]
    private val selected get() = page.items.firstOrNull { it.id == grid.selectedId }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appPrefs = AppPrefs(this)
        startLanguage = appPrefs.language
        startNight = com.arashrasoulzadeh.einkdash.face.Night.isNight(this)
        store = LayoutStore(this)
        layout = store.load(orientation)

        grid = DashboardGrid(this).apply {
            onLongPress = { if (!editing) setEditing(true) }
            onCellTap = ::onCellTap
            onStackSwipe = { item, dir ->
                if (this@HomeActivity.page.tryUpdate(item.cycled(dir))) { rebuild(); save(); refresh() }
            }
        }
        pageLabel = Ui.text(this, 18f, Ui.bold).apply {
            gravity = Gravity.CENTER
            setOnClickListener { flip(if (pageIndex == this@HomeActivity.layout.pages.size - 1) -pageIndex else 1) }
        }
        status = Ui.text(this, 13f).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.END }

        // Slim footer: apps on the start side, page dots in the middle, device status on the end side.
        val apps = Ui.text(this, 15f, Ui.bold).apply {
            text = getString(R.string.apps).uppercase(resources.configuration.locale) + "  ▦"
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener {
                startActivity(Intent(this@HomeActivity, AppDrawerActivity::class.java))
                overridePendingTransition(0, 0)
            }
        }
        // One-tap language switch: shows the language you'd switch to.
        val lang = Ui.text(this, 15f, Ui.bold).apply {
            val fa = com.arashrasoulzadeh.einkdash.util.Fa.isFa(this@HomeActivity)
            text = if (fa) "EN" else "فا"
            gravity = Gravity.CENTER
            setOnClickListener {
                appPrefs.language = if (fa) "en" else "fa"
                recreate()
            }
        }
        val rotate = com.arashrasoulzadeh.einkdash.util.RotateIcon(this).apply {
            setOnClickListener { com.arashrasoulzadeh.einkdash.util.DeviceControls.rotate(this@HomeActivity) }
        }
        normalBar = LinearLayout(this).apply {
            val h = Ui.dp(context, 48f)
            addView(apps, LinearLayout.LayoutParams(0, h, 1f))
            addView(lang, LinearLayout.LayoutParams(Ui.dp(context, 48f), h))
            addView(rotate, LinearLayout.LayoutParams(Ui.dp(context, 48f), h))
            addView(pageLabel, LinearLayout.LayoutParams(0, h, 0.5f))
            addView(status, LinearLayout.LayoutParams(0, h, 1.5f))
        }
        editBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(bar(
                button(R.string.edit_add) { addWidget() },
                button(R.string.edit_remove) { removeSelected() },
                button(R.string.edit_setup) { setupSelected() },
                button(R.string.edit_page) { pageMenu() },
            ))
            addView(bar(
                button(R.string.edit_wider) { resize(1, 0) },
                button(R.string.edit_narrower) { resize(-1, 0) },
                button(R.string.edit_taller) { resize(0, 1) },
                button(R.string.edit_shorter) { resize(0, -1) },
                button(R.string.edit_done) { setEditing(false) },
            ))
            visibility = View.GONE
        }

        val pad = Ui.dp(this, 12f)
        // fitsSystemWindows on the outer frame keeps bars clear of system bars on edge-to-edge Android 15+.
        setContentView(android.widget.FrameLayout(this).apply {
            fitsSystemWindows = true
            setBackgroundColor(Ui.PAPER)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad, pad, pad)
                addView(grid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
                addView(Ui.rule(context))
                addView(normalBar)
                addView(editBar)
            })
        })
        showPage()
    }

    override fun onResume() {
        super.onResume()
        if (appPrefs.language != startLanguage || com.arashrasoulzadeh.einkdash.face.Night.isNight(this) != startNight) { recreate(); return }
        com.arashrasoulzadeh.einkdash.notes.NoteServer.onNote = { grid.refreshWidgets(); Eink.fullRefresh(window.decorView) }
        // A restored backup may have replaced the saved layout.
        layout = store.load(orientation)
        pageIndex = pageIndex.coerceAtMost(layout.pages.size - 1)
        grid.page = page
        if (appPrefs.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        scheduleDim()
        // Settings screens may have changed widget data; rebuild and restart the ticker.
        grid.rebuild()
        handler.removeCallbacks(tick)
        tick.run()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
        handler.removeCallbacks(dimNow)
    }

    private fun updateStatus() {
        val b = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = b?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = b?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100) ?: 100
        @Suppress("DEPRECATION")
        val net = (getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager).activeNetworkInfo
        @Suppress("DEPRECATION")
        val online = net?.isConnected == true
        val pct = if (level >= 0) com.arashrasoulzadeh.einkdash.util.Fa.digits(this, "${level * 100 / scale}%") else ""
        status.text = pct + "   " + (if (online) "◉ " else "○ ") + getString(if (online) R.string.status_online else R.string.status_offline)
    }

    private fun setupSelected() {
        val sel = selected ?: return toast(R.string.select_first)
        if (WidgetRegistry.get(sel.type)?.configure(this) != true) toast(R.string.nothing_to_setup)
    }

    private fun bar(vararg views: View) = LinearLayout(this).apply {
        val gap = Ui.dp(context, 6f)
        setPadding(0, gap, 0, 0)
        views.forEachIndexed { i, v ->
            addView(v, LinearLayout.LayoutParams(0, Ui.dp(context, 52f), 1f).apply { if (i > 0) marginStart = gap })
        }
    }

    private fun button(text: Int, onClick: () -> Unit) = Ui.button(this, text, onClick)

    private fun showPage() {
        grid.page = page
        pageLabel.text = if (layout.pages.size < 2) "" else (0 until layout.pages.size).joinToString(" ") { if (it == pageIndex) "●" else "○" }
        refresh()
    }

    private fun refresh() {
        grid.requestLayout()
        grid.invalidate()
        Eink.fullRefresh(window.decorView)
    }

    private fun save() = store.save(orientation, layout)

    private fun flip(delta: Int) {
        val next = (pageIndex + delta).coerceIn(0, layout.pages.size - 1)
        if (next != pageIndex) { pageIndex = next; grid.selectedId = null; showPage() }
    }

    private fun setEditing(on: Boolean) {
        editing = on
        grid.editMode = on
        grid.selectedId = null
        normalBar.visibility = if (on) View.GONE else View.VISIBLE
        editBar.visibility = if (on) View.VISIBLE else View.GONE
        if (!on) save()
        refresh()
    }

    /** Tap a widget to select it; tap an empty cell to move the selection there. */
    private fun onCellTap(col: Int, row: Int) {
        val hit = page.at(col, row)
        val sel = selected
        when {
            hit != null -> grid.selectedId = if (hit.id == sel?.id) null else hit.id
            // Clamp so a wide/tall widget tapped near the edge still lands on the grid.
            sel != null -> if (page.tryUpdate(sel.copy(
                col = col.coerceAtMost(page.cols - sel.w), row = row.coerceAtMost(page.rows - sel.h)))) {
                grid.rebuild(); save()
            } else toast(R.string.no_space)
        }
        refresh()
    }

    private fun resize(dw: Int, dh: Int) {
        val sel = selected ?: return toast(R.string.select_first)
        if (page.tryUpdate(sel.copy(w = sel.w + dw, h = sel.h + dh))) { grid.rebuild(); save(); refresh() }
        else toast(R.string.no_space)
    }

    private fun removeSelected() {
        val sel = selected ?: return toast(R.string.select_first)
        // Removes only the showing widget of a stack; the spot goes when its last widget does.
        val rest = sel.popped()
        if (rest != null) { page.tryUpdate(rest); grid.rebuild(); save(); refresh(); return }
        page.items.removeAll { it.id == sel.id }
        grid.selectedId = null
        grid.rebuild(); save(); refresh()
    }

    private fun addWidget() {
        val widgets = WidgetRegistry.all
        AlertDialog.Builder(this)
            .setItems(widgets.map { getString(it.titleRes) }.toTypedArray()) { _, i ->
                val w = widgets[i]
                selected?.let { sel ->
                    // With a spot selected, Add stacks onto it instead of finding a new spot.
                    page.tryUpdate(sel.pushed(w.type)); grid.rebuild(); save(); refresh()
                    return@setItems
                }
                val id = layout.nextId()
                val spot = page.firstFree(id, w.type, w.defaultW, w.defaultH) ?: page.firstFree(id, w.type, 1, 1)
                if (spot == null) toast(R.string.no_space)
                else { page.tryUpdate(spot); grid.selectedId = id; grid.rebuild(); save(); refresh() }
            }
            .show()
    }

    private fun pageMenu() {
        AlertDialog.Builder(this)
            .setItems(arrayOf(getString(R.string.page_add), getString(R.string.page_delete), getString(R.string.settings))) { _, i ->
                if (i == 2) {
                    setEditing(false)
                    return@setItems startActivity(Intent(this, SettingsActivity::class.java))
                }
                if (i == 0) {
                    layout.addPage(); pageIndex = layout.pages.size - 1
                } else if (layout.pages.size > 1) {
                    layout.pages.removeAt(pageIndex); pageIndex = pageIndex.coerceAtMost(layout.pages.size - 1)
                } else return@setItems toast(R.string.last_page)
                grid.selectedId = null
                save(); showPage()
            }
            .show()
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_SHORT).show()

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        // Launchers swallow Back; in edit mode Back means Done.
        KeyEvent.KEYCODE_BACK -> { if (editing) setEditing(false); true }
        KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN -> { flip(1); true }
        KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_VOLUME_UP -> { flip(-1); true }
        else -> super.onKeyDown(keyCode, event)
    }
}
