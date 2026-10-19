package com.arashrasoulzadeh.einkdash.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import com.arashrasoulzadeh.einkdash.settings.AppPrefs
import java.util.Locale

/** Applies the in-app language choice (Persian/English) and disables transition animations. */
open class BaseActivity : Activity() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(wrap(base))
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceControls.applyOrientation(this)
        // Show above Android's swipe lock screen, so waking the device returns straight to the dashboard
        // (the app's own PIN lock below still guards the data).
        if (android.os.Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        @Suppress("DEPRECATION")
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
    }

    override fun onResume() {
        super.onResume()
        com.arashrasoulzadeh.einkdash.lock.OffScreen.front = java.lang.ref.WeakReference(this)
        // Every screen sits behind the privacy lock except the lock itself and the "off" page.
        if (this !is com.arashrasoulzadeh.einkdash.lock.LockActivity && this !is com.arashrasoulzadeh.einkdash.lock.OffActivity &&
            com.arashrasoulzadeh.einkdash.lock.AppLock.isLocked(this)) com.arashrasoulzadeh.einkdash.lock.LockActivity.show(this)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val LARGE_PRINT_SCALE = 1.35f

        fun wrap(base: Context): Context {
            val lang = AppPrefs(base).language
            val night = com.arashrasoulzadeh.einkdash.face.Night.isNight(base)
            val large = AppPrefs(base).largePrint
            if (lang == "system" && !night && !large) return base
            val config = Configuration(base.resources.configuration)
            if (lang != "system") {
                val locale = Locale(lang)
                Locale.setDefault(locale)
                config.setLocale(locale)
                config.setLayoutDirection(locale)
            }
            // Night theme: larger text, readable from across the room.
            if (night) config.fontScale = config.fontScale * com.arashrasoulzadeh.einkdash.face.Night.FONT_SCALE
            if (large) config.fontScale = config.fontScale * LARGE_PRINT_SCALE
            return base.createConfigurationContext(config)
        }
    }
}
