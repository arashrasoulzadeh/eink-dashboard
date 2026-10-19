package com.arashrasoulzadeh.einkdash.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.dashboard.DashboardGrid
import com.arashrasoulzadeh.einkdash.dashboard.DashboardLayout
import com.arashrasoulzadeh.einkdash.dashboard.LayoutStore
import com.arashrasoulzadeh.einkdash.dashboard.Placement
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.widgets.WidgetRegistry

class HomeActivity : Activity() {

    private lateinit var store: LayoutStore
    private lateinit var layout: DashboardLayout
    private lateinit var grid: DashboardGrid
    private lateinit var pageLabel: TextView
    private lateinit var normalBar: LinearLayout
    private lateinit var editBar: LinearLayout
    private var pageIndex = 0
    private var editing = false

    private val orientation get() = resources.configuration.orientation
    private val page get() = layout.pages[pageIndex]
    private val selected get() = page.items.firstOrNull { it.id == grid.selectedId }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        store = LayoutStore(this)
        layout = store.load(orientation)

        grid = DashboardGrid(this).apply {
            onLongPress = { if (!editing) setEditing(true) }
            onCellTap = ::onCellTap
        }
        pageLabel = TextView(this).apply { textSize = 20f; gravity = Gravity.CENTER }

        normalBar = bar(
            button(R.string.prev) { flip(-1) },
            button(R.string.apps) {
                startActivity(Intent(this, AppDrawerActivity::class.java))
                overridePendingTransition(0, 0)
            },
            pageLabel,
            button(R.string.next) { flip(1) },
        )
        editBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(bar(
                button(R.string.edit_add) { addWidget() },
                button(R.string.edit_remove) { removeSelected() },
                button(R.string.edit_wider) { resize(1, 0) },
                button(R.string.edit_narrower) { resize(-1, 0) },
            ))
            addView(bar(
                button(R.string.edit_taller) { resize(0, 1) },
                button(R.string.edit_shorter) { resize(0, -1) },
                button(R.string.edit_page) { pageMenu() },
                button(R.string.edit_done) { setEditing(false) },
            ))
            visibility = View.GONE
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            // Keeps bars clear of system bars on edge-to-edge Android 15+.
            fitsSystemWindows = true
            addView(grid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(normalBar)
            addView(editBar)
        })
        showPage()
    }

    private fun bar(vararg views: View) = LinearLayout(this).apply {
        views.forEach { addView(it, LinearLayout.LayoutParams(0, (56 * resources.displayMetrics.density).toInt(), 1f)) }
    }

    private fun button(text: Int, onClick: () -> Unit) =
        Button(this).apply { setText(text); textSize = 16f; setOnClickListener { onClick() } }

    private fun showPage() {
        grid.page = page
        pageLabel.text = "${pageIndex + 1}/${layout.pages.size}"
        refresh()
    }

    private fun refresh() {
        grid.requestLayout()
        grid.invalidate()
        Eink.fullRefresh(window.decorView)
    }

    private fun save() = store.save(orientation, layout)

    private fun flip(delta: Int) {
        val next = (pageIndex + delta).coerceIn(0, layout.pages.size - 1)
        if (next != pageIndex) { pageIndex = next; grid.selectedId = null; showPage() }
    }

    private fun setEditing(on: Boolean) {
        editing = on
        grid.editMode = on
        grid.selectedId = null
        normalBar.visibility = if (on) View.GONE else View.VISIBLE
        editBar.visibility = if (on) View.VISIBLE else View.GONE
        if (!on) save()
        refresh()
    }

    /** Tap a widget to select it; tap an empty cell to move the selection there. */
    private fun onCellTap(col: Int, row: Int) {
        val hit = page.at(col, row)
        val sel = selected
        when {
            hit != null -> grid.selectedId = if (hit.id == sel?.id) null else hit.id
            // Clamp so a wide/tall widget tapped near the edge still lands on the grid.
            sel != null -> if (page.tryUpdate(sel.copy(
                col = col.coerceAtMost(page.cols - sel.w), row = row.coerceAtMost(page.rows - sel.h)))) {
                grid.rebuild(); save()
            } else toast(R.string.no_space)
        }
        refresh()
    }

    private fun resize(dw: Int, dh: Int) {
        val sel = selected ?: return toast(R.string.select_first)
        if (page.tryUpdate(sel.copy(w = sel.w + dw, h = sel.h + dh))) { grid.rebuild(); save(); refresh() }
        else toast(R.string.no_space)
    }

    private fun removeSelected() {
        val sel = selected ?: return toast(R.string.select_first)
        page.items.removeAll { it.id == sel.id }
        grid.selectedId = null
        grid.rebuild(); save(); refresh()
    }

    private fun addWidget() {
        val widgets = WidgetRegistry.all
        AlertDialog.Builder(this)
            .setItems(widgets.map { getString(it.titleRes) }.toTypedArray()) { _, i ->
                val w = widgets[i]
                val id = layout.nextId()
                val spot = page.firstFree(id, w.type, w.defaultW, w.defaultH) ?: page.firstFree(id, w.type, 1, 1)
                if (spot == null) toast(R.string.no_space)
                else { page.tryUpdate(spot); grid.selectedId = id; grid.rebuild(); save(); refresh() }
            }
            .show()
    }

    private fun pageMenu() {
        AlertDialog.Builder(this)
            .setItems(arrayOf(getString(R.string.page_add), getString(R.string.page_delete))) { _, i ->
                if (i == 0) {
                    layout.addPage(); pageIndex = layout.pages.size - 1
                } else if (layout.pages.size > 1) {
                    layout.pages.removeAt(pageIndex); pageIndex = pageIndex.coerceAtMost(layout.pages.size - 1)
                } else return@setItems toast(R.string.last_page)
                grid.selectedId = null
                save(); showPage()
            }
            .show()
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_SHORT).show()

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        // Launchers swallow Back; in edit mode Back means Done.
        KeyEvent.KEYCODE_BACK -> { if (editing) setEditing(false); true }
        KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN -> { flip(1); true }
        KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_VOLUME_UP -> { flip(-1); true }
        else -> super.onKeyDown(keyCode, event)
    }
}
