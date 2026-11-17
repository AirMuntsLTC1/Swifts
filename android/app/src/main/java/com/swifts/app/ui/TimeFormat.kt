package com.swifts.app.ui

import java.time.OffsetDateTime

/** Short relative time such as "now", "5m", "2h", "3d". */
fun relativeTime(iso: String, nowMillis: Long = System.currentTimeMillis()): String {
    val then = runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull() ?: return ""
    val minutes = ((nowMillis - then) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 60 * 24 -> "${minutes / 60}h"
        else -> "${minutes / (60 * 24)}d"
    }
}
