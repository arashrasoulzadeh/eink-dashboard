package com.arashrasoulzadeh.einkdash.face

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.latency.LatencyStore
import com.arashrasoulzadeh.einkdash.notes.NotesDb
import com.arashrasoulzadeh.einkdash.util.BaseActivity
import com.arashrasoulzadeh.einkdash.util.Fa
import com.arashrasoulzadeh.einkdash.util.FitText
import com.arashrasoulzadeh.einkdash.util.Jalali
import com.arashrasoulzadeh.einkdash.util.Ui
import com.arashrasoulzadeh.einkdash.weather.WeatherStore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Full-screen "faces" that sit on the e-ink panel for a long time:
 * SLEEP (idle desk display), SUMMARY (morning briefing) and CLOCK (bedside, charging at night).
 * Redrawn once a minute; any tap returns to the dashboard.
 */
class FaceActivity : BaseActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mode: String
    private lateinit var body: LinearLayout

    private val tick: Runnable = object : Runnable {
        override fun run() {
            // The home screen is paused under a face, so faces check the schedule themselves.
            val due = Night.dueFace(this@FaceActivity)
            if (due != null && due != mode) { mode = due; applyBrightness() }
            else if (mode == CLOCK && due == null) return finish()
            com.arashrasoulzadeh.einkdash.pomodoro.Pomodoro.checkFinished(this@FaceActivity)
            render()
            // Full clean on entry (onWindowFocusChanged) and every ten minutes; other minutes redraw partially.
            val now = System.currentTimeMillis()
            if (cleaned && mode != SUMMARY && now - lastClean >= com.arashrasoulzadeh.einkdash.launcher.HomeActivity.CLEAN_EVERY_MS) {
                lastClean = now; Eink.clean(this@FaceActivity)
            } else Eink.fullRefresh(window.decorView)
            // Full redraw (and new couplet) on the clock's minute change; a running pomodoro ticks separately.
            handler.postDelayed(this, 60_000 - System.currentTimeMillis() % 60_000 + 50)
            handler.removeCallbacks(secondTick)
            if (pomoRing != null) handler.post(secondTick)
        }
    }

    private var pomoRing: com.arashrasoulzadeh.einkdash.util.Ring? = null

    /**
     * Seconds on the pomodoro ring, updated every 10 s: only the ring redraws (a partial e-ink update),
     * so the page doesn't flash; the full refresh stays once a minute.
     */
    private val secondTick: Runnable = object : Runnable {
        override fun run() {
            val ring = pomoRing ?: return
            val p = com.arashrasoulzadeh.einkdash.pomodoro.Pomodoro
            val end = p.endAt(this@FaceActivity)
            val left = end - System.currentTimeMillis()
            if (end == 0L || left <= 0) { p.checkFinished(this@FaceActivity); tick.run(); return }
            setRing(ring, left)
            // Every 10 seconds on the sleep face (on the countdown's 10-second marks): close enough, far fewer redraws.
            handler.postDelayed(this, (left % 10_000).let { if (it <= 0) 10_000 else it } + 5)
        }
    }

    private fun setRing(ring: com.arashrasoulzadeh.einkdash.util.Ring, left: Long) {
        val p = com.arashrasoulzadeh.einkdash.pomodoro.Pomodoro
        val total = (if (p.phase(this) == p.BREAK) p.breakMin(this) else p.sessionMin(this)) * 60_000L
        val secs = (left + 999) / 1000
        ring.progress = left.toFloat() / total // remaining, same as the home card
        ring.label = num("%d:%02d".format(secs / 60, secs % 60))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getStringExtra(EXTRA_MODE) ?: SLEEP
        // In battery saver the device may sleep under the sleep face; the clock face is only shown while charging.
        if (com.arashrasoulzadeh.einkdash.settings.AppPrefs(this).keepScreenOn || mode == CLOCK) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyBrightness()
        val pad = Ui.dp(this, 24f)
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        setContentView(FrameLayout(this).apply {
            fitsSystemWindows = true
            // The sleep face is a night page; the others stay paper-white.
            setBackgroundColor(if (mode == SLEEP) NIGHT else Ui.PAPER)
            addView(body)
        })
    }

    /** Summary is read in the morning with the light on; sleep and clock faces keep it off. */
    private fun applyBrightness() {
        window.attributes = window.attributes.apply {
            screenBrightness = if (mode == SUMMARY) android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else 0f
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_UP) finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(tick)
        tick.run()
    }

    private var cleaned = false
    private var lastClean = System.currentTimeMillis()

    /** Once the window is really on screen, flash it clean so the previous screen doesn't ghost through. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !cleaned) { cleaned = true; Eink.clean(this) }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
        handler.removeCallbacks(secondTick)
    }

    private fun num(s: String) = Fa.digits(this, s)

    private fun weight(v: android.view.View, w: Float) =
        body.addView(v, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, w))

    private fun dates(): String {
        val cal = Calendar.getInstance()
        val j = Jalali.fromGregorian(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        val greg = SimpleDateFormat("EEEE d MMMM yyyy", resources.configuration.locale).format(Date())
        val fa = Fa.isFa(this)
        val jal = "${j.day} ${(if (fa) Jalali.monthsFa else Jalali.monthsEn)[j.month - 1]} ${j.year}"
        return if (fa) num(jal) + "\n" + greg else greg + "\n" + jal
    }

    private fun time() = num(SimpleDateFormat("HH:mm", Locale.US).format(Date()))

    private fun weatherLine(): String? = WeatherStore(this).now()?.let {
        num("${it.temp}°") + "  " + getString(it.codeLabel) + "   " + num("${it.hi}° / ${it.lo}°")
    }

    private fun render() {
        body.removeAllViews()
        pomoRing = null
        when (mode) {
            CLOCK -> {
                weight(FitText(this, Ui.light).apply { text = time(); gravity = Gravity.CENTER }, 1f)
                body.addView(Ui.text(this, 18f).apply { text = dates().replace("\n", Fa.sep(this@FaceActivity)); gravity = Gravity.CENTER })
            }
            SLEEP -> {
                // Night page, top to bottom: status · moon · couplet · (pomodoro) · divider ·
                // weather and up to three todos · landscape · "sleep well".
                body.setPadding(0, 0, 0, 0)
                val side = Ui.dp(this, 28f)
                val soft = 0xFFB5B5B5.toInt()
                body.addView(LinearLayout(this).apply {
                    setPadding(Ui.dp(context, 20f), Ui.dp(context, 10f), Ui.dp(context, 20f), 0)
                    addView(Ui.text(context, 13f).apply { text = time(); setTextColor(soft) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    addView(Ui.text(context, 13f).apply { text = battery(); setTextColor(soft) })
                })
                body.addView(Ui.gap(this, 1f), LinearLayout.LayoutParams(1, 0, 0.5f))
                body.addView(LinearLayout(this).apply {
                    gravity = Gravity.CENTER
                    addView(com.arashrasoulzadeh.einkdash.util.Icon(context, com.arashrasoulzadeh.einkdash.util.Icon.Kind.MOON).apply { color = Ui.PAPER },
                        LinearLayout.LayoutParams(Ui.dp(context, 40f), Ui.dp(context, 40f)))
                })
                body.addView(Ui.gap(this, 8f))
                couplet()
                pomodoro()
                val todos = com.arashrasoulzadeh.einkdash.notes.NotesDb.of(this).all().filter { !it.done }.take(3)
                val weather = weatherLine()
                if (weather != null || todos.isNotEmpty()) {
                    body.addView(Ui.gap(this, 18f))
                    body.addView(LinearLayout(this).apply {
                        gravity = Gravity.CENTER
                        addView(android.view.View(context).apply { setBackgroundColor(0xFF3A3A3A.toInt()) }, LinearLayout.LayoutParams(Ui.dp(context, 120f), Ui.dp(context, 1f)))
                    })
                    body.addView(Ui.gap(this, 14f))
                }
                weather?.let { w -> body.addView(Ui.text(this, 16f).apply { text = w; gravity = Gravity.CENTER; setTextColor(Ui.PAPER) }) }
                if (todos.isNotEmpty()) body.addView(Ui.gap(this, 8f))
                todos.forEach { n ->
                    body.addView(Ui.text(this, 15f).apply {
                        text = "☐  " + n.text + if (n.due != 0) Fa.sep(context) + com.arashrasoulzadeh.einkdash.notes.NotesDb.dueLabel(context, n.due) else ""
                        setTextColor(soft); gravity = Gravity.CENTER; setSingleLine(); ellipsize = android.text.TextUtils.TruncateAt.END
                        setPadding(side, Ui.dp(context, 3f), side, Ui.dp(context, 3f))
                    })
                }
                body.addView(Ui.gap(this, 1f), LinearLayout.LayoutParams(1, 0, 0.5f))
                body.addView(com.arashrasoulzadeh.einkdash.util.NightScene(this), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.9f))
                body.addView(Ui.text(this, 13f).apply {
                    text = getString(R.string.sleep_well) + Fa.sep(context) + time()
                    gravity = Gravity.CENTER; setTextColor(soft); setBackgroundColor(0xFF151515.toInt())
                    setPadding(0, Ui.dp(context, 8f), 0, Ui.dp(context, 12f))
                })
            }
            else -> { // SUMMARY
                body.addView(Ui.text(this, 30f, Ui.light).apply { setText(R.string.summary_title) })
                body.addView(Ui.text(this, 18f, Ui.bold).apply { text = dates() })
                body.addView(Ui.gap(this, 16f)); body.addView(Ui.rule(this)); body.addView(Ui.gap(this, 12f))
                body.addView(Ui.label(this, R.string.widget_weather))
                body.addView(Ui.text(this, 26f, Ui.light).apply { text = weatherLine() ?: getString(R.string.weather_tap_setup) })
                body.addView(Ui.gap(this, 16f)); body.addView(Ui.rule(this)); body.addView(Ui.gap(this, 12f))
                body.addView(Ui.label(this, R.string.summary_todos))
                notes(3)
                body.addView(Ui.gap(this, 16f)); body.addView(Ui.rule(this)); body.addView(Ui.gap(this, 12f))
                body.addView(Ui.label(this, R.string.widget_latency))
                val store = LatencyStore(this)
                val down = store.urls.filter { store.history(it).lastOrNull()?.let { ms -> ms < 0 } == true }
                body.addView(Ui.text(this, 20f).apply {
                    text = when {
                        store.urls.isEmpty() -> getString(R.string.latency_tap_setup)
                        down.isEmpty() -> getString(R.string.summary_all_up)
                        else -> down.joinToString("\n") { "✕  " + it.substringAfter("://").substringBefore("/") }
                    }
                })
                body.addView(Ui.gap(this, 1f), LinearLayout.LayoutParams(1, 0, 1f))
                body.addView(Ui.text(this, 13f).apply { setText(R.string.face_tap); gravity = Gravity.CENTER })
            }
        }
    }

    /** A running pomodoro on the sleep face: ring with mm:ss left. Returns whether one is shown. */
    private fun pomodoro(): Boolean {
        val p = com.arashrasoulzadeh.einkdash.pomodoro.Pomodoro
        val end = p.endAt(this)
        val left = end - System.currentTimeMillis()
        if (end == 0L || left <= 0) return false
        val isBreak = p.phase(this) == p.BREAK
        val ring = com.arashrasoulzadeh.einkdash.util.Ring(this).apply { unit = getString(R.string.pomo_unit_min_sec); if (mode == SLEEP) inverted() }
        setRing(ring, left)
        pomoRing = ring
        body.addView(Ui.gap(this, 16f))
        body.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(ring, LinearLayout.LayoutParams(Ui.dp(context, 92f), Ui.dp(context, 92f)))
            addView(light(Ui.text(context, 18f, Ui.bold)).apply {
                text = getString(R.string.widget_pomodoro) + Fa.sep(context) + getString(if (isBreak) R.string.pomo_break else R.string.pomo_focus)
                setPadding(Ui.dp(context, 16f), 0, Ui.dp(context, 16f), 0)
            })
        })
        return true
    }

    /** A different couplet every minute, with the poet's name. */
    private fun couplet() {
        val c = Couplets.forMinute(this) ?: return
        body.addView(Ui.gap(this, 18f))
        listOf(c.first, c.second).forEach { line ->
            body.addView(light(Ui.text(this, 19f)).apply {
                text = line; gravity = Gravity.CENTER; textDirection = android.view.View.TEXT_DIRECTION_RTL
                setPadding(0, Ui.dp(context, 2f), 0, Ui.dp(context, 2f))
            })
        }
        body.addView(Ui.text(this, 13f).apply { text = "— " + c.poet; gravity = Gravity.CENTER; setTextColor(if (mode == SLEEP) 0xFFAAAAAA.toInt() else Ui.MUTED); setPadding(0, Ui.dp(context, 6f), 0, 0) })
    }

    /** Paper-colored text on the dark sleep page; unchanged elsewhere. */
    private fun light(t: android.widget.TextView) = t.apply { if (mode == SLEEP && currentTextColor == Ui.INK) setTextColor(Ui.PAPER) }

    private fun battery(): String {
        val b = registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = b?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = b?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (level < 0) "" else num("${level * 100 / scale}%")
    }

    private fun notes(max: Int) {
        val open = NotesDb.of(this).all().filter { !it.done }
        if (open.isEmpty()) body.addView(Ui.text(this, 18f).apply { setText(R.string.notes_none_short) })
        open.take(max).forEach { body.addView(Ui.text(this, 20f).apply { text = "☐  " + it.text; setPadding(0, Ui.dp(context, 4f), 0, 0) }) }
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val SLEEP = "sleep"
        const val SUMMARY = "summary"
        const val CLOCK = "clock"
        private const val NIGHT = 0xFF0E0E0E.toInt()

        fun show(ctx: Context, mode: String) = ctx.startActivity(
            Intent(ctx, FaceActivity::class.java).putExtra(EXTRA_MODE, mode).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }
}
