package com.arashrasoulzadeh.einkdash.settings

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.notes.NoteServer
import com.arashrasoulzadeh.einkdash.util.BaseActivity
import com.arashrasoulzadeh.einkdash.util.Fa
import com.arashrasoulzadeh.einkdash.util.Ui
import java.io.File

class SettingsActivity : BaseActivity() {

    private lateinit var prefs: AppPrefs
    private val renders = mutableListOf<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        val screen = Ui.screen(this, R.string.settings)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        screen.addView(ScrollView(this).apply { addView(root) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        fun section(title: Int) { root.addView(Ui.gap(this, 20f)); root.addView(Ui.label(this, title)); root.addView(Ui.gap(this, 8f)) }
        fun toggle(text: () -> String, onClick: () -> Unit): Button {
            val b = Ui.button(this, "") { onClick(); render() }
            renders += { b.text = text() }
            root.addView(b); root.addView(Ui.gap(this, 8f))
            return b
        }
        fun note(text: () -> String) {
            val t = Ui.text(this, 13f)
            renders += { t.text = text() }
            root.addView(t); root.addView(Ui.gap(this, 4f))
        }
        fun onOff(on: Boolean) = getString(if (on) R.string.on else R.string.off)
        fun hour(h: Int) = Fa.digits(this, "%02d:00".format(h))
        fun <T> next(choices: IntArray, cur: Int) = choices[(choices.indexOf(cur) + 1) % choices.size]

        section(R.string.settings_home)
        toggle({ getString(if (DefaultHome.isDefault(this)) R.string.home_is_default else R.string.home_set_default) }) {
            if (!DefaultHome.isDefault(this)) DefaultHome.request(this)
        }

        section(R.string.settings_lock)
        toggle({ getString(R.string.lock_toggle, onOff(com.arashrasoulzadeh.einkdash.lock.AppLock.enabled(this))) }) {
            if (com.arashrasoulzadeh.einkdash.lock.AppLock.enabled(this)) com.arashrasoulzadeh.einkdash.lock.AppLock.disable(this) else askPin()
        }
        toggle({ getString(R.string.lock_change) }) { if (com.arashrasoulzadeh.einkdash.lock.AppLock.enabled(this)) askPin() }
        note { getString(R.string.lock_help) }

        section(R.string.settings_language)
        root.addView(Ui.row(this,
            Ui.button(this, R.string.lang_system) { setLanguage("system") },
            Ui.button(this, "English") { setLanguage("en") },
            Ui.button(this, "فارسی") { setLanguage("fa") },
        ))

        section(R.string.settings_display)
        toggle({ getString(if (prefs.keepScreenOn) R.string.screen_mode_on else R.string.screen_mode_saver) }) { prefs.keepScreenOn = !prefs.keepScreenOn }
        note { getString(R.string.saver_help) }
        toggle({ if (prefs.dimAfterMin == 0) getString(R.string.dim_never) else getString(R.string.dim_after, prefs.dimAfterMin) }) {
            prefs.dimAfterMin = next<Int>(AppPrefs.DIM_CHOICES, prefs.dimAfterMin)
        }
        toggle({ getString(R.string.large_print, onOff(prefs.largePrint)) }) { prefs.largePrint = !prefs.largePrint; recreate() }
        toggle({ getString(R.string.sleep_face, onOff(prefs.sleepFace)) }) { prefs.sleepFace = !prefs.sleepFace }
        note { getString(R.string.sleep_face_help) }

        section(R.string.settings_morning)
        toggle({ if (prefs.summaryHour < 0) getString(R.string.summary_off) else getString(R.string.summary_at, hour(prefs.summaryHour)) }) {
            prefs.summaryHour = next<Int>(AppPrefs.SUMMARY_CHOICES, prefs.summaryHour)
        }

        section(R.string.settings_night)
        toggle({ if (prefs.nightStart < 0) getString(R.string.night_off) else getString(R.string.night_from, hour(prefs.nightStart), hour(prefs.nightEnd)) }) {
            prefs.nightStart = next<Int>(AppPrefs.NIGHT_CHOICES, prefs.nightStart)
        }
        toggle({ getString(R.string.night_clock, onOff(prefs.nightClock)) }) { prefs.nightClock = !prefs.nightClock }
        note { getString(R.string.night_help) }

        section(R.string.settings_phone_notes)
        toggle({ getString(R.string.note_server, onOff(prefs.noteServer)) }) {
            prefs.noteServer = !prefs.noteServer
            NoteServer.sync(this)
        }
        note {
            if (!prefs.noteServer) getString(R.string.note_server_help)
            else NoteServer.address(this)?.let { getString(R.string.note_server_open, it, Fa.digits(this, prefs.noteServerPin)) }
                ?: getString(R.string.note_server_no_wifi)
        }

        section(R.string.settings_backup)
        root.addView(Ui.row(this,
            Ui.button(this, R.string.backup_export) { export() },
            Ui.button(this, R.string.backup_import) { import() },
        ))
        root.addView(Ui.gap(this, 8f))
        root.addView(Ui.text(this, 13f).apply { setText(R.string.backup_help) })
        render()
    }

    override fun onResume() {
        super.onResume()
        render() // default-home state may have changed in the system dialog
    }

    private fun render() {
        renders.forEach { it() }
        Eink.fullRefresh(window.decorView)
    }

    /** PIN of 4–8 digits, typed twice. */
    private fun askPin() {
        fun field() = Ui.input(this, R.string.lock_pin_hint).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val a = field(); val b = field()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = Ui.dp(context, 16f); setPadding(p, p, p, 0)
            addView(a); addView(Ui.gap(context, 8f)); addView(b)
        }
        android.app.AlertDialog.Builder(this).setTitle(R.string.lock_set_pin).setView(box)
            .setPositiveButton(R.string.save) { _, _ ->
                val pin = a.text.toString()
                when {
                    pin.length !in 4..8 -> Toast.makeText(this, R.string.lock_pin_len, Toast.LENGTH_LONG).show()
                    pin != b.text.toString() -> Toast.makeText(this, R.string.lock_pin_mismatch, Toast.LENGTH_LONG).show()
                    else -> { com.arashrasoulzadeh.einkdash.lock.AppLock.setPin(this, pin); render() }
                }
            }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun setLanguage(lang: String) {
        if (prefs.language == lang) return
        prefs.language = lang
        recreate()
    }

    // Android 4.4+ uses the system file picker; 4.2/4.3 fall back to a fixed file in Downloads.
    private val legacyFile get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)

