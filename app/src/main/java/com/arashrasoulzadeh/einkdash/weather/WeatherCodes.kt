package com.arashrasoulzadeh.einkdash.weather

import com.arashrasoulzadeh.einkdash.R

/** WMO weather codes as used by Open-Meteo. */
object WeatherCodes {
    fun label(code: Int): Int = when (code) {
        0 -> R.string.wx_clear
        1, 2 -> R.string.wx_partly
        3 -> R.string.wx_cloudy
        45, 48 -> R.string.wx_fog
        in 51..57 -> R.string.wx_drizzle
        in 61..67, in 80..82 -> R.string.wx_rain
        in 71..77, 85, 86 -> R.string.wx_snow
        in 95..99 -> R.string.wx_storm
        else -> R.string.wx_unknown
    }
}
