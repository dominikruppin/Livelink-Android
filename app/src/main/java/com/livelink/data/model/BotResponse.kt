package com.livelink.data.model

// Datenklasse für die Antwort von der Perplexity API
data class BotResponse(
    val choices: List<Choice>
)