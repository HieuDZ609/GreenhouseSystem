package com.example.greenhousesystem.model

data class Telemetry(
    val currentTemp: Double = 0.0,
    val currentHumid: Double = 0.0,
    val timestamp: Long = 0L
)

data class TelemetryHistory(
    val temp: Double = 0.0,
    val humid: Double = 0.0,
    val timestamp: Long = 0L
)