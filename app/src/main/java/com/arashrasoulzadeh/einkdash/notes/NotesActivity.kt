package com.arashrasoulzadeh.einkdash.notes

import android.app.Activity
import com.arashrasoulzadeh.einkdash.util.BaseActivity
import android.graphics.Paint
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.util.Ui

/** Checklist editor. Long-press an item to delete it. */
class NotesActivity : BaseActivity() {

    private lateinit var db: NotesDb
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = NotesDb(this)
        val input = Ui.input(this, R.string.notes_hint).apply { setSingleLine() }
        val add = Ui.button(this, R.string.edit_add) {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) { db.add(text); input.setText(""); render() }
        }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val root = Ui.screen(this, R.string.widget_notes)
        root.addView(LinearLayout(this).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(add, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = Ui.dp(context, 8f) })
        })
        root.addView(Ui.gap(this, 8f))
        root.addView(Ui.text(this, 13f).apply { setText(R.string.notes_delete_hint) })
        root.addView(Ui.gap(this, 8f))
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        render()
    }

    private fun render() {
        list.removeAllViews()
        db.all().forEach { note ->
            list.addView(CheckBox(this).apply {
                text = note.text
                textSize = 18f
                setTextColor(Ui.INK)
                typeface = Ui.regular
                isChecked = note.done
                minHeight = (48 * resources.displayMetrics.density).toInt()
                if (note.done) paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                setOnClickListener { db.setDone(note.id, isChecked); render() }
                setOnLongClickListener { db.delete(note.id); render(); true }
            })
            list.addView(Ui.rule(this))
        }
        Eink.fullRefresh(window.decorView)
    }
}
