package com.example.greenhousesystem.model

enum class LedMode(
    val displayName: String,
    val icon: String,
    val description: String,
    val red: Int,
    val green: Int,
    val blue: Int

) {
    GERMINATION(
        displayName = "Nảy mầm",
        icon = "🌱",
        description = "Kích thích hạt nảy mầm, phát triển rễ",
        red = 180, green = 30, blue = 120
    ),
    VEGETATIVE(
        displayName = "Phát triển lá",
        icon = "🍃",
        description = "Thúc đẩy quang hợp, tăng diện tích lá",
        red = 100, green = 20, blue = 255
    ),
    FLOWERING(
        displayName = "Ra hoa",
        icon = "🌸",
        description = "Kích thích hormone ra hoa",
        red = 255, green = 0, blue = 80
    ),
    FRUITING(
        displayName = "Đậu quả",
        icon = "🍅",
        description = "Tối ưu quang hợp để nuôi quả",
        red = 255, green = 10, blue = 40
    ),
    MANUAL(
        displayName = "Thủ công",
        icon = "✋",
        description = "Tự chỉnh màu theo ý muốn",
        red = 0, green = 0, blue = 0
    )
}