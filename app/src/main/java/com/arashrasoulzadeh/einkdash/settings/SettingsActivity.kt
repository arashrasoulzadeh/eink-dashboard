package com.arashrasoulzadeh.einkdash.settings

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import com.arashrasoulzadeh.einkdash.R
import com.arashrasoulzadeh.einkdash.eink.Eink
import com.arashrasoulzadeh.einkdash.util.BaseActivity
import com.arashrasoulzadeh.einkdash.util.Ui
import java.io.File

class SettingsActivity : BaseActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var keepOn: Button
    private lateinit var dim: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        val root = Ui.screen(this, R.string.settings)

        root.addView(Ui.label(this, R.string.settings_language))
        root.addView(Ui.gap(this, 8f))
        root.addView(Ui.row(this,
            Ui.button(this, R.string.lang_system) { setLanguage("system") },
            Ui.button(this, "English") { setLanguage("en") },
            Ui.button(this, "فارسی") { setLanguage("fa") },
        ))

        root.addView(Ui.gap(this, 24f))
        root.addView(Ui.label(this, R.string.settings_display))
        root.addView(Ui.gap(this, 8f))
        keepOn = Ui.button(this, "") { prefs.keepScreenOn = !prefs.keepScreenOn; render() }
        dim = Ui.button(this, "") {
            val c = AppPrefs.DIM_CHOICES
            prefs.dimAfterMin = c[(c.indexOf(prefs.dimAfterMin) + 1) % c.size]
            render()
        }
        root.addView(keepOn)
        root.addView(Ui.gap(this, 8f))
        root.addView(dim)

        root.addView(Ui.gap(this, 24f))
        root.addView(Ui.label(this, R.string.settings_backup))
        root.addView(Ui.gap(this, 8f))
        root.addView(Ui.row(this,
            Ui.button(this, R.string.backup_export) { export() },
            Ui.button(this, R.string.backup_import) { import() },
        ))
        root.addView(Ui.gap(this, 8f))
        root.addView(Ui.text(this, 13f).apply { setText(R.string.backup_help) })
        render()
    }

    private fun render() {
        keepOn.text = getString(R.string.keep_screen_on, getString(if (prefs.keepScreenOn) R.string.on else R.string.off))
        dim.text = if (prefs.dimAfterMin == 0) getString(R.string.dim_never) else getString(R.string.dim_after, prefs.dimAfterMin)
        Eink.fullRefresh(window.decorView)
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
