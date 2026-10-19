package com.arashrasoulzadeh.einkdash.latency

import android.app.Activity
import com.arashrasoulzadeh.einkdash.util.BaseActivity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.util.Ui

/** One URL per line. */
class LatencySettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LatencyStore(this)
        val input = Ui.input(this, R.string.latency_hint).apply {
            setText(store.urls.joinToString("\n"))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_VARIATION_URI
            gravity = Gravity.TOP or Gravity.START
            minLines = 5
            textSize = 16f
        }
        val root = Ui.screen(this, R.string.widget_latency)
        root.addView(Ui.text(this, 14f).apply { setText(R.string.latency_help) })
        root.addView(Ui.gap(this, 12f))
        root.addView(input)
        root.addView(Ui.gap(this, 12f))
        root.addView(Ui.button(this, R.string.save) { store.urls = input.text.toString().lines(); finish() })
    }
}
