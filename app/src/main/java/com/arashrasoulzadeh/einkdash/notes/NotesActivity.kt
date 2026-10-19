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
import com.arashrasoulzadeh.einkdash.util.Fa

/** Checklist editor. Long-press an item to delete it. */
class NotesActivity : BaseActivity() {

    private lateinit var db: NotesDb
    private lateinit var list: LinearLayout
    private var due = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = NotesDb.of(this)
        val input = Ui.input(this, R.string.notes_hint).apply { setSingleLine() }
        lateinit var dateBtn: android.widget.Button
        fun showDue() { dateBtn.text = if (due == 0) getString(R.string.due_none) else NotesDb.dueLabel(this, due) }
        // No date → today → tomorrow → pick a day.
        dateBtn = Ui.button(this, "") {
            when (due) {
                0 -> { due = NotesDb.today(); showDue() }
                NotesDb.today() -> { due = NotesDb.tomorrow(); showDue() }
                NotesDb.tomorrow() -> {
                    val c = java.util.Calendar.getInstance()
                    android.app.DatePickerDialog(this, { _, y, m, d -> due = y * 10000 + (m + 1) * 100 + d; showDue() },
                        c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).apply {
                        setOnCancelListener { due = 0; showDue() }
                    }.show()
                }
                else -> { due = 0; showDue() }
            }
        }
        showDue()
        val add = Ui.button(this, R.string.edit_add) {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) { db.add(text, due); input.setText(""); due = 0; showDue(); render() }
        }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val root = Ui.screen(this, R.string.widget_notes)
        root.addView(LinearLayout(this).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(add, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = Ui.dp(context, 8f) })
        })
        root.addView(Ui.gap(this, 8f))
        root.addView(dateBtn)
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
                text = if (note.due == 0) note.text else note.text + Fa.sep(context) + NotesDb.dueLabel(context, note.due)
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
