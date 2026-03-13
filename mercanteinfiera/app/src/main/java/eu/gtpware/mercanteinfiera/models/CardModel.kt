package eu.gtpware.mercanteinfiera.models

import androidx.compose.ui.graphics.Color
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class CardModel(
    var id: Int = 0,
    var name: String = "",
    var deckName: String = "default_deck",
    var placeholderColorArgb: Int = 0xFFCCCCCC.toInt()
) {
    // Helper to get Compose Color back
    @get:Exclude
    val placeholderColor: Color
        get() = Color(placeholderColorArgb)

    // Il percorso relativo negli assets: "deck_images/default_deck/Nome Carta.png"
    @get:Exclude
    val imagePath: String
        get() = "deck_images/$deckName/${name.lowercase().replace(" ", "_")}.png"
}
