package com.arashrasoulzadeh.einkdash.lock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.util.BaseActivity
import com.arashrasoulzadeh.einkdash.util.Fa
import com.arashrasoulzadeh.einkdash.util.FitText
import com.arashrasoulzadeh.einkdash.util.Jalali
import com.arashrasoulzadeh.einkdash.util.Ui
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Clock, date and a big keypad. Nothing private is drawn until the PIN is right. */
class LockActivity : BaseActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var time: FitText
    private lateinit var date: android.widget.TextView
    private lateinit var dots: android.widget.TextView
    private var entered = ""

    private val tick = object : Runnable {
        override fun run() {
            renderClock()
            Eink.fullRefresh(window.decorView)
            handler.postDelayed(this, 60_000 - System.currentTimeMillis() % 60_000 + 50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val pad = Ui.dp(this, 20f)
        time = FitText(this, Ui.light).apply { gravity = Gravity.CENTER }
        date = Ui.text(this, 18f, Ui.bold).apply { gravity = Gravity.CENTER }
        dots = Ui.text(this, 28f, Ui.bold).apply { gravity = Gravity.CENTER }
        val keys = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "✓")).forEach { row ->
            keys.addView(LinearLayout(this).apply {
                // Keypad stays left-to-right in Persian too, like a phone dialer.
                layoutDirection = LinearLayout.LAYOUT_DIRECTION_LTR
                row.forEachIndexed { i, k ->
                    addView(Ui.button(context, Fa.digits(context, k)) { press(k) }.apply { textSize = 26f },
                        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { if (i > 0) marginStart = Ui.dp(context, 8f) })
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = Ui.dp(this@LockActivity, 8f) })
        }
        setContentView(FrameLayout(this).apply {
            fitsSystemWindows = true
            setBackgroundColor(Ui.PAPER)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad, pad, pad)
                addView(time, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.1f))
                addView(date)
                addView(Ui.gap(context, 16f)); addView(Ui.rule(context)); addView(Ui.gap(context, 12f))
                addView(Ui.text(context, 14f).apply { setText(R.string.lock_enter_pin); gravity = Gravity.CENTER })
                addView(dots)
                addView(keys, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.6f))
            })
        })
        renderDots()
    }

    private fun press(k: String) {
        when (k) {
            "⌫" -> entered = entered.dropLast(1)
            "✓" -> submit()
            else -> if (entered.length < 8) { entered += k; if (entered.length >= 4 && AppLock.tryUnlock(this, entered)) return finish() }
        }
        renderDots()
        Eink.fullRefresh(window.decorView)
    }

    private fun submit() {
        if (AppLock.tryUnlock(this, entered)) return finish()
        entered = ""
        dots.setText(R.string.lock_wrong)
    }

    private fun renderDots() { dots.text = if (entered.isEmpty()) "–" else "● ".repeat(entered.length).trim() }

    private fun renderClock() {
        time.text = Fa.digits(this, SimpleDateFormat("HH:mm", Locale.US).format(Date()))
        val c = Calendar.getInstance()
        val j = Jalali.fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        val greg = SimpleDateFormat("EEEE d MMMM", resources.configuration.locale).format(Date())
        date.text = if (Fa.isFa(this)) Fa.digits(this, "${j.day} ${Jalali.monthsFa[j.month - 1]} ${j.year}") + Fa.sep(this) + greg
        else greg + Fa.sep(this) + "${j.day} ${Jalali.monthsEn[j.month - 1]} ${j.year}"
    }

    override fun onResume() { super.onResume(); handler.removeCallbacks(tick); tick.run() }
    override fun onPause() { super.onPause(); handler.removeCallbacks(tick) }

    // Back can't skip the lock.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event)

    companion object {
        fun show(ctx: Context) = ctx.startActivity(Intent(ctx, LockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION))
    }
}
