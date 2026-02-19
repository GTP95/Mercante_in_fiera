package com.example.mercanteinfiera.utils

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

object ColorGenerator {
    
    fun generateUniqueColor(): Color {
        // Generate random RGB values
        val red = Random.nextInt(0, 256)
        val green = Random.nextInt(0, 256)
        val blue = Random.nextInt(0, 256)
        
        return Color(red, green, blue)
    }
    
    fun generateUniqueColors(count: Int): List<Color> {
        return (1..count).map { generateUniqueColor() }
    }
}