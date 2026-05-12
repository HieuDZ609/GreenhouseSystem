package com.example.greenhousesystem.model

data class Device(
    val deviceId: String = "",      // MAC address
    val ownerUid: String = "",
    val deviceName: String = "",
    val status: String = "offline",
    val lastActive: Long = 0L
)