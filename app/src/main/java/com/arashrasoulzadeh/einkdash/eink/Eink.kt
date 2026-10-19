package com.arashrasoulzadeh.einkdash.eink

import android.os.Build
import android.view.View

/** E-ink helpers. Uses the Onyx (Boox) SDK via reflection when present, else a generic full redraw. */
object Eink {
    val isOnyx: Boolean = Build.MANUFACTURER.equals("onyx", ignoreCase = true)

    /** Full refresh: the plan forbids ghosting, so every visible change goes through here. */
    fun fullRefresh(view: View) {
        if (isOnyx && onyxFullRefresh(view)) return
        view.invalidate()
    }

    private fun onyxFullRefresh(view: View): Boolean = try {
        val cls = Class.forName("android.onyx.ViewUpdateHelper")
        cls.getMethod("invalidate", View::class.java).invoke(null, view)
        true
    } catch (_: Throwable) {
        false
    }
}
