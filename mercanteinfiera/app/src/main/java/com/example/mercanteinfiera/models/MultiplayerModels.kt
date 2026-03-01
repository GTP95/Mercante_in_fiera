package com.example.mercanteinfiera.models

data class GameRoom(
    val code: String = "",
    val hostId: String = "",
    val status: RoomStatus = RoomStatus.LOBBY,
    val players: Map<String, RoomPlayer> = emptyMap(),
    val merchantPot: Int = 0,
    val currentMessage: String = ""
)

data class RoomPlayer(
    val id: String = "",
    val name: String = "",
    val money: Int = 100,
    val isReady: Boolean = false,
    val cards: List<CardModel> = emptyList()
)

enum class RoomStatus {
    LOBBY,
    AUCTION,
    ELIMINATION,
    FINISHED
}