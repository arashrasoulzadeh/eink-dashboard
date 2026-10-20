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
            Eink.fullRefresh(window.decorView)
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
        ring.progress = 1f - left.toFloat() / total
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
            setBackgroundColor(Ui.PAPER)
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
                weight(FitText(this, Ui.light).apply { text = time(); gravity = Gravity.CENTER }, 1.2f)
                body.addView(Ui.text(this, 18f, Ui.bold).apply { text = dates(); gravity = Gravity.CENTER })
                weatherLine()?.let { body.addView(Ui.gap(this, 12f)); body.addView(Ui.text(this, 20f).apply { text = it; gravity = Gravity.CENTER }) }
                val pomo = pomodoro()
                couplet()
                body.addView(Ui.gap(this, 16f)); body.addView(Ui.rule(this)); body.addView(Ui.gap(this, 12f))
                body.addView(Ui.label(this, R.string.widget_notes))
                notes(if (pomo) 3 else 4)
                body.addView(Ui.gap(this, 1f), LinearLayout.LayoutParams(1, 0, 0.3f))
                body.addView(Ui.text(this, 13f).apply { setText(R.string.face_tap); gravity = Gravity.CENTER })
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
        val ring = com.arashrasoulzadeh.einkdash.util.Ring(this).apply { unit = getString(R.string.pomo_unit_min_sec) }
        setRing(ring, left)
        pomoRing = ring
        body.addView(Ui.gap(this, 16f))
        body.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(ring, LinearLayout.LayoutParams(Ui.dp(context, 110f), Ui.dp(context, 110f)))
            addView(Ui.text(context, 20f, Ui.bold).apply {
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
            body.addView(Ui.text(this, 19f).apply {
                text = line; gravity = Gravity.CENTER; textDirection = android.view.View.TEXT_DIRECTION_RTL
                setPadding(0, Ui.dp(context, 2f), 0, Ui.dp(context, 2f))
            })
        }
        body.addView(Ui.text(this, 13f, Ui.bold).apply { text = c.poet; gravity = Gravity.CENTER; setPadding(0, Ui.dp(context, 4f), 0, 0) })
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

        fun show(ctx: Context, mode: String) = ctx.startActivity(
            Intent(ctx, FaceActivity::class.java).putExtra(EXTRA_MODE, mode).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }
}
