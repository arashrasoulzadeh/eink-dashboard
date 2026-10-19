package com.arashrasoulzadeh.einkdash

import android.app.Application
import com.arashrasoulzadeh.einkdash.util.Ui

class App : Application() {
    // The un-flagged registerReceiver below only runs before Android 13, where the flag doesn't exist.
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        Ui.init(this)
        com.arashrasoulzadeh.einkdash.notes.NoteServer.sync(this)
        // Screen off locks the dashboard; shutdown draws the "device is off" page.
        // Registered here as well as in the manifest: newer Android drops manifest receivers for these.
        registerReceiver(object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context, i: android.content.Intent) { com.arashrasoulzadeh.einkdash.lock.AppLock.lock() }
        }, android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF))
        val off = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_SHUTDOWN)
            addAction("android.intent.action.QUICKBOOT_POWEROFF") // some vendors' fast power-off
        }
        // The vendor power-off broadcast comes from outside the app, so it must be exported on 13+.
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(com.arashrasoulzadeh.einkdash.lock.OffScreen.Receiver(), off, RECEIVER_EXPORTED)
        else registerReceiver(com.arashrasoulzadeh.einkdash.lock.OffScreen.Receiver(), off)
    }
}
