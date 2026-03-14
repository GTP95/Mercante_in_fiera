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

enum class DifficultyLevel {
    EASY,
    MEDIUM,
    HARD
}

interface PlayerBase {
    val id: String
    val name: String
    val money: Int
    val cards: List<CardModel>
}

data class Player(
    override val id: String = "",
    override val name: String = "",
    val isHuman: Boolean = false,
    override val cards: List<CardModel> = emptyList(),
    override val money: Int = 100
) : PlayerBase

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
