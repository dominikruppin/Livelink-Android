package com.livelink.data.model

// Datenklasse für die Sperrung eines Nutzers
data class LockInfo(
    val lockedBy: String = "",
    val reason: String = "",
    val expirationTimestamp: Long = 0L
)