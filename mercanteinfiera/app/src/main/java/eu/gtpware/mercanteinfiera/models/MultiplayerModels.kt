package eu.gtpware.mercanteinfiera.models

import com.google.firebase.database.PropertyName

data class GameRoom(
    @get:PropertyName("code") @set:PropertyName("code") var code: String = "",
    @get:PropertyName("hostId") @set:PropertyName("hostId") var hostId: String = "",
    @get:PropertyName("status") @set:PropertyName("status") var status: RoomStatus = RoomStatus.LOBBY,
    @get:PropertyName("players") @set:PropertyName("players") var players: Map<String, RoomPlayer> = emptyMap(),
    @get:PropertyName("merchantPot") @set:PropertyName("merchantPot") var merchantPot: Int = 0,
    @get:PropertyName("currentMessage") @set:PropertyName("currentMessage") var currentMessage: String = ""
)

data class RoomPlayer(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("money") @set:PropertyName("money") var money: Int = 100,
    @get:PropertyName("isReady") @set:PropertyName("isReady") var isReady: Boolean = false,
    @get:PropertyName("cards") @set:PropertyName("cards") var cards: List<CardModel> = emptyList()
)

enum class RoomStatus {
    LOBBY,
    AUCTION,
    ELIMINATION,
    FINISHED
}
