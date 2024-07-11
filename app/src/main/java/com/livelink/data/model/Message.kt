package com.livelink.data.model

import com.google.firebase.firestore.FieldValue

// Definiert das Format einer Chatnachricht (Message).
// Wir benötigen den Username des Senders, die gesendete Nachricht (content)
// Außerdem speichern wir automatisch den Zeitpunkt der Nachricht als Timestamp
data class Message(
    val senderId: String = "",
    val content: String = "",
    val timestamp: Any = FieldValue.serverTimestamp()
)