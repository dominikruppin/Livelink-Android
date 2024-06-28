package com.livelink.data.model

data class Message(
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)