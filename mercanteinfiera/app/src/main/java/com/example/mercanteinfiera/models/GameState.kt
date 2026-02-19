package com.example.mercanteinfiera.models

enum class GamePhase {
    DISTRIBUTION,
    PRIZES,
    ELIMINATION,
    FINISHED
}

data class Player(
    val id: Int,
    val name: String,
    val isHuman: Boolean = false,
    val cards: List<CardModel> = emptyList(),
    val balance: Int = 100 // Monete virtuali per l'asta
)

data class Prize(
    val card: CardModel,
    val value: Int
)