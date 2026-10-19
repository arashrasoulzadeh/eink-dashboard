package com.arashrasoulzadeh.einkdash

import android.app.Application
import com.arashrasoulzadeh.einkdash.util.Ui

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Ui.init(this)
    }
}
