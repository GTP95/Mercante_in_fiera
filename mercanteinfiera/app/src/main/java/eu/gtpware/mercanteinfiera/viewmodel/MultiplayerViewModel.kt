package eu.gtpware.mercanteinfiera.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.gtpware.mercanteinfiera.data.DeckManager
import eu.gtpware.mercanteinfiera.models.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

class MultiplayerViewModel : ViewModel() {

    private val database = Firebase.database("https://mercante-in-fiera-15aed-default-rtdb.europe-west1.firebasedatabase.app/").reference
    private val auth = Firebase.auth

    private val _currentRoom = MutableStateFlow<GameRoom?>(null)
    val currentRoom: StateFlow<GameRoom?> = _currentRoom.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var roomListener: ValueEventListener? = null
    
    // For local UI state in multiplayer
    private val _inspectingPlayer = MutableStateFlow<RoomPlayer?>(null)
    val inspectingPlayer: StateFlow<RoomPlayer?> = _inspectingPlayer.asStateFlow()

    private val _tradeDialogTarget = MutableStateFlow<Pair<RoomPlayer, CardModel>?>(null)
    val tradeDialogTarget: StateFlow<Pair<RoomPlayer, CardModel>?> = _tradeDialogTarget.asStateFlow()

    private val _offeringCard = MutableStateFlow<CardModel?>(null)
    val offeringCard: StateFlow<CardModel?> = _offeringCard.asStateFlow()

    private val _isNewGameButtonEnabled = MutableStateFlow(true)
    val isNewGameButtonEnabled: StateFlow<Boolean> = _isNewGameButtonEnabled.asStateFlow()

    private val lastProposalTimes = mutableMapOf<String, Long>()

    private var auctionTimerJob: Job? = null

    init {
        startBotTradeLoop()
    }

    private fun startBotTradeLoop() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                val room = _currentRoom.value ?: continue
                val user = auth.currentUser ?: continue
                if (room.hostId != user.uid) continue // Only host runs bot trade logic
                
