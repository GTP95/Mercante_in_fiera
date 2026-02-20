package com.example.mercanteinfiera.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mercanteinfiera.data.DeckManager
import com.example.mercanteinfiera.data.SettingsManager
import com.example.mercanteinfiera.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsManager = SettingsManager(application)
    
    private val _gameState = MutableStateFlow(GamePhase.MENU)
    val gameState: StateFlow<GamePhase> = _gameState.asStateFlow()
    
    private val _merchantDeck = MutableStateFlow<List<CardModel>>(emptyList())
    val merchantDeck: StateFlow<List<CardModel>> = _merchantDeck.asStateFlow()
    
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()
    
    private val _prizes = MutableStateFlow<List<Prize>>(emptyList())
    val prizes: StateFlow<List<Prize>> = _prizes.asStateFlow()
    
    private val _eliminatedCards = MutableStateFlow<Set<Int>>(emptySet())
    val eliminatedCards: StateFlow<Set<Int>> = _eliminatedCards.asStateFlow()
    
    private val _currentMessage = MutableStateFlow("Benvenuti al Mercante in Fiera!")
    val currentMessage: StateFlow<String> = _currentMessage.asStateFlow()

    private val _inspectingPlayer = MutableStateFlow<Player?>(null)
    val inspectingPlayer: StateFlow<Player?> = _inspectingPlayer.asStateFlow()

    private val _tradeDialogTarget = MutableStateFlow<Pair<Player, CardModel>?>(null)
    val tradeDialogTarget: StateFlow<Pair<Player, CardModel>?> = _tradeDialogTarget.asStateFlow()

    private val _offeringCard = MutableStateFlow<CardModel?>(null)
    val offeringCard: StateFlow<CardModel?> = _offeringCard.asStateFlow()

    // Auction State
    private val _auctionCard = MutableStateFlow<CardModel?>(null)
    val auctionCard: StateFlow<CardModel?> = _auctionCard.asStateFlow()

    private val _currentBid = MutableStateFlow(0)
    val currentBid: StateFlow<Int> = _currentBid.asStateFlow()

    private val _highestBidder = MutableStateFlow<Player?>(null)
    val highestBidder: StateFlow<Player?> = _highestBidder.asStateFlow()

    private val _merchantPot = MutableStateFlow(0)
    val merchantPot: StateFlow<Int> = _merchantPot.asStateFlow()

    // Settings State
    private val _playerName = MutableStateFlow(settingsManager.getPlayerName())
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    private val _themeMode = MutableStateFlow(settingsManager.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private var merchantCardsRemaining = mutableListOf<CardModel>()
    private var cardsToAuction = mutableListOf<CardModel>()

    fun startSinglePlayer() {
        initializeGame(isFirstTime = true)
    }

    private fun initializeGame(isFirstTime: Boolean = false) {
        viewModelScope.launch {
            val fullDeck = DeckManager.createFullDeck()
            _merchantDeck.value = fullDeck
            merchantCardsRemaining = fullDeck.toMutableList()
            
            val playerDeck = fullDeck.shuffled().toMutableList()
            
            val humanCards = playerDeck.take(10)
            playerDeck.removeAll(humanCards)
            val ai1Cards = playerDeck.take(10)
            playerDeck.removeAll(ai1Cards)
            val ai2Cards = playerDeck.take(10)
            playerDeck.removeAll(ai2Cards)
            
            cardsToAuction = playerDeck.take(10).toMutableList()
            
            if (isFirstTime) {
                val human = Player(1, _playerName.value, isHuman = true, cards = humanCards, money = 100)
                val ai1 = Player(2, "IA 1", isHuman = false, cards = ai1Cards, money = 100)
                val ai2 = Player(3, "IA 2", isHuman = false, cards = ai2Cards, money = 100)
                _players.value = listOf(human, ai1, ai2)
            } else {
                _players.update { currentPlayers ->
                    if (currentPlayers.isEmpty()) return@update currentPlayers
                    listOf(
                        currentPlayers[0].copy(name = _playerName.value, cards = humanCards),
                        currentPlayers[1].copy(cards = ai1Cards),
                        currentPlayers[2].copy(cards = ai2Cards)
                    )
                }
            }
            
            _merchantPot.value = 0
            _currentMessage.value = "Carte iniziali distribuite! Inizia l'asta per le rimanenti."
            _gameState.value = GamePhase.AUCTION
            startNextAuction()
        }
    }

    private fun startNextAuction() {
        if (cardsToAuction.isEmpty()) {
            _gameState.value = GamePhase.DISTRIBUTION
            _currentMessage.value = "Asta terminata! Il Mercante ha raccolto ${_merchantPot.value} €. Ora stabiliamo i premi."
            _auctionCard.value = null
            return
        }

        val card = cardsToAuction.removeAt(0)
        _auctionCard.value = card
        _currentBid.value = 0
        _highestBidder.value = null
        _currentMessage.value = "All'asta la carta: ${card.name}. Chi offre di più?"
        
        viewModelScope.launch {
            delay(2000)
            simulateAiBidding()
        }
    }

    private suspend fun simulateAiBidding() {
        if (_gameState.value != GamePhase.AUCTION) return

        var active = true
        while (active) {
            delay(1500)
            val currentHighest = _currentBid.value
            val aiPlayers = _players.value.filter { !it.isHuman }
            
            val bidder = aiPlayers.filter { it.money > currentHighest + 2 }.shuffled().firstOrNull {
                Random.nextInt(100) > 60 
            }

            if (bidder != null) {
                val newBid = currentHighest + Random.nextInt(1, 5)
                if (newBid <= bidder.money) {
                    _currentBid.value = newBid
                    _highestBidder.value = bidder
                    _currentMessage.value = "${bidder.name} offre $newBid €!"
                } else {
                    active = false
                }
            } else {
                active = false
            }
        }
        
        delay(1000)
        _currentMessage.value = "Andata! A ${_highestBidder.value?.name ?: "nessuno"} perLoad card images ${_currentBid.value} €"
        delay(1000)
        concludeAuction()
    }

    fun playerBid() {
        if (_gameState.value != GamePhase.AUCTION) return
        val human = _players.value.find { it.isHuman } ?: return
        val newBid = _currentBid.value + 5
        
        if (newBid <= human.money) {
            _currentBid.value = newBid
            _highestBidder.value = human
            _currentMessage.value = "Hai offerto $newBid €!"
        } else {
            _currentMessage.value = "Non hai abbastanza soldi!"
        }
    }

    private fun concludeAuction() {
        val winner = _highestBidder.value
        val bid = _currentBid.value
        val card = _auctionCard.value

        if (winner != null && card != null) {
            _players.update { currentPlayers ->
                currentPlayers.map { p ->
                    if (p.id == winner.id) {
                        p.copy(money = p.money - bid, cards = p.cards + card)
                    } else p
                }
            }
            _merchantPot.value += bid
        }

        startNextAuction()
    }
    
    fun startPrizesPhase() {
        viewModelScope.launch {
            _gameState.value = GamePhase.PRIZES
            
            val pot = _merchantPot.value
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
            val pool = merchantCardsRemaining.shuffled().toMutableList()
            for (value in prizeValues) {
                if (pool.isNotEmpty()) {
                    val card = pool.removeAt(0)
                    selectedPrizes.add(Prize(card, value))
                    merchantCardsRemaining.remove(card)
                }
            }
            
            _prizes.value = selectedPrizes
            _currentMessage.value = "Premi stabiliti in base all'asta. Iniziamo l'eliminazione!"
            delay(2000)
            _gameState.value = GamePhase.ELIMINATION
        }
    }
    
    fun drawEliminationCard() {
        if (_gameState.value != GamePhase.ELIMINATION) return
        
        viewModelScope.launch {
            if (merchantCardsRemaining.isNotEmpty()) {
                val card = merchantCardsRemaining.removeAt(Random.nextInt(merchantCardsRemaining.size))
                _eliminatedCards.update { it + card.id }
                _currentMessage.value = "È uscita la carta: ${card.name}. Chi ce l'ha è eliminato!"
                
                _players.update { currentPlayers ->
                    currentPlayers.map { player ->
                        player.copy(cards = player.cards.filter { it.id != card.id })
                    }
                }
                
                if (merchantCardsRemaining.isEmpty()) {
                    finishGame()
                }
            }
        }
    }

    private fun finishGame() {
        _gameState.value = GamePhase.FINISHED
        _currentMessage.value = "Gioco terminato! Ecco le vincite finali."
        
        _players.update { currentPlayers ->
            currentPlayers.map { player ->
                var roundWinnings = 0
                player.cards.forEach { card ->
                    val prize = _prizes.value.find { it.card.id == card.id }
                    if (prize != null) {
                        roundWinnings += prize.value
                    }
                }
                player.copy(money = player.money + roundWinnings)
            }
        }
    }

    fun resetGame() {
        _gameState.value = GamePhase.DISTRIBUTION
        _prizes.value = emptyList()
        _eliminatedCards.value = emptySet()
        _inspectingPlayer.value = null
        _tradeDialogTarget.value = null
        _offeringCard.value = null
        _auctionCard.value = null
        _merchantPot.value = 0
        initializeGame(isFirstTime = false)
    }

    fun goToMenu() {
        _gameState.value = GamePhase.MENU
    }

    fun goToSettings() {
        _gameState.value = GamePhase.SETTINGS
    }

    fun updatePlayerName(newName: String) {
        _playerName.value = newName
        settingsManager.savePlayerName(newName)
    }

    fun updateThemeMode(newMode: String) {
        _themeMode.value = newMode
        settingsManager.saveThemeMode(newMode)
    }

    fun inspectPlayer(player: Player) {
        _inspectingPlayer.value = player
    }

    fun stopInspecting() {
        _inspectingPlayer.value = null
    }

    fun openTradeDialog(player: Player, card: CardModel) {
        _tradeDialogTarget.value = player to card
    }

    fun closeTradeDialog() {
        _tradeDialogTarget.value = null
    }

    fun openOfferDialog(card: CardModel) {
        _offeringCard.value = card
    }

    fun closeOfferDialog() {
        _offeringCard.value = null
    }

    fun proposeMoneyTrade(targetPlayer: Player, targetCard: CardModel, moneyOffer: Int) {
        viewModelScope.launch {
            val human = _players.value.find { it.isHuman } ?: return@launch
            
            if (human.money < moneyOffer) {
                _currentMessage.value = "Non hai abbastanza soldi!"
                return@launch
            }

            val accepted = Random.nextBoolean() 

            if (accepted) {
                _players.update { currentPlayers ->
                    currentPlayers.map { p ->
                        when (p.id) {
                            human.id -> p.copy(
                                money = p.money - moneyOffer,
                                cards = p.cards + targetCard
                            )
                            targetPlayer.id -> p.copy(
                                money = p.money + moneyOffer,
                                cards = p.cards - targetCard
                            )
                            else -> p
                        }
                    }
                }
                _currentMessage.value = "${targetPlayer.name} ha accettato lo scambio per $moneyOffer €!"
            } else {
                _currentMessage.value = "${targetPlayer.name} ha rifiutato l'offerta."
            }
            closeTradeDialog()
            stopInspecting()
        }
    }

    fun proposeCardTrade(targetPlayer: Player, targetCard: CardModel, offeredCard: CardModel) {
        viewModelScope.launch {
            val human = _players.value.find { it.isHuman } ?: return@launch
            val accepted = Random.nextBoolean()

            if (accepted) {
                _players.update { currentPlayers ->
                    currentPlayers.map { p ->
                        when (p.id) {
                            human.id -> p.copy(
                                cards = (p.cards - offeredCard) + targetCard
                            )
                            targetPlayer.id -> p.copy(
                                cards = (p.cards - targetCard) + offeredCard
                            )
                            else -> p
                        }
                    }
                }
                _currentMessage.value = "${targetPlayer.name} ha scambiato ${targetCard.name} con ${offeredCard.name}!"
            } else {
                _currentMessage.value = "${targetPlayer.name} ha rifiutato lo scambio di carte."
            }
            closeTradeDialog()
            stopInspecting()
        }
    }

    fun sellCardForMoney(targetPlayer: Player, myCard: CardModel, moneyRequested: Int) {
        viewModelScope.launch {
            if (targetPlayer.money < moneyRequested) {
                _currentMessage.value = "${targetPlayer.name} non ha abbastanza soldi!"
                closeOfferDialog()
                return@launch
            }

            val accepted = Random.nextBoolean() // IA logic

            if (accepted) {
                _players.update { currentPlayers ->
                    currentPlayers.map { p ->
                        when (p.id) {
                            1 -> p.copy(
                                money = p.money + moneyRequested,
                                cards = p.cards - myCard
                            )
                            targetPlayer.id -> p.copy(
                                money = p.money - moneyRequested,
                                cards = p.cards + myCard
                            )
                            else -> p
                        }
                    }
                }
                _currentMessage.value = "${targetPlayer.name} ha comprato ${myCard.name} per $moneyRequested €!"
            } else {
                _currentMessage.value = "${targetPlayer.name} non è interessato all'acquisto."
            }
            closeOfferDialog()
        }
    }

    fun swapCardForCard(targetPlayer: Player, myCard: CardModel, targetCard: CardModel) {
        viewModelScope.launch {
            val accepted = Random.nextBoolean() // IA logic

            if (accepted) {
                _players.update { currentPlayers ->
                    currentPlayers.map { p ->
                        when (p.id) {
                            1 -> p.copy(
                                cards = (p.cards - myCard) + targetCard
                            )
                            targetPlayer.id -> p.copy(
                                cards = (p.cards - targetCard) + myCard
                            )
                            else -> p
                        }
                    }
                }
                _currentMessage.value = "${targetPlayer.name} ha accettato lo scambio: ${myCard.name} per ${targetCard.name}!"
            } else {
                _currentMessage.value = "${targetPlayer.name} ha rifiutato lo scambio di carte."
            }
            closeOfferDialog()
        }
    }
}