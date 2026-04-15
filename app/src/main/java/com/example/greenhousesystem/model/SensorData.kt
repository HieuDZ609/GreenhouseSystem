package com.example.greenhousesystem.model

data class SensorData(
    val temperature: Double = 0.0,
    val humidity: Double = 0.0,
    val timestamp: Long = 0L
)

data class DeviceStatus(
    val status: String = "offline",
    val lastSeen: Long = 0L
)

data class LedStatus(
    val isOn: Boolean = false,
    val red: Int = 0,
    val green: Int = 0,
    val blue: Int = 0,
    val mode: String = "MANUAL"
)