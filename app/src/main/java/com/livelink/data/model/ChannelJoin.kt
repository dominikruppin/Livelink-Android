package com.livelink.data.model

// Die Datenklasse ist für die Speicherung des Channels den man aktuell betreten hat
// Dabei speichern wir natürlich die ChannelID (die auch gleichzeitig der Channelname ist)
// Sowie den Zeitpunkt wann der Channel betreten wurde als Timestamp
data class ChannelJoin(
    val channelID: String,
    val timestamp: Long = System.currentTimeMillis()
)