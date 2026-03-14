package com.focusfarm.app.ui.theme

import androidx.compose.ui.graphics.Color

fun plantAccentColor(plantId: String): Color = when (plantId) {
    "sprout" -> Success
    "sunflower" -> Amber
    "cactus" -> Color(0xFF8DB87A)
    "tree" -> Color(0xFF3D7B5A)
    "mushroom" -> Soil
    "cherry" -> Color(0xFFE8A0BF)
    "bamboo" -> Color(0xFF8BC48A)
    "lotus" -> Color(0xFFC97BB5)
    "clover" -> Color(0xFF5BBF9F)
    "bonsai" -> Color(0xFF7A5F4A)
    else -> Amber
}
