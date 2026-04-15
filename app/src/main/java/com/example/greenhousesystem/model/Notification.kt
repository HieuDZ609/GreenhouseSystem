package com.example.greenhousesystem.model

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "TEMPERATURE",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

enum class NotificationType {
    TEMPERATURE, HUMIDITY, DEVICE
}