package com.servilocal.core.util

/** Formatea una marca de tiempo como texto relativo para la bandeja de notificaciones. */
fun relativeTime(millis: Long, now: Long = System.currentTimeMillis()): String {
    val diffMinutes = ((now - millis).coerceAtLeast(0L)) / 60_000L
    return when {
        diffMinutes < 1 -> "Ahora"
        diffMinutes < 60 -> "Hace $diffMinutes min"
        diffMinutes < 24 * 60 -> "Hace ${diffMinutes / 60} h"
        diffMinutes < 48 * 60 -> "Ayer"
        else -> "Hace ${diffMinutes / (24 * 60)} días"
    }
}
