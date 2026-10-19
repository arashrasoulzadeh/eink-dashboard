package com.arashrasoulzadeh.einkdash.launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.arashrasoulzadeh.einkdash.R

class HomeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        // Placeholder until the grid engine (phase 1) lands.
        root.addView(TextView(this).apply {
            setText(R.string.dashboard_placeholder)
            textSize = 32f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(Button(this).apply {
            setText(R.string.apps)
            textSize = 22f
            minHeight = 144
            setOnClickListener {
                startActivity(Intent(this@HomeActivity, AppDrawerActivity::class.java))
                overridePendingTransition(0, 0)
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        setContentView(root)
    }

    // Launchers must swallow Back on the home screen.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event)
}
