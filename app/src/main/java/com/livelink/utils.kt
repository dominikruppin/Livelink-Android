package com.livelink

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun getUserStatus(statusNumber: Int): String {
        return when (statusNumber) {
            0 -> "Mitglied"
            6 -> "Admin"
            in 10..11 -> "Sysadmin"
            else -> "Mitglied"
        }
    }

    fun convertTimestampToTime(timestamp: Any): String {
        val timestamp = timestamp as Timestamp
        val date = timestamp.toDate()
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Europe/Berlin")
        }
        return dateFormat.format(date)
    }