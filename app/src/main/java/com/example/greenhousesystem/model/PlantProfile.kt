package com.example.greenhousesystem.model



data class PlantProfile(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val tempMin: Double = 0.0,
    val tempMax: Double = 0.0,
    val humidityMin: Double = 0.0,
    val humidityMax: Double = 0.0,
    val recommendedLedMode: String = "VEGETATIVE"
)