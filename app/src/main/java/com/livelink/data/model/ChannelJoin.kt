package com.livelink.data.model

data class ChannelJoin(
    val channelID: String,
    val timestamp: Long = System.currentTimeMillis()
)