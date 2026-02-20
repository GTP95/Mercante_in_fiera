package com.example.mercanteinfiera.models

import androidx.compose.ui.graphics.Color

data class CardModel(
    val id: Int,
    val name: String,
    val deckName: String = "default_deck",
    val placeholderColor: Color
) {
    // Il percorso relativo negli assets: "deck_images/default_deck/Nome Carta.png"
    val imagePath: String
        get() = "deck_images/$deckName/${name.lowercase().replace(" ", "_")}.png"
}