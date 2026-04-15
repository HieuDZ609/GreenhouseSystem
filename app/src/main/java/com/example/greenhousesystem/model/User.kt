package com.example.greenhousesystem.model

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val loginFailCount: Int = 0,
    val isLocked: Boolean = false
)