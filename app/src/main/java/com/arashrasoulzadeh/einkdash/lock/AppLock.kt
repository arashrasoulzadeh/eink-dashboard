package com.arashrasoulzadeh.einkdash.lock

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The app's own privacy lock (separate from Android's). When locked, only the clock and date show;
 * notes, endpoints, settings and apps wait behind a PIN. The PIN is stored as a salted SHA-256 hash.
 */
object AppLock {
    /** In memory: a restart or reboot comes back locked. */
    @Volatile var unlocked = false
        private set

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("lock", Context.MODE_PRIVATE)

    fun enabled(ctx: Context) = prefs(ctx).contains("hash")

    fun isLocked(ctx: Context) = enabled(ctx) && !unlocked

    fun lock() { unlocked = false }

    fun setPin(ctx: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        prefs(ctx).edit().putString("salt", salt).putString("hash", hash(salt, pin)).apply()
        unlocked = true
    }

    fun disable(ctx: Context) {
        prefs(ctx).edit().clear().apply()
        unlocked = true
    }

    fun tryUnlock(ctx: Context, pin: String): Boolean {
        val p = prefs(ctx)
        val ok = hash(p.getString("salt", "")!!, pin) == p.getString("hash", null)
        if (ok) unlocked = true
        return ok
    }

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).joinToString("") { "%02x".format(it) }
}
