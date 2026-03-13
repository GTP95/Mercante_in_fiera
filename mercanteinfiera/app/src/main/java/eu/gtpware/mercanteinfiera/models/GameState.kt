package eu.gtpware.mercanteinfiera.models

import com.google.firebase.database.IgnoreExtraProperties

enum class GamePhase {
    MENU,
    SETTINGS,
    MULTIPLAYER_MENU,
    LOBBY,
    DISTRIBUTION,
    AUCTION,
    PRIZES,
    ELIMINATION,
    FINISHED
}

data class Player(
    val id: String = "",
    val name: String = "",
    val isHuman: Boolean = false,
    val cards: List<CardModel> = emptyList(),
    val money: Int = 100
)

@IgnoreExtraProperties
data class Prize(
    var card: CardModel = CardModel(),
    var value: Int = 0
)

sealed class AIProposal {
    data class Buy(val ai: Player, val card: CardModel, val price: Int) : AIProposal()
    data class Sell(val ai: Player, val card: CardModel, val price: Int) : AIProposal()
    data class Exchange(val ai: Player, val aiCard: CardModel, val playerCard: CardModel) : AIProposal()
}
