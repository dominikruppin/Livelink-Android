package com.livelink.data.model

// Datenklasse für die Anfrage an die Perplexity API
data class BotRequest(
    val model: String,
    val messages: List<BotMessage>,
    val language: String = "de"
)