package com.example.greenhousesystem.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class SensorHistory(
    val temperature: Double? = 0.0,
    val humidity: Double? = 0.0,
    val timestamp: Long? = 0L,
    var id: String? = "" // Dùng để lưu key từ Firebase (record_001,...)
)