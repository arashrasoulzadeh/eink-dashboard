package com.arashrasoulzadeh.einkdash.notes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** [due] is yyyyMMdd (Gregorian) or 0 for no date. */
data class Note(val id: Long, val text: String, val done: Boolean, val due: Int = 0)

class NotesDb private constructor(context: Context) : SQLiteOpenHelper(context.applicationContext, "notes.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0, due INTEGER NOT NULL DEFAULT 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE notes ADD COLUMN due INTEGER NOT NULL DEFAULT 0")
    }

    /**
     * Open items first. Among them, anything due today or overdue leads, then upcoming by date,
     * then undated newest first. Done items last.
     */
    fun all(): List<Note> {
        val notes = readableDatabase.rawQuery("SELECT id, text, done, due FROM notes", null).use { c ->
            List(c.count) { c.moveToNext(); Note(c.getLong(0), c.getString(1), c.getInt(2) == 1, c.getInt(3)) }
        }
        val today = today()
        return notes.sortedWith(compareBy<Note>(
            { it.done },
            { if (it.due != 0 && it.due <= today) 0 else if (it.due != 0) 1 else 2 },
            { if (it.due != 0) it.due else 0 },
            { -it.id },
        ))
    }

    fun add(text: String, due: Int = 0): Long =
        writableDatabase.insert("notes", null, ContentValues().apply { put("text", text); put("due", due) })

    fun setDone(id: Long, done: Boolean) {
        writableDatabase.update("notes", ContentValues().apply { put("done", if (done) 1 else 0) }, "id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("notes", "id = ?", arrayOf(id.toString()))
    }

    /** Gregorian yyyyMMdd days that have open todos due, for marking the calendar. */
    fun dueDays(): Set<Int> = all().filter { !it.done && it.due != 0 }.map { it.due }.toSet()

    companion object {
        @Volatile private var shared: NotesDb? = null

        /** One open helper for the whole app; opening one per refresh leaked connections. */
        fun of(ctx: Context): NotesDb = shared ?: synchronized(this) { shared ?: NotesDb(ctx.applicationContext).also { shared = it } }

        fun ymd(c: Calendar) = c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
        fun today() = ymd(Calendar.getInstance())
        fun tomorrow() = ymd(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) })

        /** Short human label for a due date: today, tomorrow, overdue, or the date. */
        fun dueLabel(ctx: Context, due: Int): String {
            val r = ctx.resources
            return when {
                due == 0 -> ""
                due < today() -> r.getString(com.arashrasoulzadeh.einkdash.R.string.due_overdue)
                due == today() -> r.getString(com.arashrasoulzadeh.einkdash.R.string.due_today)
                due == tomorrow() -> r.getString(com.arashrasoulzadeh.einkdash.R.string.due_tomorrow)
                else -> {
                    val c = Calendar.getInstance().apply { set(due / 10000, due / 100 % 100 - 1, due % 100) }
                    if (com.arashrasoulzadeh.einkdash.util.Fa.isFa(ctx)) {
                        val j = com.arashrasoulzadeh.einkdash.util.Jalali.fromGregorian(due / 10000, due / 100 % 100, due % 100)
                        com.arashrasoulzadeh.einkdash.util.Fa.digits(ctx, "${j.day} ${com.arashrasoulzadeh.einkdash.util.Jalali.monthsFa[j.month - 1]}")
                    } else SimpleDateFormat("d MMM", Locale.ENGLISH).format(c.time)
                }
            }
        }
    }
}
