package com.livelink.data.model

import com.google.firebase.firestore.FieldValue

data class OnlineUser(
    val username: String = "",
    val timestamp: Any = FieldValue.serverTimestamp()
)