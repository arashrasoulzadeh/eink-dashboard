package com.arashrasoulzadeh.einkdash.notes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Note(val id: Long, val text: String, val done: Boolean)

class NotesDb(context: Context) : SQLiteOpenHelper(context.applicationContext, "notes.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    /** Open items first, then done ones; newest first within each. */
    fun all(): List<Note> = readableDatabase.rawQuery("SELECT id, text, done FROM notes ORDER BY done, id DESC", null).use { c ->
        List(c.count) { c.moveToNext(); Note(c.getLong(0), c.getString(1), c.getInt(2) == 1) }
    }

    fun add(text: String) {
        writableDatabase.insert("notes", null, ContentValues().apply { put("text", text) })
    }

    fun setDone(id: Long, done: Boolean) {
        writableDatabase.update("notes", ContentValues().apply { put("done", if (done) 1 else 0) }, "id = ?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("notes", "id = ?", arrayOf(id.toString()))
    }
}
