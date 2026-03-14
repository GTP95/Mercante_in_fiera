package eu.gtpware.mercanteinfiera.models

import com.google.firebase.database.PropertyName

data class GameRoom(
    @get:PropertyName("code") @set:PropertyName("code") var code: String = "",
    @get:PropertyName("hostId") @set:PropertyName("hostId") var hostId: String = "",
    @get:PropertyName("status") @set:PropertyName("status") var status: RoomStatus = RoomStatus.LOBBY,
    @get:PropertyName("players") @set:PropertyName("players") var players: Map<String, RoomPlayer> = emptyMap(),
    @get:PropertyName("merchantPot") @set:PropertyName("merchantPot") var merchantPot: Int = 0,
    @get:PropertyName("currentMessage") @set:PropertyName("currentMessage") var currentMessage: String = "",
    @get:PropertyName("merchantCardsRemaining") @set:PropertyName("merchantCardsRemaining") var merchantCardsRemaining: List<CardModel> = emptyList(),
    @get:PropertyName("prizes") @set:PropertyName("prizes") var prizes: List<Prize> = emptyList(),
    @get:PropertyName("eliminatedCardIds") @set:PropertyName("eliminatedCardIds") var eliminatedCardIds: List<Int> = emptyList(),
    @get:PropertyName("cardsToAuction") @set:PropertyName("cardsToAuction") var cardsToAuction: List<CardModel> = emptyList(),
    @get:PropertyName("auctionCard") @set:PropertyName("auctionCard") var auctionCard: CardModel? = null,
    @get:PropertyName("currentBid") @set:PropertyName("currentBid") var currentBid: Int = 0,
    @get:PropertyName("highestBidderId") @set:PropertyName("highestBidderId") var highestBidderId: String? = null,
    @get:PropertyName("tradeRequest") @set:PropertyName("tradeRequest") var tradeRequest: TradeRequest? = null,
    @get:PropertyName("auctionTimer") @set:PropertyName("auctionTimer") var auctionTimer: Int = 0
)

data class RoomPlayer(
    @get:PropertyName("id") @set:PropertyName("id") override var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") override var name: String = "",
    @get:PropertyName("money") @set:PropertyName("money") override var money: Int = 100,
    @get:PropertyName("isReady") @set:PropertyName("isReady") var isReady: Boolean = false,
    @get:PropertyName("cards") @set:PropertyName("cards") override var cards: List<CardModel> = emptyList(),
    @get:PropertyName("isBot") @set:PropertyName("isBot") var isBot: Boolean = false
) : PlayerBase

data class TradeRequest(
    @get:PropertyName("senderId") @set:PropertyName("senderId") var senderId: String = "",
    @get:PropertyName("receiverId") @set:PropertyName("receiverId") var receiverId: String = "",
    @get:PropertyName("type") @set:PropertyName("type") var type: String = "", // "BUY", "SELL", "EXCHANGE"
    @get:PropertyName("card1") @set:PropertyName("card1") var card1: CardModel? = null,
    @get:PropertyName("card2") @set:PropertyName("card2") var card2: CardModel? = null,
    @get:PropertyName("money") @set:PropertyName("money") var money: Int = 0,
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "PENDING" // "PENDING", "ACCEPTED", "REJECTED"
)

enum class RoomStatus {
    LOBBY,
    DISTRIBUTION,
    AUCTION,
    PRIZES,
    ELIMINATION,
    FINISHED
}

fun RoomPlayer.toPlayer(): Player = Player(
    id = id,
    name = name,
    isHuman = !isBot,
    cards = cards,
    money = money
)
