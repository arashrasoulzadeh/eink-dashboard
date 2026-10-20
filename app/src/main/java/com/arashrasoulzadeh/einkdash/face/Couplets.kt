package com.arashrasoulzadeh.einkdash.face

import android.content.Context
import org.json.JSONArray

/**
 * Offline couplet pool for the sleep face: one couplet from each of a few hundred poems by
 * Hafez, Saadi, Molavi, Khayyam, Eraqi and Baba Taher (fetched once from Ganjoor and bundled),
 * plus the Hafez ghazal openings. No network at runtime.
 */
object Couplets {
    class Couplet(val poet: String, val first: String, val second: String)

    @Volatile private var pool: List<Couplet>? = null

    fun all(ctx: Context): List<Couplet> = pool ?: synchronized(this) {
        pool ?: (read(ctx, "couplets.json") { Couplet(it.getString("p"), it.getString("a"), it.getString("b")) } +
            read(ctx, "hafez.json") { Couplet("حافظ", it.getString("a"), it.getString("b")) }).also { pool = it }
    }

    private fun read(ctx: Context, name: String, f: (org.json.JSONObject) -> Couplet): List<Couplet> = runCatching {
        val arr = JSONArray(ctx.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() })
        List(arr.length()) { f(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    /** A different couplet each minute; stepping by a large prime scatters poets instead of running through one. */
    fun forMinute(ctx: Context, epochMinute: Long = System.currentTimeMillis() / 60_000): Couplet? {
        val list = all(ctx)
        if (list.isEmpty()) return null
        return list[((epochMinute * 7919) % list.size).toInt()]
    }
}
