package com.livelink.data.model

data class BotRequest(
    val model: String,
    val messages: List<BotMessage>,
    val language: String = "de"
)