package com.livelink.data

data class UserData(
    val username: String = "",
    val email: String = "",
    val profilePic: ProfilePic? = null,
    val status: Int = 0,
    val name: String = "",
    val age: Int = 0,
    val birthday: String = "",
    val gender: String = "",
    val relationshipStatus: String = "",
    val allProfilePics: MutableList<ProfilePic> = mutableListOf(),
)