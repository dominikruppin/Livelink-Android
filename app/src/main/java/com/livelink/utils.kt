package com.livelink

    fun getUserStatus(statusNumber: Int): String {
        return when (statusNumber) {
            0 -> "Mitglied"
            6 -> "Admin"
            in 10..11 -> "Sysadmin"
            else -> "Mitglied"
        }
    }