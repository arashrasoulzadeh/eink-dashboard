package com.arashrasoulzadeh.einkdash.dashboard

import org.json.JSONArray
import org.json.JSONObject

data class Placement(val id: Long, val type: String, val col: Int, val row: Int, val w: Int, val h: Int) {
    fun overlaps(o: Placement) =
        col < o.col + o.w && o.col < col + w && row < o.row + o.h && o.row < row + h
}

/** One grid page. Pure logic, no Android views, so it stays easy to test. */
class Page(val cols: Int, val rows: Int, val items: MutableList<Placement> = mutableListOf()) {

    fun fits(p: Placement): Boolean =
        p.col >= 0 && p.row >= 0 && p.w >= 1 && p.h >= 1 &&
            p.col + p.w <= cols && p.row + p.h <= rows &&
            items.none { it.id != p.id && it.overlaps(p) }

    fun at(col: Int, row: Int): Placement? =
        items.firstOrNull { col >= it.col && col < it.col + it.w && row >= it.row && row < it.row + it.h }

    /** Replaces the item with the same id if the new geometry fits. */
    fun tryUpdate(p: Placement): Boolean {
        if (!fits(p)) return false
        val i = items.indexOfFirst { it.id == p.id }
        if (i >= 0) items[i] = p else items.add(p)
        return true
    }

    fun firstFree(id: Long, type: String, w: Int, h: Int): Placement? {
        for (row in 0..rows - h) for (col in 0..cols - w) {
            val p = Placement(id, type, col, row, w, h)
            if (fits(p)) return p
        }
        return null
    }
}

class DashboardLayout(val cols: Int, val rows: Int, val pages: MutableList<Page>) {

    fun nextId(): Long = (pages.flatMap { it.items }.maxOfOrNull { it.id } ?: 0L) + 1

    fun addPage(): Page = Page(cols, rows).also { pages.add(it) }

    fun toJson(): String = JSONObject()
        .put("cols", cols).put("rows", rows)
        .put("pages", JSONArray().apply {
            pages.forEach { page ->
                put(JSONArray().apply {
                    page.items.forEach {
                        put(JSONObject().put("id", it.id).put("type", it.type)
                            .put("col", it.col).put("row", it.row).put("w", it.w).put("h", it.h))
                    }
                })
            }
        }).toString()

    companion object {
        /** Returns null when the JSON is corrupt or was saved for a different grid size. */
        fun fromJson(json: String, cols: Int, rows: Int): DashboardLayout? = try {
            val o = JSONObject(json)
            if (o.getInt("cols") != cols || o.getInt("rows") != rows) null
            else {
                val arr = o.getJSONArray("pages")
                val pages = MutableList(arr.length()) { i ->
                    val items = arr.getJSONArray(i)
                    Page(cols, rows).apply {
                        for (j in 0 until items.length()) {
                            val it = items.getJSONObject(j)
                            tryUpdate(Placement(it.getLong("id"), it.getString("type"),
                                it.getInt("col"), it.getInt("row"), it.getInt("w"), it.getInt("h")))
                        }
                    }
                }
                if (pages.isEmpty()) pages.add(Page(cols, rows))
                DashboardLayout(cols, rows, pages)
            }
        } catch (_: Exception) {
            null
        }
    }
}
