package com.arashrasoulzadeh.einkdash.util

import android.content.Context

/** Persian locale helpers. */
object Fa {
    fun isFa(ctx: Context): Boolean = ctx.resources.configuration.locale.language == "fa"

    /** Separator between parts of a line. The Persian zero (۰) is a dot, so "·" would read as a digit there. */
    fun sep(ctx: Context): String = if (isFa(ctx)) " | " else " · "

    /** Swaps ASCII digits for Persian ones when the UI is Persian. */
    fun digits(ctx: Context, s: String): String =
        if (!isFa(ctx)) s else buildString { s.forEach { append(if (it in '0'..'9') '۰' + (it - '0') else it) } }
}
