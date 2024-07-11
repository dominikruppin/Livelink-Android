package com.livelink.data.model

import com.google.firebase.firestore.FieldValue

// Datenklasse für das speichern eines Nutzers für die onlineUsersListe
data class OnlineUser(
    val username: String = "",
    val age: String = "",
    val gender: String = "",
    val profilePic: String = "",
    val status: Int = 0,
    val joinTimestamp: Any = FieldValue.serverTimestamp(),
    val timestamp: Any = FieldValue.serverTimestamp()
)