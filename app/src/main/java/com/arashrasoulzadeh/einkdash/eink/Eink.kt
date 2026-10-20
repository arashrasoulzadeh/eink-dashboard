package com.arashrasoulzadeh.einkdash.eink

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import java.lang.reflect.Method

/**
 * E-ink refresh control, using the panel's own API when the firmware has one:
 * - Rockchip readers (Fidibook and others): hidden public `View.requestEpdMode(View.EINK_MODE)`.
 *   EPD_PART = clean partial update (no flash), EPD_FULL = real full refresh (clears ghosting).
 *   Without it the firmware picks its own fast mode, which is what left ghosts.
 * - Onyx (Boox): `android.onyx.ViewUpdateHelper.invalidate`.
 * - Anything else: plain redraws, plus a black/white flash for [clean].
 */
object Eink {
    val isOnyx: Boolean = Build.MANUFACTURER.equals("onyx", ignoreCase = true)

    private val epdModeClass: Class<*>? = runCatching { Class.forName("android.view.View\$EINK_MODE") }.getOrNull()
    private val requestEpdMode: Method? = epdModeClass?.let { runCatching { View::class.java.getMethod("requestEpdMode", it) }.getOrNull() }

    /** True when the firmware exposes Rockchip's EPD modes. */
    val hasEpd: Boolean get() = requestEpdMode != null

    init { android.util.Log.i("Eink", "refresh API: " + when { requestEpdMode != null -> "rockchip EPD"; isOnyx -> "onyx"; else -> "none" }) }

    @Suppress("UNCHECKED_CAST")
    private fun epd(view: View, mode: String): Boolean = runCatching {
        val m = java.lang.Enum.valueOf(epdModeClass as Class<out Enum<*>>, mode)
        requestEpdMode!!.invoke(view, m)
        true
    }.onFailure { android.util.Log.w("Eink", "requestEpdMode($mode) failed", it) }.getOrDefault(false)

    /** Every visible change: a clean partial update where the panel supports choosing one. */
    fun fullRefresh(view: View) {
        if (isOnyx && onyxFullRefresh(view)) return
        if (hasEpd) epd(view, "EPD_PART")
        view.invalidate()
    }

    /** A real full refresh of just this view (e.g. a widget whose icon changed shape). */
    fun fullView(view: View) {
        if (hasEpd && epd(view, "EPD_FULL")) { view.invalidate(); return }
        fullRefresh(view)
    }

    /**
     * Clears ghosting across the whole window. With a vendor API that's the panel's full refresh;
     * otherwise cover the window in black, then white, then show the page, driving every pixel
     * through both extremes the way readers' own "full refresh" does.
     */
    fun clean(activity: Activity) {
        val decor = activity.window.decorView as? ViewGroup ?: return
        if (hasEpd && epd(decor, "EPD_FULL")) { decor.invalidate(); return }
        if (isOnyx && onyxFullRefresh(decor)) return
        val cover = View(activity).apply { setBackgroundColor(0xFF000000.toInt()) }
        decor.addView(cover, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val h = Handler(Looper.getMainLooper())
        h.postDelayed({ cover.setBackgroundColor(0xFFFFFFFF.toInt()) }, 180)
        h.postDelayed({ decor.removeView(cover); fullRefresh(decor) }, 360)
    }

    /** Sets a window's default update mode to clean partial, instead of the firmware's fast (ghosting) choice. */
    fun preferClean(activity: Activity) {
        if (hasEpd) epd(activity.window.decorView, "EPD_PART")
    }

    private fun onyxFullRefresh(view: View): Boolean = try {
        val cls = Class.forName("android.onyx.ViewUpdateHelper")
        cls.getMethod("invalidate", View::class.java).invoke(null, view)
        true
    } catch (_: Throwable) {
        false
    }
}
