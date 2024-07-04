package com.livelink.data.model

data class LockInfo(
    val lockedBy: String = "",
    val reason: String = "",
    val expirationTimestamp: Long = 0L
)