                if (room.status == RoomStatus.ELIMINATION && room.tradeRequest == null) {
                    tryProposeBotTrade(room)
                }
            }
        }
    }

    private fun tryProposeBotTrade(room: GameRoom) {
        val currentTime = System.currentTimeMillis()
        val bots = room.players.values.filter { it.isBot }
        val humans = room.players.values.filter { !it.isBot }
        
        for (bot in bots) {
            val lastTime = lastProposalTimes[bot.id] ?: 0L
            if (currentTime - lastTime > 15000) {
                if (Random.nextInt(100) < 15) {
                    val target = humans.randomOrNull() ?: continue
                    generateBotProposal(bot, target)
                    lastProposalTimes[bot.id] = currentTime
                    return
                }
            }
        }
    }

    private fun generateBotProposal(bot: RoomPlayer, target: RoomPlayer) {
        val type = Random.nextInt(3)
        viewModelScope.launch {
            when (type) {
                0 -> { // Buy from human
                    if (target.cards.isNotEmpty() && bot.money >= 10) {
                        val card = target.cards.random()
                        val price = Random.nextInt(5, (bot.money / 2).coerceAtLeast(10))
                        proposeTrade(TradeRequest(bot.id, target.id, "BUY", card1 = card, money = price))
                    }
                }
                1 -> { // Sell to human
                    if (bot.cards.isNotEmpty() && target.money >= 10) {
                        val card = bot.cards.random()
                        val price = Random.nextInt(5, (target.money / 2).coerceAtLeast(10))
                        proposeTrade(TradeRequest(bot.id, target.id, "SELL", card1 = card, money = price))
                    }
                }
                2 -> { // Exchange
                    if (bot.cards.isNotEmpty() && target.cards.isNotEmpty()) {
                        val botCard = bot.cards.random()
                        val humanCard = target.cards.random()
                        proposeTrade(TradeRequest(bot.id, target.id, "EXCHANGE", card1 = botCard, card2 = humanCard))
                    }
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun createRoom(playerName: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                val user = auth.currentUser ?: try {
                    withTimeout(30000) {
                        auth.signInAnonymously().await().user ?: throw Exception("Auth failed")
                    }
                } catch (e: TimeoutCancellationException) {
                    throw Exception("Auth timeout. Check connection.")
                }
                
                val code = generateRoomCode()
                val room = GameRoom(
                    code = code,
                    hostId = user.uid,
                    status = RoomStatus.LOBBY,
                    players = mapOf(user.uid to RoomPlayer(id = user.uid, name = playerName, isReady = false))
                )
                
                try {
                    withTimeout(30000) {
                        database.child("rooms").child(code).setValue(room).await()
                    }
                } catch (e: TimeoutCancellationException) {
                    throw Exception("Timeout during room creation. Please check your connection.")
                }
                observeRoom(code)
            } catch (e: Exception) {
                Log.e("MultiplayerVM", "Error creating room", e)
                _error.value = "Failed to create room: ${e.message}"
            }
        }
    }

    fun joinRoom(code: String, playerName: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                val user = auth.currentUser ?: try {
                    withTimeout(30000) {
                        auth.signInAnonymously().await().user ?: throw Exception("Auth failed")
                    }
                } catch (e: TimeoutCancellationException) {
                    throw Exception("Auth timeout. Check connection.")
                }

                val snapshot = try {
                    withTimeout(30000) {
                        database.child("rooms").child(code).get().await()
                    }
                } catch (e: TimeoutCancellationException) {
                    throw Exception("Timeout joining room. Please check your connection.")
                }
                
                if (snapshot.exists()) {
                    val room = snapshot.getValue(GameRoom::class.java)
                    if (room != null) {
                        if (room.status != RoomStatus.LOBBY) {
                            _error.value = "Game already started"
                            return@launch
                        }
                        
                        val updatedPlayers = room.players.toMutableMap()
                        
                        var finalName = playerName
                        var counter = 2
                        val existingNames = room.players.values.map { it.name }
                        while (existingNames.contains(finalName)) {
                            finalName = "$playerName $counter"
                            counter++
                        }

                        updatedPlayers[user.uid] = RoomPlayer(id = user.uid, name = finalName)
                        database.child("rooms").child(code).child("players").setValue(updatedPlayers).await()
                        observeRoom(code)
                    } else {
                        _error.value = "Invalid room data"
                    }
                } else {
                    _error.value = "Room not found"
                }
            } catch (e: Exception) {
                Log.e("MultiplayerVM", "Error joining room", e)
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun toggleReady() {
        val room = _currentRoom.value ?: return
        val user = auth.currentUser ?: return
        val currentPlayer = room.players[user.uid] ?: return
        
        val newReadyStatus = !currentPlayer.isReady
        
        viewModelScope.launch {
            try {
                database.child("rooms")
                    .child(room.code)
                    .child("players")
                    .child(user.uid)
                    .child("isReady")
                    .setValue(newReadyStatus)
                    .await()
            } catch (e: Exception) {
                Log.e("MultiplayerVM", "Error toggling ready status", e)
                _error.value = "Failed to update status: ${e.message}"
            }
        }
    }

    fun startGame() {
        val room = _currentRoom.value ?: return
        val user = auth.currentUser ?: return
        if (room.hostId != user.uid) return

        viewModelScope.launch {
            try {
                val updatedPlayersMap = room.players.toMutableMap()
                
                // Add bots if less than 3 players
                var botCount = 1
                while (updatedPlayersMap.size < 3) {
                    val botId = "bot_$botCount"
                    val botName = "Bot $botCount"
                    updatedPlayersMap[botId] = RoomPlayer(id = botId, name = botName, isReady = true, isBot = true)
                    botCount++
                }

                val playerDeck = DeckManager.createFullDeck().shuffled()
                val merchantDeck = DeckManager.createFullDeck().shuffled()
                
                val numPlayers = updatedPlayersMap.size
                val cardsPerPlayer = if (numPlayers <= 3) 10 else 40 / (numPlayers + 1)
                
                // Distribute cards to each player
                var deckIndex = 0
                val finalPlayersMap = updatedPlayersMap.mapValues { (_, player) ->
                    val cards = playerDeck.subList(deckIndex, deckIndex + cardsPerPlayer)
                    deckIndex += cardsPerPlayer
                    player.copy(cards = cards, isReady = true)
                }

                val cardsToAuction = playerDeck.subList(deckIndex, (deckIndex + 10).coerceAtMost(playerDeck.size))
                val merchantCardsRemaining = merchantDeck.toMutableList()

                database.child("rooms").child(room.code).updateChildren(mapOf(
                    "players" to finalPlayersMap,
                    "status" to RoomStatus.AUCTION,
                    "cardsToAuction" to cardsToAuction,
                    "merchantCardsRemaining" to merchantCardsRemaining,
                    "merchantPot" to 0,
                    "currentMessage" to "Carte distribuite! Inizia l'asta."
                )).await()
                
                startNextAuction()
            } catch (e: Exception) {
                Log.e("MultiplayerVM", "Error starting game", e)
                _error.value = "Failed to start game: ${e.message}"
            }
        }
    }

    private fun startNextAuction() {
        val room = _currentRoom.value ?: return
        val user = auth.currentUser ?: return
        if (room.hostId != user.uid) return

        viewModelScope.launch {
            val snapshot = database.child("rooms").child(room.code).get().await()
            val currentRoomData = snapshot.getValue(GameRoom::class.java) ?: return@launch
            
            val cardsToAuction = currentRoomData.cardsToAuction.toMutableList()
            if (cardsToAuction.isEmpty()) {
                auctionTimerJob?.cancel()
                database.child("rooms").child(room.code).updateChildren(mapOf(
                    "status" to RoomStatus.DISTRIBUTION,
                    "auctionCard" to null,
                    "auctionTimer" to 0,
                    "currentMessage" to "Asta terminata! Il Mercante ha raccolto ${currentRoomData.merchantPot} €. Ora stabiliamo i premi."
                )).await()
                return@launch
            }

            val card = cardsToAuction.removeAt(0)
            database.child("rooms").child(room.code).updateChildren(mapOf(
                "cardsToAuction" to cardsToAuction,
                "auctionCard" to card,
                "currentBid" to 0,
                "highestBidderId" to null,
                "auctionTimer" to 10,
                "currentMessage" to "All'asta la carta: ${card.name}. Chi offre di più?"
            )).await()

            startAuctionTimer(room.code)

            delay(2000)
            simulateAiBidding()
        }
    }

    private fun startAuctionTimer(roomCode: String) {
        auctionTimerJob?.cancel()
        auctionTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val snapshot = database.child("rooms").child(roomCode).get().await()
                val room = snapshot.getValue(GameRoom::class.java) ?: break
                if (room.status != RoomStatus.AUCTION) break
                
                val newTimerValue = room.auctionTimer - 1
                if (newTimerValue >= 0) {
                    database.child("rooms").child(roomCode).child("auctionTimer").setValue(newTimerValue).await()
                } else {
                    // Timer finished
                    val winnerId = room.highestBidderId
                    val winnerName = if (winnerId != null) room.players[winnerId]?.name ?: "Nessuno" else "Nessuno"
                    
                    database.child("rooms").child(roomCode).child("currentMessage")
                        .setValue("Andata! A $winnerName per ${room.currentBid} €").await()
                    
                    delay(1000)
                    concludeAuction()
                    break
                }
            }
        }
    }

    private suspend fun simulateAiBidding() {
        val room = _currentRoom.value ?: return
        val user = auth.currentUser ?: return
        if (room.hostId != user.uid) return

        var active = true
        while (active) {
            delay(1500)
            val snapshot = database.child("rooms").child(room.code).get().await()
            val currentRoomData = snapshot.getValue(GameRoom::class.java) ?: break
            if (currentRoomData.status != RoomStatus.AUCTION) break
            if (currentRoomData.auctionTimer <= 2) break // Bots stop bidding when time is low

            val currentHighest = currentRoomData.currentBid
            val bots = currentRoomData.players.values.filter { it.isBot }
            
            val bidder = bots.filter { it.id != currentRoomData.highestBidderId && it.money > currentHighest + 2 }.shuffled().firstOrNull {
                Random.nextInt(100) > 60 
            }

            if (bidder != null) {
                val newBid = currentHighest + Random.nextInt(1, 5)
                if (newBid <= bidder.money) {
                    val updateMap = mutableMapOf<String, Any>(
                        "currentBid" to newBid,
                        "highestBidderId" to bidder.id,
                        "currentMessage" to "${bidder.name} offre $newBid €!",
                        "auctionTimer" to 10
                    )
                    database.child("rooms").child(room.code).updateChildren(updateMap).await()
                } else {
                    active = false
                }
            } else {
                active = false
            }
        }
    }

    fun playerBid() {
        val room = _currentRoom.value ?: return
        val user = auth.currentUser ?: return
        val player = room.players[user.uid] ?: return
        
        if (room.highestBidderId == user.uid) {
            // Locally we can't easily change the message since it's in the DB, 
            // but we can at least prevent the bid.
            return
        }

        val newBid = room.currentBid + 5
        if (newBid <= player.money) {
            viewModelScope.launch {
                val updateMap = mutableMapOf<String, Any>(
                    "currentBid" to newBid,
                    "highestBidderId" to user.uid,
                    "currentMessage" to "${player.name} offre $newBid €!",
                    "auctionTimer" to 10
                )
                database.child("rooms").child(room.code).updateChildren(updateMap).await()
            }
        } else {
            _error.value = "Non hai abbastanza soldi!"
        }
    }

    private suspend fun concludeAuction() {
        val room = _currentRoom.value ?: return
        val user = auth.currentUser ?: return
        if (room.hostId != user.uid) return

        val snapshot = database.child("rooms").child(room.code).get().await()
        val currentRoomData = snapshot.getValue(GameRoom::class.java) ?: return
        
        val winnerId = currentRoomData.highestBidderId
        val bid = currentRoomData.currentBid
        val card = currentRoomData.auctionCard

        if (winnerId != null && card != null) {
            val updatedPlayers = currentRoomData.players.toMutableMap()
            val winner = updatedPlayers[winnerId] ?: return
            updatedPlayers[winnerId] = winner.copy(
                money = winner.money - bid,
                cards = winner.cards + card
            )
            
            database.child("rooms").child(room.code).updateChildren(mapOf(
                "players" to updatedPlayers,
                "merchantPot" to currentRoomData.merchantPot + bid
            )).await()
        }

        startNextAuction()
    }

    fun startPrizesPhase() {
        val room = _currentRoom.value ?: return
        val user = auth.currentUser ?: return
        if (room.hostId != user.uid) return

        viewModelScope.launch {
            database.child("rooms").child(room.code).child("status").setValue(RoomStatus.PRIZES).await()
            
            val pot = room.merchantPot
            val prizeValues = if (pot > 0) {
                listOf(
                    (pot * 0.4).toInt().coerceAtLeast(5),
                    (pot * 0.25).toInt().coerceAtLeast(3),
                    (pot * 0.15).toInt().coerceAtLeast(2),
                    (pot * 0.1).toInt().coerceAtLeast(1),
                    (pot * 0.1).toInt().coerceAtLeast(1)
                )
            } else {
                listOf(50, 30, 20, 10, 5)
            }
            
            val selectedPrizes = mutableListOf<Prize>()
            val pool = room.merchantCardsRemaining.shuffled().toMutableList()
            val remainingMerchantCards = room.merchantCardsRemaining.toMutableList()
            
            for (value in prizeValues) {
                if (pool.isNotEmpty()) {
                    val card = pool.removeAt(0)
                    selectedPrizes.add(Prize(card, value))
                    remainingMerchantCards.remove(card)
                }
            }
            
            database.child("rooms").child(room.code).updateChildren(mapOf(
                "prizes" to selectedPrizes,
                "merchantCardsRemaining" to remainingMerchantCards,
                "currentMessage" to "Premi stabiliti in base all'asta. Iniziamo l'eliminazione!",
                "status" to RoomStatus.ELIMINATION
            )).await()
        }
    }

    fun drawEliminationCard() {
        val room = _currentRoom.value ?: return
        val user = auth.currentUser ?: return
        if (room.hostId != user.uid) return

        viewModelScope.launch {
            if (room.merchantCardsRemaining.isNotEmpty()) {
                val remaining = room.merchantCardsRemaining.toMutableList()
                val card = remaining.removeAt(Random.nextInt(remaining.size))
                
                val updatedPlayers = room.players.mapValues { (_, player) ->
                    player.copy(cards = player.cards.filter { it.id != card.id })
                }
                
                val newEliminatedIds = room.eliminatedCardIds + card.id
                
                database.child("rooms").child(room.code).updateChildren(mapOf(
                    "merchantCardsRemaining" to remaining,
                    "eliminatedCardIds" to newEliminatedIds,
                    "players" to updatedPlayers,
                    "currentMessage" to "È uscita la carta: ${card.name}. Questa carta è eliminata dal gioco!"
                )).await()
                
                if (remaining.isEmpty()) {
                    finishGame()
                }
            }
        }
    }

    private suspend fun finishGame() {
        val room = _currentRoom.value ?: return
        val user = auth.currentUser ?: return
        if (room.hostId != user.uid) return

        val finalPlayers = room.players.mapValues { (_, player) ->
            var roundWinnings = 0
            player.cards.forEach { card ->
                val prize = room.prizes.find { it.card.id == card.id }
                if (prize != null) {
                    roundWinnings += prize.value
                }
            }
            player.copy(money = player.money + roundWinnings)
        }

        database.child("rooms").child(room.code).updateChildren(mapOf(
            "status" to RoomStatus.FINISHED,
            "players" to finalPlayers,
            "currentMessage" to "Gioco terminato! Ecco le vincite finali."
        )).await()

        viewModelScope.launch {
            _isNewGameButtonEnabled.value = false
            delay(2000)
            _isNewGameButtonEnabled.value = true
        }
    }

    fun resetGame() {
        val room = _currentRoom.value ?: return
        val user = auth.currentUser ?: return
        if (room.hostId != user.uid) return

        viewModelScope.launch {
            database.child("rooms").child(room.code).updateChildren(mapOf(
                "status" to RoomStatus.LOBBY,
                "prizes" to emptyList<Prize>(),
                "eliminatedCardIds" to emptyList<Int>(),
                "merchantPot" to 0,
                "auctionCard" to null,
                "cardsToAuction" to emptyList<CardModel>(),
                "tradeRequest" to null,
                "auctionTimer" to 0
            )).await()
        }
    }

    // Trade Logic
    fun proposeTrade(request: TradeRequest) {
        val room = _currentRoom.value ?: return
        viewModelScope.launch {
            database.child("rooms").child(room.code).child("tradeRequest").setValue(request).await()
            
            // If receiver is bot, simulate response
            val receiver = room.players[request.receiverId]
            if (receiver != null && receiver.isBot) {
                delay(2000)
                if (Random.nextBoolean()) acceptTrade() else rejectTrade()
            }
        }
    }

    fun acceptTrade() {
        val room = _currentRoom.value ?: return
        val request = room.tradeRequest ?: return
        
        viewModelScope.launch {
            val updatedPlayers = room.players.toMutableMap()
            val sender = updatedPlayers[request.senderId] ?: return@launch
            val receiver = updatedPlayers[request.receiverId] ?: return@launch
            
            when (request.type) {
                "BUY" -> {
                    updatedPlayers[request.senderId] = sender.copy(money = sender.money - request.money, cards = sender.cards + request.card1!!)
                    updatedPlayers[request.receiverId] = receiver.copy(money = receiver.money + request.money, cards = receiver.cards - request.card1!!)
                }
                "SELL" -> {
                    updatedPlayers[request.senderId] = sender.copy(money = sender.money + request.money, cards = sender.cards - request.card1!!)
                    updatedPlayers[request.receiverId] = receiver.copy(money = receiver.money - request.money, cards = receiver.cards + request.card1!!)
                }
                "EXCHANGE" -> {
                    updatedPlayers[request.senderId] = sender.copy(cards = (sender.cards - request.card1!!) + request.card2!!)
                    updatedPlayers[request.receiverId] = receiver.copy(cards = (receiver.cards - request.card2!!) + request.card1!!)
                }
            }
            
            database.child("rooms").child(room.code).updateChildren(mapOf(
                "players" to updatedPlayers,
                "tradeRequest" to null,
                "currentMessage" to "Scambio effettuato tra ${sender.name} e ${receiver.name}!"
            )).await()
        }
    }

    fun rejectTrade() {
        val room = _currentRoom.value ?: return
        val request = room.tradeRequest ?: return
        val sender = room.players[request.senderId]
        val receiver = room.players[request.receiverId]

        viewModelScope.launch {
            database.child("rooms").child(room.code).updateChildren(mapOf(
                "tradeRequest" to null,
                "currentMessage" to "${receiver?.name ?: "Qualcuno"} ha rifiutato l'offerta di ${sender?.name ?: "qualcuno"}."
            )).await()
        }
    }

    // Trade Helpers to match GameViewModel API
    fun proposeMoneyTrade(targetPlayer: RoomPlayer, targetCard: CardModel, moneyOffer: Int) {
        if (moneyOffer <= 0) return
        val myId = auth.currentUser?.uid ?: return
        proposeTrade(TradeRequest(myId, targetPlayer.id, "BUY", card1 = targetCard, money = moneyOffer))
    }

    fun proposeCardTrade(targetPlayer: RoomPlayer, targetCard: CardModel, offeredCard: CardModel) {
        val myId = auth.currentUser?.uid ?: return
        proposeTrade(TradeRequest(myId, targetPlayer.id, "EXCHANGE", card1 = offeredCard, card2 = targetCard))
    }

    fun sellCardForMoney(targetPlayer: RoomPlayer, myCard: CardModel, moneyRequested: Int) {
        if (moneyRequested <= 0) return
        val myId = auth.currentUser?.uid ?: return
        proposeTrade(TradeRequest(myId, targetPlayer.id, "SELL", card1 = myCard, money = moneyRequested))
    }

    fun swapCardForCard(targetPlayer: RoomPlayer, myCard: CardModel, targetCard: CardModel) {
        val myId = auth.currentUser?.uid ?: return
        proposeTrade(TradeRequest(myId, targetPlayer.id, "EXCHANGE", card1 = myCard, card2 = targetCard))
    }

    // Local UI interactions in MP
    fun inspectPlayer(player: RoomPlayer) {
        val room = _currentRoom.value ?: return
        if (room.status != RoomStatus.ELIMINATION) return
        _inspectingPlayer.value = player
    }

    fun stopInspecting() {
        _inspectingPlayer.value = null
    }

    fun openTradeDialog(player: RoomPlayer, card: CardModel) {
        _tradeDialogTarget.value = player to card
    }

    fun closeTradeDialog() {
        _tradeDialogTarget.value = null
    }

    fun openOfferDialog(card: CardModel) {
        val room = _currentRoom.value ?: return
        if (room.status != RoomStatus.ELIMINATION) return
        _offeringCard.value = card
    }

    fun closeOfferDialog() {
        _offeringCard.value = null
    }

    private fun observeRoom(code: String) {
        roomListener?.let { database.child("rooms").child(_currentRoom.value?.code ?: "").removeEventListener(it) }
        
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val room = snapshot.getValue(GameRoom::class.java)
                    _currentRoom.value = room
                } catch (e: Exception) {
                    Log.e("MultiplayerVM", "Error parsing room data", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _error.value = error.message
            }
        }
        database.child("rooms").child(code).addValueEventListener(roomListener!!)
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    override fun onCleared() {
        super.onCleared()
        _currentRoom.value?.code?.let { code ->
            roomListener?.let { database.child("rooms").child(code).removeEventListener(it) }
        }
    }
}