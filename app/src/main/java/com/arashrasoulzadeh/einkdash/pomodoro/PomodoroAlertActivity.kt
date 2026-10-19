package com.arashrasoulzadeh.einkdash.pomodoro

import android.app.Activity
import com.arashrasoulzadeh.einkdash.util.BaseActivity
import android.content.Context
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Vibrator
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.util.Ui

/** Full-screen inverted notice: works on e-readers with no speaker or vibration motor. */
class PomodoroAlertActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val wasWork = intent.getStringExtra("phase") != Pomodoro.BREAK
        val white = 0xFFFFFFFF.toInt()
        val pad = (24 * resources.displayMetrics.density).toInt()

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF000000.toInt())
            setPadding(pad, pad, pad, pad)
            addView(TextView(context).apply {
                setText(if (wasWork) R.string.pomo_work_done else R.string.pomo_break_done)
                textSize = 44f; typeface = Ui.light; setTextColor(white); gravity = Gravity.CENTER
            })
            addView(Ui.gap(context, 40f))
            addView(Ui.button(context, if (wasWork) R.string.pomo_start_break else R.string.pomo_start_work) {
                Pomodoro.start(context, if (wasWork) Pomodoro.BREAK else Pomodoro.WORK); finish()
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(Ui.gap(context, 12f))
            addView(Ui.button(context, R.string.pomo_dismiss) { finish() },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        })

        (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.takeIf { it.hasVibrator() }?.let {
            @Suppress("DEPRECATION") it.vibrate(800)
        }
        runCatching {
            RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
        }
    }
}
