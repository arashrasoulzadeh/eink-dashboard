package com.arashrasoulzadeh.einkdash.pomodoro

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/** Timer state in prefs plus an AlarmManager wake-up, so it survives the screen sleeping. */
object Pomodoro {
    const val WORK = "work"
    const val BREAK = "break"

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("pomodoro", Context.MODE_PRIVATE)

    fun endAt(ctx: Context) = prefs(ctx).getLong("endAt", 0)
    fun phase(ctx: Context) = prefs(ctx).getString("phase", WORK)!!
    fun workMin(ctx: Context) = prefs(ctx).getInt("workMin", 25)
    fun breakMin(ctx: Context) = prefs(ctx).getInt("breakMin", 5)
    fun setWorkMin(ctx: Context, m: Int) = prefs(ctx).edit().putInt("workMin", m).apply()

    /** Reading focus: open the reader app and silence notifications for focus sessions. */
    fun openReader(ctx: Context) = prefs(ctx).getBoolean("openReader", false)
    fun setOpenReader(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean("openReader", v).apply()
    fun dnd(ctx: Context) = prefs(ctx).getBoolean("dnd", false)
    fun setDnd(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean("dnd", v).apply()

    private fun setDndOn(ctx: Context, on: Boolean) {
        if (Build.VERSION.SDK_INT < 23 || !dnd(ctx)) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) return
        nm.setInterruptionFilter(if (on) android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY else android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    fun start(ctx: Context, phase: String) {
        val minutes = if (phase == WORK) workMin(ctx) else breakMin(ctx)
        val end = System.currentTimeMillis() + minutes * 60_000L
        prefs(ctx).edit().putLong("endAt", end).putString("phase", phase).apply()
        if (phase == WORK) {
            setDndOn(ctx, true)
            if (openReader(ctx)) com.arashrasoulzadeh.einkdash.widgets.ReadingWidget.launchReader(ctx)
        }
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmIntent(ctx)
        when {
            Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms() -> am.set(AlarmManager.RTC_WAKEUP, end, pi)
            Build.VERSION.SDK_INT >= 19 -> am.setExact(AlarmManager.RTC_WAKEUP, end, pi)
            else -> am.set(AlarmManager.RTC_WAKEUP, end, pi)
        }
    }

    fun stop(ctx: Context) {
        setDndOn(ctx, false)
        prefs(ctx).edit().putLong("endAt", 0).apply()
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(alarmIntent(ctx))
    }

    /** Fires the alert once when the timer has run out. Called by the alarm and by the widget tick. */
    fun checkFinished(ctx: Context) {
        val end = endAt(ctx)
        if (end == 0L || System.currentTimeMillis() < end - 1000) return
        val phase = phase(ctx)
        stop(ctx)
        // Finished focus sessions count as reading time when reading focus is on.
        if (phase == WORK && openReader(ctx)) com.arashrasoulzadeh.einkdash.reading.ReadingStats(ctx).add(workMin(ctx))
        ctx.startActivity(Intent(ctx, PomodoroAlertActivity::class.java)
            .putExtra("phase", phase)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun alarmIntent(ctx: Context): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(ctx, 0, Intent(ctx, Receiver::class.java), flags)
    }

    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = checkFinished(context)
    }
}
