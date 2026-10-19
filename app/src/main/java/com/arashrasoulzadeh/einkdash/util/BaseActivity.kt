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

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        fun wrap(base: Context): Context {
            val lang = AppPrefs(base).language
            val night = com.arashrasoulzadeh.einkdash.face.Night.isNight(base)
            if (lang == "system" && !night) return base
            val config = Configuration(base.resources.configuration)
            if (lang != "system") {
                val locale = Locale(lang)
                Locale.setDefault(locale)
                config.setLocale(locale)
                config.setLayoutDirection(locale)
            }
            // Night theme: larger text, readable from across the room.
            if (night) config.fontScale = config.fontScale * com.arashrasoulzadeh.einkdash.face.Night.FONT_SCALE
            return base.createConfigurationContext(config)
        }
    }
}
