package com.example.mercanteinfiera.models

enum class GamePhase {
    MENU,
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