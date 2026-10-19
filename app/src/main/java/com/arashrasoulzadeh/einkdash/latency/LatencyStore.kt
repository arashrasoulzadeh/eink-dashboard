package com.arashrasoulzadeh.einkdash.latency

import android.content.Context
import com.arashrasoulzadeh.einkdash.util.Http
import org.json.JSONArray
import org.json.JSONObject

/** Endpoints to probe and their last 10 results (ms, or -1 when down). */
class LatencyStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("latency", Context.MODE_PRIVATE)

    var urls: List<String>
        get() = prefs.getString("urls", "")!!.lines().map { it.trim() }.filter { it.isNotEmpty() }
        set(value) { prefs.edit().putString("urls", value.joinToString("\n")).putLong("ranAt", 0).apply() }

    val ranAt get() = prefs.getLong("ranAt", 0)

    fun isStale(now: Long) = now - ranAt > 5 * 60_000L

    fun history(url: String): List<Int> {
        val all = runCatching { JSONObject(prefs.getString("history", "{}")!!) }.getOrDefault(JSONObject())
        val arr = all.optJSONArray(url) ?: return emptyList()
        return List(arr.length()) { arr.getInt(it) }
    }

    /** Blocking; call from [com.arashrasoulzadeh.einkdash.util.Bg]. Up = any response below 500. */
    fun probeAll() {
        prefs.edit().putLong("ranAt", System.currentTimeMillis()).apply()
        val all = runCatching { JSONObject(prefs.getString("history", "{}")!!) }.getOrDefault(JSONObject())
        val next = JSONObject()
        urls.forEach { url ->
            val ms = probe(url)
            val prev = all.optJSONArray(url) ?: JSONArray()
            val kept = (0 until prev.length()).map { prev.getInt(it) }.takeLast(9) + ms
            next.put(url, JSONArray(kept))
        }
        prefs.edit().putString("history", next.toString()).apply()
    }

    private fun probe(url: String): Int = try {
        val start = System.nanoTime()
        val r = Http.get(if ("://" in url) url else "https://$url", timeoutMs = 8_000)
        val ms = ((System.nanoTime() - start) / 1_000_000).toInt()
        if (r.code < 500) ms else -1
    } catch (_: Exception) {
        -1
    }
}
