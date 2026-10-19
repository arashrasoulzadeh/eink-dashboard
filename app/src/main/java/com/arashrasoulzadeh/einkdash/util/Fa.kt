package com.arashrasoulzadeh.einkdash.util

import android.content.Context

/** Persian locale helpers. */
object Fa {
    fun isFa(ctx: Context): Boolean = ctx.resources.configuration.locale.language == "fa"

    /** Swaps ASCII digits for Persian ones when the UI is Persian. */
    fun digits(ctx: Context, s: String): String =
        if (!isFa(ctx)) s else buildString { s.forEach { append(if (it in '0'..'9') '۰' + (it - '0') else it) } }
}
