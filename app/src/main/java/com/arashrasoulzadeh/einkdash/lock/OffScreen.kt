package com.arashrasoulzadeh.einkdash.lock

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.util.BaseActivity
import com.arashrasoulzadeh.einkdash.util.Fa
import com.arashrasoulzadeh.einkdash.util.Ui
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Device is off" page. E-ink keeps the last image without power, so drawing this during shutdown
 * stops a powered-off reader from looking like it is still on.
 */
object OffScreen {
    /** The activity currently in front; swapping its content is faster than starting a new one. */
    @Volatile var front: WeakReference<Activity>? = null

    fun build(activity: Activity): android.view.View {
        val pad = Ui.dp(activity, 32f)
        return FrameLayout(activity).apply {
            setBackgroundColor(Ui.PAPER)
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(pad, pad, pad, pad)
                addView(com.arashrasoulzadeh.einkdash.util.PowerIcon(activity), LinearLayout.LayoutParams(Ui.dp(activity, 96f), Ui.dp(activity, 96f)))
                addView(Ui.gap(activity, 16f))
                addView(Ui.text(activity, 34f, Ui.bold).apply { setText(R.string.off_title); gravity = Gravity.CENTER })
                addView(Ui.gap(activity, 12f))
                addView(Ui.text(activity, 16f).apply {
                    text = activity.getString(R.string.off_since,
                        Fa.digits(activity, SimpleDateFormat("HH:mm  yyyy/MM/dd", Locale.US).format(Date())))
                    gravity = Gravity.CENTER
                })
                addView(Ui.gap(activity, 8f))
                addView(Ui.text(activity, 14f).apply { setText(R.string.off_hint); gravity = Gravity.CENTER })
            }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
    }

    fun show(ctx: Context) {
        AppLock.lock()
        val a = front?.get()
        if (a != null && !a.isFinishing) {
            a.setContentView(build(a))
            Eink.fullRefresh(a.window.decorView)
        } else {
            ctx.startActivity(Intent(ctx, OffActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
        }
    }

    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SHUTDOWN || intent.action == "android.intent.action.QUICKBOOT_POWEROFF") show(context)
        }
    }
}

class OffActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(OffScreen.build(this))
    }
}
