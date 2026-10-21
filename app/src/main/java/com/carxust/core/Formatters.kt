package com.carxust.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Formatters {

    const val KM_PER_MILE = 1.609344

    fun speed(kph: Float, useMiles: Boolean): String {
        if (kph.isNaN() || kph.isInfinite()) return "0"
        val value = if (useMiles) kph / KM_PER_MILE else kph
        return value.roundToInt().toString()
    }

    fun speedUnit(useMiles: Boolean): String = if (useMiles) "mph" else "km/h"

    fun distance(km: Float, useMiles: Boolean): String {
        val value = if (useMiles) km / KM_PER_MILE else km
        return if (value >= 100f) value.roundToInt().toString()
        else String.format(Locale.US, "%.1f", value)
    }

    fun distanceUnit(useMiles: Boolean): String = if (useMiles) "mi" else "km"

    fun power(kw: Float): String = when {
        abs(kw) >= 100f -> kw.roundToInt().toString()
        abs(kw) >= 10f -> String.format(Locale.US, "%.0f", kw)
        else -> String.format(Locale.US, "%.1f", kw)
    }

    fun temperature(celsius: Float, useMiles: Boolean): String {
        val value = if (useMiles) celsius * 9f / 5f + 32f else celsius
        return "${value.roundToInt()}°"
    }

    fun temperatureUnit(useMiles: Boolean): String = if (useMiles) "°F" else "°C"

    fun clock(date: Date = Date()): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)

    fun shortDate(date: Date = Date()): String =
        SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(date)

    fun duration(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    fun gearLabel(gear: String): String = when (gear.uppercase(Locale.US)) {
        "P" -> "P"
        "R" -> "R"
        "N" -> "N"
        "D" -> "D"
        "B" -> "B"
        else -> gear.uppercase(Locale.US)
    }

    fun pressure(bar: Float, useMiles: Boolean): String {
        val value = if (useMiles) bar * 14.5038f else bar
        val unit = if (useMiles) "psi" else "bar"
        return String.format(Locale.US, "%.1f %s", value, unit)
    }

    fun percent(value: Float): String = "${value.roundToInt()}%"
}
