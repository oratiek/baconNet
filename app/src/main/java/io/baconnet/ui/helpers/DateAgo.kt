package io.baconnet.ui.helpers

import java.util.Date

fun GetDateAgo(date: Date, now: Date): String {
    val ms = now.time - date.time
    val sec = ms / 1000

    val min = sec / 60
    if (min == 0L) {
        return "${sec}s"
    }

    val hour = min / 60
    if (hour == 0L) {
        return "${min}m"
    }

    val day = hour / 60
    if (day == 0L) {
        return "${hour}h"
    }

    return "${day}d"
}
