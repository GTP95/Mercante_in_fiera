package com.example.mercanteinfiera.models

import androidx.compose.ui.graphics.Color

data class CardModel(
    val id: Int,
    val name: String,
    val imageRes: Int? = null,
    val placeholderColor: Color
)