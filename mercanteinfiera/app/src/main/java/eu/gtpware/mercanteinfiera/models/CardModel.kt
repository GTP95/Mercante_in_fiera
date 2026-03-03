package eu.gtpware.mercanteinfiera.models

import androidx.compose.ui.graphics.Color

data class CardModel(
    val id: Int = 0,
    val name: String = "",
    val deckName: String = "default_deck",
    val placeholderColorArgb: Int = 0xFFCCCCCC.toInt()
) {
    // Helper to get Compose Color back
    val placeholderColor: Color
        get() = Color(placeholderColorArgb)

    // Il percorso relativo negli assets: "deck_images/default_deck/Nome Carta.png"
    val imagePath: String
        get() = "deck_images/$deckName/${name.lowercase().replace(" ", "_")}.png"
}