    private fun export() {
        if (Build.VERSION.SDK_INT >= 19) {
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, FILE_NAME), REQ_EXPORT)
        } else done(runCatching { legacyFile.parentFile?.mkdirs(); legacyFile.writeText(Backup.export(this)) }, R.string.backup_saved)
    }

    private fun import() {
        if (Build.VERSION.SDK_INT >= 19) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), REQ_IMPORT)
        } else done(runCatching { Backup.import(this, legacyFile.readText()) }, R.string.backup_restored)
    }

    @Deprecated("Framework API; minSdk 17 has no Activity Result API without AndroidX")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri = data?.data
        if (resultCode != Activity.RESULT_OK || uri == null) return
        when (requestCode) {
            REQ_EXPORT -> done(runCatching {
                contentResolver.openOutputStream(uri)!!.use { it.write(Backup.export(this).toByteArray()) }
            }, R.string.backup_saved)
            REQ_IMPORT -> done(runCatching {
                Backup.import(this, contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })
            }, R.string.backup_restored)
        }
    }

    private fun done(r: Result<Unit>, okRes: Int) {
        Toast.makeText(this, if (r.isSuccess) okRes else R.string.backup_failed, Toast.LENGTH_LONG).show()
        if (r.isSuccess && okRes == R.string.backup_restored) recreate()
    }

    companion object {
        private const val FILE_NAME = "einkdash-backup.json"
        private const val REQ_EXPORT = 1
        private const val REQ_IMPORT = 2
    }
}
