package com.eka.conversation.common

import java.util.Calendar
import java.util.TimeZone

object TimeUtils {
    fun getCurrentUTCEpochMillis(): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
    }

    fun formatMillisToMinutesSeconds(milliseconds: Long): String {
        val minutes = milliseconds / 1000 / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
