package com.example.greenhousesystem.model

data class DeviceConfig(
    val ledOn: Boolean = false,
    val ledRed: Int = 0,
    val ledGreen: Int = 0,
    val ledBlue: Int = 0,
    val ledMode: String = "MANUAL",
    val tempMax: Double = 35.0,
    val tempMin: Double = 15.0,
    val humidMax: Double = 90.0,
    val humidMin: Double = 40.0,
    val selectedPlant: String = "TOMATO"
)