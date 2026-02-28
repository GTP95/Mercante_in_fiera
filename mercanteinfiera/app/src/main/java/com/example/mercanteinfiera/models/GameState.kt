package com.example.mercanteinfiera.models

enum class GamePhase {
    MENU,
    SETTINGS,
    DISTRIBUTION,
    AUCTION,
    PRIZES,
    ELIMINATION,
    FINISHED
}

data class Player(
    val id: Int,
    val name: String,
    val isHuman: Boolean = false,
    val cards: List<CardModel> = emptyList(),
    val money: Int = 100 // Unico campo per il denaro del giocatore
)

data class Prize(
    val card: CardModel,
    val value: Int
)

sealed class AIProposal {
    data class Buy(val ai: Player, val card: CardModel, val price: Int) : AIProposal()
    data class Sell(val ai: Player, val card: CardModel, val price: Int) : AIProposal()
    data class Exchange(val ai: Player, val aiCard: CardModel, val playerCard: CardModel) : AIProposal()
}