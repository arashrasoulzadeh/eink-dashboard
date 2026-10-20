package com.arashrasoulzadeh.einkdash.eink

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup

/** E-ink helpers. Uses the Onyx (Boox) SDK via reflection when present, else a generic full redraw. */
object Eink {
    val isOnyx: Boolean = Build.MANUFACTURER.equals("onyx", ignoreCase = true)

    /** Full refresh: the plan forbids ghosting, so every visible change goes through here. */
    fun fullRefresh(view: View) {
        if (isOnyx && onyxFullRefresh(view)) return
        view.invalidate()
    }

    /**
     * Clears ghosting on panels without a vendor refresh API (e.g. Fidibook): cover the window
     * in black, then white, then show the page. Driving every pixel through both extremes is what
     * the readers' own "full refresh" does. Used once when a long-lived screen opens.
     */
    fun clean(activity: Activity) {
        val decor = activity.window.decorView as? ViewGroup ?: return
        val cover = View(activity).apply { setBackgroundColor(0xFF000000.toInt()) }
        decor.addView(cover, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val h = Handler(Looper.getMainLooper())
        h.postDelayed({ cover.setBackgroundColor(0xFFFFFFFF.toInt()) }, 180)
        h.postDelayed({ decor.removeView(cover); fullRefresh(decor) }, 360)
    }

    private fun onyxFullRefresh(view: View): Boolean = try {
        val cls = Class.forName("android.onyx.ViewUpdateHelper")
        cls.getMethod("invalidate", View::class.java).invoke(null, view)
        true
    } catch (_: Throwable) {
        false
    }
}
