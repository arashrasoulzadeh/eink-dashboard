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
            if (lang == "system") return base
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            return base.createConfigurationContext(config)
        }
    }
}
