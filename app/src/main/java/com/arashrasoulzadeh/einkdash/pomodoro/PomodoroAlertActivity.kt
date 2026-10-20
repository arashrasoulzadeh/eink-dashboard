package com.arashrasoulzadeh.einkdash.pomodoro

import android.content.Context
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.util.BaseActivity
import com.arashrasoulzadeh.einkdash.util.Ui

/**
 * Full-screen notice when a focus or break session ends. It flashes black/white until a button is
 * tapped, so it's noticed from across the room on readers with no speaker or vibration motor.
 */
class PomodoroAlertActivity : BaseActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: LinearLayout
    private lateinit var title: TextView
    private var flashes = 0
    private var dark = true

    /** About once a second for up to two minutes, then it rests on black to spare the panel and battery. */
    private val flash = object : Runnable {
        override fun run() {
            dark = !dark
            paint()
            Eink.fullRefresh(root)
            if (++flashes < MAX_FLASHES) handler.postDelayed(this, 1000)
            else { dark = true; paint() }
        }
    }

    private fun paint() {
        root.setBackgroundColor(if (dark) Ui.INK else Ui.PAPER)
        title.setTextColor(if (dark) Ui.PAPER else Ui.INK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val wasWork = intent.getStringExtra("phase") != Pomodoro.BREAK
        val pad = (24 * resources.displayMetrics.density).toInt()
        title = TextView(this).apply {
            setText(if (wasWork) R.string.pomo_work_done else R.string.pomo_break_done)
            textSize = 44f; typeface = Ui.light; gravity = Gravity.CENTER
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            addView(title)
            addView(Ui.gap(context, 40f))
            addView(Ui.button(context, if (wasWork) R.string.pomo_start_break else R.string.pomo_start_work) {
                Pomodoro.start(context, if (wasWork) Pomodoro.BREAK else Pomodoro.WORK); finish()
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(Ui.gap(context, 12f))
            addView(Ui.button(context, R.string.pomo_dismiss) { finish() },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        paint()
        setContentView(root)

        (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.takeIf { it.hasVibrator() }?.let {
            @Suppress("DEPRECATION") it.vibrate(800)
        }
        runCatching {
            RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(flash)
        if (flashes < MAX_FLASHES) handler.postDelayed(flash, 1000)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(flash)
    }

    companion object {
        private const val MAX_FLASHES = 120
    }
}
