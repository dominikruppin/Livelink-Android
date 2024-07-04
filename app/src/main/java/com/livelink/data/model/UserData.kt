package com.livelink.data.model

import com.livelink.data.model.Channel
import com.livelink.data.model.ProfileVisitor

// Datenklasse zum speichern(FireStore) und abrufen der Daten eines Users
data class UserData(
    val username: String = "",
    val usernameLowercase: String = "",
    val email: String = "",
    val profilePicURL: String = "",
    val status: Int = 0,
    val name: String = "",
    val age: String = "",
    val birthday: String = "",
    val gender: String = "",
    val relationshipStatus: String = "",
    val zipCode: String = "",
    val country: String = "",
    val state: String = "",
    val city: String = "",
    val lastChannels: List<Channel> = emptyList(),
    val recentProfileVisitors: List<ProfileVisitor> = emptyList(),
    val lockInfo: LockInfo? = null
)