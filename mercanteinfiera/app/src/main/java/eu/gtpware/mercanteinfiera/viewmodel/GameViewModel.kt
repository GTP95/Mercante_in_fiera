package eu.gtpware.mercanteinfiera.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.gtpware.mercanteinfiera.R
import eu.gtpware.mercanteinfiera.data.DeckManager
import eu.gtpware.mercanteinfiera.data.SettingsManager
import eu.gtpware.mercanteinfiera.models.*
import kotlinx.coroutines.Job
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
    
    private val _currentMessage = MutableStateFlow(application.getString(R.string.msg_benvenuti))
    val currentMessage: StateFlow<String> = _currentMessage.asStateFlow()

    private val _inspectingPlayer = MutableStateFlow<Player?>(null)
    val inspectingPlayer: StateFlow<Player?> = _inspectingPlayer.asStateFlow()

    private val _tradeDialogTarget = MutableStateFlow<Pair<Player, CardModel>?>(null)
    val tradeDialogTarget: StateFlow<Pair<Player, CardModel>?> = _tradeDialogTarget.asStateFlow()

    private val _offeringCard = MutableStateFlow<CardModel?>(null)
    val offeringCard: StateFlow<CardModel?> = _offeringCard.asStateFlow()

    private val _aiProposal = MutableStateFlow<AIProposal?>(null)
    val aiProposal: StateFlow<AIProposal?> = _aiProposal.asStateFlow()

    private val lastProposalTimes = mutableMapOf<String, Long>()

    // Auction State
    private val _auctionCard = MutableStateFlow<CardModel?>(null)
    val auctionCard: StateFlow<CardModel?> = _auctionCard.asStateFlow()

    private val _currentBid = MutableStateFlow(0)
    val currentBid: StateFlow<Int> = _currentBid.asStateFlow()

    private val _highestBidder = MutableStateFlow<Player?>(null)
    val highestBidder: StateFlow<Player?> = _highestBidder.asStateFlow()

    private val _merchantPot = MutableStateFlow(0)
    val merchantPot: StateFlow<Int> = _merchantPot.asStateFlow()

    private val _auctionTimer = MutableStateFlow(0)
    val auctionTimer: StateFlow<Int> = _auctionTimer.asStateFlow()

    private var auctionTimerJob: Job? = null

    // Settings State
    private val _playerName = MutableStateFlow(settingsManager.getPlayerName())
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    private val _themeMode = MutableStateFlow(settingsManager.getThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(settingsManager.getLanguage())
    val language: StateFlow<String> = _language.asStateFlow()

    private val _showTutorial = MutableStateFlow(false)
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    private var merchantCardsRemaining = mutableListOf<CardModel>()
    private var cardsToAuction = mutableListOf<CardModel>()

    init {
        startAIProposalLoop()
    }

    private fun getString(resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }

    private fun startAIProposalLoop() {
        viewModelScope.launch {
            while (true) {
                delay(2000)
                if (_gameState.value == GamePhase.ELIMINATION && _aiProposal.value == null && _tradeDialogTarget.value == null && _offeringCard.value == null) {
                    tryProposeAIExchange()
                }
            }
        }
    }

    private fun tryProposeAIExchange() {
        val currentTime = System.currentTimeMillis()
        val aiPlayers = _players.value.filter { !it.isHuman }
        val player = _players.value.find { it.isHuman } ?: return
        
        for (ai in aiPlayers) {
            val lastTime = lastProposalTimes[ai.id] ?: 0L
            if (currentTime - lastTime > 10000) {
                if (Random.nextInt(100) < 15) { // 15% chance to propose every 2s if cooled down
                    generateAIProposal(ai, player)
                    lastProposalTimes[ai.id] = currentTime
                    return
                }
            }
        }
    }

    private fun generateAIProposal(ai: Player, player: Player) {
        val type = Random.nextInt(3)
        when (type) {
            0 -> { // Buy from player
                if (player.cards.isNotEmpty() && ai.money >= 10) {
                    val card = player.cards.random()
                    val price = Random.nextInt(5, (ai.money / 2).coerceAtLeast(10))
                    _aiProposal.value = AIProposal.Buy(ai, card, price)
                }
            }
            1 -> { // Sell to player
                if (ai.cards.isNotEmpty() && player.money >= 10) {
                    val card = ai.cards.random()
                    val price = Random.nextInt(5, (player.money / 2).coerceAtLeast(10))
                    _aiProposal.value = AIProposal.Sell(ai, card, price)
                }
            }
            2 -> { // Exchange
                if (ai.cards.isNotEmpty() && player.cards.isNotEmpty()) {
                    val aiCard = ai.cards.random()
                    val playerCard = player.cards.random()
                    _aiProposal.value = AIProposal.Exchange(ai, aiCard, playerCard)
                }
            }
        }
    }

    fun acceptAIProposal() {
        val proposal = _aiProposal.value ?: return
        _players.update { currentPlayers ->
            val human = currentPlayers.find { it.isHuman } ?: return@update currentPlayers
            val ai = currentPlayers.find { it.id == proposal.getAI().id } ?: return@update currentPlayers
            
            when (proposal) {
                is AIProposal.Buy -> {
                    currentPlayers.map { p ->
                        when (p.id) {
                            human.id -> p.copy(money = p.money + proposal.price, cards = p.cards - proposal.card)
                            ai.id -> p.copy(money = p.money - proposal.price, cards = p.cards + proposal.card)
                            else -> p
                        }
                    }
                }
                is AIProposal.Sell -> {
                    currentPlayers.map { p ->
                        when (p.id) {
                            human.id -> p.copy(money = p.money - proposal.price, cards = p.cards + proposal.card)
                            ai.id -> p.copy(money = p.money + proposal.price, cards = p.cards - proposal.card)
                            else -> p
                        }
                    }
                }
                is AIProposal.Exchange -> {
                    currentPlayers.map { p ->
                        when (p.id) {
                            human.id -> p.copy(cards = (p.cards - proposal.playerCard) + proposal.aiCard)
                            ai.id -> p.copy(cards = (p.cards - proposal.aiCard) + proposal.playerCard)
                            else -> p
                        }
                    }
                }
            }
        }
        _aiProposal.value = null
    }

    fun rejectAIProposal() {
        _aiProposal.value = null
    }

    private fun AIProposal.getAI() = when(this) {
        is AIProposal.Buy -> ai
        is AIProposal.Sell -> ai
        is AIProposal.Exchange -> ai
    }

    fun startSinglePlayer() {
        if (!settingsManager.hasSeenTutorial()) {
            _showTutorial.value = true
        }
        initializeGame(isFirstTime = true)
    }

    fun markTutorialSeen() {
        settingsManager.setTutorialSeen()
        _showTutorial.value = false
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
                val human = Player("1", _playerName.value, isHuman = true, cards = humanCards, money = 100)
                val ai1 = Player("2", getString(R.string.ai_name_1), isHuman = false, cards = ai1Cards, money = 100)
                val ai2 = Player("3", getString(R.string.ai_name_2), isHuman = false, cards = ai2Cards, money = 100)
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
            _currentMessage.value = getString(R.string.msg_carte_distribuite)
            _gameState.value = GamePhase.AUCTION
            startNextAuction()
        }
    }

    private fun startNextAuction() {
        if (cardsToAuction.isEmpty()) {
            _gameState.value = GamePhase.DISTRIBUTION
            _currentMessage.value = getString(R.string.msg_asta_terminata, _merchantPot.value)
            _auctionCard.value = null
            return
        }

        val card = cardsToAuction.removeAt(0)
        _auctionCard.value = card
        _currentBid.value = 0
        _highestBidder.value = null
        _currentMessage.value = getString(R.string.msg_all_asta_carta, card.name)
        
        startAuctionTimer()
        
        viewModelScope.launch {
            delay(2000)
            simulateAiBidding()
        }
    }

    private fun startAuctionTimer() {
        auctionTimerJob?.cancel()
        _auctionTimer.value = 10 // 10 seconds for each card
        auctionTimerJob = viewModelScope.launch {
            while (_auctionTimer.value > 0) {
                delay(1000)
                _auctionTimer.value -= 1
            }
            if (_gameState.value == GamePhase.AUCTION) {
                _currentMessage.value = getString(R.string.msg_andata, _highestBidder.value?.name ?: getString(R.string.nessuno), _currentBid.value)
                delay(1000)
                concludeAuction()
            }
        }
    }

    private suspend fun simulateAiBidding() {
        if (_gameState.value != GamePhase.AUCTION) return

        var active = true
        while (active && _auctionTimer.value > 2) { // Bots only bid if there is time
            delay(1500)
            if (_auctionTimer.value <= 2) break

            val currentHighest = _currentBid.value
            val aiPlayers = _players.value.filter { !it.isHuman }
            
            // AI won't bid if they are already the highest bidder
            val bidder = aiPlayers.filter { it.id != _highestBidder.value?.id && it.money > currentHighest + 5 }.shuffled().firstOrNull {
                Random.nextInt(100) > 60 
            }

            if (bidder != null) {
                val newBid = currentHighest + 5 // AI now bids exact increment
                if (newBid <= bidder.money) {
                    _currentBid.value = newBid
                    _highestBidder.value = bidder
                    _currentMessage.value = getString(R.string.msg_offerta_ia, bidder.name, newBid)
                    resetAuctionTimer()
                } else {
                    active = false
                }
            } else {
                active = false
            }
        }
    }

    private fun resetAuctionTimer() {
        // Every time someone bids, we reset the timer to 10s
        _auctionTimer.value = 10
    }

    fun playerBid() {
        if (_gameState.value != GamePhase.AUCTION) return
        val human = _players.value.find { it.isHuman } ?: return
        
        // Prevent bidding if already the highest bidder
        if (_highestBidder.value?.id == human.id) {
            _currentMessage.value = getString(R.string.msg_sei_gia_miglior_offerente)
            return
        }

        val newBid = _currentBid.value + 5
        
        if (newBid <= human.money) {
            _currentBid.value = newBid
            _highestBidder.value = human
            _currentMessage.value = getString(R.string.msg_hai_offerto, newBid)
            resetAuctionTimer()
        } else {
            _currentMessage.value = getString(R.string.msg_non_abbastanza_soldi)
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
            _currentMessage.value = getString(R.string.msg_premi_stabiliti)
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
                _currentMessage.value = getString(R.string.msg_uscita_carta, card.name)
                
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
        _currentMessage.value = getString(R.string.msg_gioco_terminato)
        
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
        _aiProposal.value = null
        _merchantPot.value = 0
        initializeGame(isFirstTime = false)
    }

    fun goToMenu() {
        _gameState.value = GamePhase.MENU
    }

    fun goToSettings() {
        _gameState.value = GamePhase.SETTINGS
    }

    fun goToMultiplayerMenu() {
        _gameState.value = GamePhase.MULTIPLAYER_MENU
    }

    fun goToLobby() {
        _gameState.value = GamePhase.LOBBY
    }

    fun updatePlayerName(newName: String) {
        _playerName.value = newName
        settingsManager.savePlayerName(newName)
    }

    fun updateThemeMode(newMode: String) {
        _themeMode.value = newMode
        settingsManager.saveThemeMode(newMode)
    }

    fun updateLanguage(newLang: String) {
        _language.value = newLang
        settingsManager.saveLanguage(newLang)
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
                _currentMessage.value = getString(R.string.msg_non_abbastanza_soldi)
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
                _currentMessage.value = getString(R.string.msg_scambio_accettato, targetPlayer.name, moneyOffer)
            } else {
                _currentMessage.value = getString(R.string.msg_scambio_rifiutato, targetPlayer.name)
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
                _currentMessage.value = getString(R.string.msg_scambio_carte_accettato, targetPlayer.name, targetCard.name, offeredCard.name)
            } else {
                _currentMessage.value = getString(R.string.msg_scambio_carte_rifiutato, targetPlayer.name)
            }
            closeTradeDialog()
            stopInspecting()
        }
    }

    fun sellCardForMoney(targetPlayer: Player, myCard: CardModel, moneyRequested: Int) {
        viewModelScope.launch {
            if (targetPlayer.money < moneyRequested) {
                _currentMessage.value = getString(R.string.msg_non_abbastanza_soldi) // Or more specific message if added
                closeOfferDialog()
                return@launch
            }

            val accepted = Random.nextBoolean() // IA logic

            if (accepted) {
                _players.update { currentPlayers ->
                    currentPlayers.map { p ->
                        when (p.id) {
                            "1" -> p.copy(
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
                _currentMessage.value = getString(R.string.msg_acquisto_accettato, targetPlayer.name, myCard.name, moneyRequested)
            } else {
                _currentMessage.value = getString(R.string.msg_acquisto_rifiutato, targetPlayer.name)
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
                            "1" -> p.copy(
                                cards = (p.cards - myCard) + targetCard
                            )
                            targetPlayer.id -> p.copy(
                                cards = (p.cards - targetCard) + myCard
                            )
                            else -> p
                        }
                    }
                }
                _currentMessage.value = getString(R.string.msg_scambio_proposto_accettato, targetPlayer.name, myCard.name, targetCard.name)
            } else {
                _currentMessage.value = getString(R.string.msg_scambio_carte_rifiutato, targetPlayer.name)
            }
            closeOfferDialog()
        }
    }
}
