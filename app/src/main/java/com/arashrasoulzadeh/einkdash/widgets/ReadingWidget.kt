package com.arashrasoulzadeh.einkdash.widgets

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import com.arashrasoulzadeh.einkdash.R

/**
 * Opens the user's reader app (Fidibo by default). Reader apps don't expose the
 * current book to other apps, so this shows the app, not the book.
 */
class ReadingWidget : DashboardWidget {
    override val type = "reading"
    override val titleRes = R.string.widget_reading
    override val defaultW = 2
    override val defaultH = 2

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("reading", Context.MODE_PRIVATE)

    private fun readerPackage(ctx: Context): String? {
        prefs(ctx).getString("pkg", null)?.let { if (launchIntent(ctx, it) != null) return it }
        return launcherApps(ctx).firstOrNull { "fidibo" in it.first.lowercase() }?.first
    }

    private fun launchIntent(ctx: Context, pkg: String) = ctx.packageManager.getLaunchIntentForPackage(pkg)

    private fun launcherApps(ctx: Context): List<Pair<String, String>> {
        val pm = ctx.packageManager
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
    }

    override fun createView(context: Context): WidgetView = object : WidgetView(context, titleRes) {
        val app = figure(26f)
        val hint = line(13f)

        init {
            setOnClickListener {
                val pkg = readerPackage(context)
                val intent = pkg?.let { launchIntent(context, it) }
                if (intent != null) context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                else if (context is Activity) configure(context)
            }
        }

        override fun refresh() {
            val pkg = readerPackage(context)
            if (pkg == null) {
                app.show(null)
                hint.setText(R.string.reading_pick)
            } else {
                val pm = context.packageManager
                app.show(runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)) }.getOrDefault(pkg))
                hint.setText(R.string.reading_tap)
            }
        }
    }

    override fun configure(activity: Activity): Boolean {
        val apps = launcherApps(activity)
        AlertDialog.Builder(activity)
            .setTitle(R.string.reading_pick)
            .setItems(apps.map { it.second }.toTypedArray()) { _, i ->
                prefs(activity).edit().putString("pkg", apps[i].first).apply()
            }
            .show()
        return true
    }
}
