package com.arashrasoulzadeh.einkdash.util

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/** One background thread for all widget I/O; results come back on the main thread. */
object Bg {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun <T> run(work: () -> T, done: (Result<T>) -> Unit) {
        executor.execute {
            val r = runCatching(work)
            main.post { done(r) }
        }
    }
}
