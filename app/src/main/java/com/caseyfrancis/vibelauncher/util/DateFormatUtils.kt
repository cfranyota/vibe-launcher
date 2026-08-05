package com.caseyfrancis.vibelauncher.util

/** "1st", "2nd", "3rd", "4th" ... "21st" ... "31st". */
fun ordinalSuffix(dayOfMonth: Int): String {
    if (dayOfMonth in 11..13) return "${dayOfMonth}th"
    return when (dayOfMonth % 10) {
        1 -> "${dayOfMonth}st"
        2 -> "${dayOfMonth}nd"
        3 -> "${dayOfMonth}rd"
        else -> "${dayOfMonth}th"
    }
}
