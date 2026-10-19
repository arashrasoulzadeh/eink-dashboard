package com.arashrasoulzadeh.einkdash.settings

import android.content.Context
import com.arashrasoulzadeh.einkdash.notes.NotesDb
import org.json.JSONArray
import org.json.JSONObject

/**
 * Layouts, widget settings and notes as one JSON document, for moving a dashboard between devices.
 * Cached data (forecasts, latency history, running timers) is left out.
 */
object Backup {
    private const val VERSION = 1

    private val files = mapOf(
        "dashboard" to null,
        "app" to null,
        "weather" to setOf("cache", "fetchedAt", "attemptedAt"),
        "latency" to setOf("history", "ranAt"),
        "pomodoro" to setOf("endAt", "phase"),
        "reading" to null,
    )

    fun export(ctx: Context): String {
        val prefs = JSONObject()
        files.forEach { (name, skip) ->
            val o = JSONObject()
            ctx.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (k, v) ->
                if (skip?.contains(k) == true || v == null) return@forEach
                val type = when (v) { is Boolean -> "b"; is Int -> "i"; is Long -> "l"; is Float -> "f"; else -> "s" }
                o.put(k, JSONObject().put("t", type).put("v", if (type == "s") v.toString() else v))
            }
            prefs.put(name, o)
        }
        val notes = JSONArray()
        NotesDb.of(ctx).all().reversed().forEach { notes.put(JSONObject().put("text", it.text).put("done", it.done).put("due", it.due)) }
        return JSONObject().put("app", "einkdash").put("version", VERSION).put("prefs", prefs).put("notes", notes).toString(2)
    }

    /** Replaces current settings and notes. Throws on anything that isn't an Eink Dash backup. */
    fun import(ctx: Context, json: String) {
        val root = JSONObject(json)
        require(root.optString("app") == "einkdash") { "not an Eink Dash backup" }
        val prefs = root.getJSONObject("prefs")
        files.keys.forEach { name ->
            val o = prefs.optJSONObject(name) ?: return@forEach
            val e = ctx.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            o.keys().forEach { k ->
                val entry = o.getJSONObject(k)
                when (entry.getString("t")) {
                    "b" -> e.putBoolean(k, entry.getBoolean("v"))
                    "i" -> e.putInt(k, entry.getInt("v"))
                    "l" -> e.putLong(k, entry.getLong("v"))
                    "f" -> e.putFloat(k, entry.getDouble("v").toFloat())
                    else -> e.putString(k, entry.getString("v"))
                }
            }
            e.apply()
        }
        root.optJSONArray("notes")?.let { arr ->
            val db = NotesDb.of(ctx)
            db.all().forEach { db.delete(it.id) }
            for (i in 0 until arr.length()) {
                val n = arr.getJSONObject(i)
                val id = db.add(n.getString("text"), n.optInt("due"))
                if (n.optBoolean("done")) db.setDone(id, true)
            }
        }
    }
}
