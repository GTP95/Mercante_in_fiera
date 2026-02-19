package com.example.mercanteinfiera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mercanteinfiera.data.DeckManager
import com.example.mercanteinfiera.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel : ViewModel() {
    
    private val _gameState = MutableStateFlow(GamePhase.DISTRIBUTION)
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

    private var merchantCardsRemaining = mutableListOf<CardModel>()

    init {
        initializeGame(isFirstTime = true)
    }
    
    private fun initializeGame(isFirstTime: Boolean = false) {
        viewModelScope.launch {
            val fullDeck = DeckManager.createFullDeck()
            _merchantDeck.value = fullDeck
            merchantCardsRemaining = fullDeck.toMutableList()
            
            val playerDeck = fullDeck.shuffled()
            
            if (isFirstTime) {
                val human = Player(1, "Tu (Giocatore)", isHuman = true, cards = playerDeck.subList(0, 13), money = 100)
                val ai1 = Player(2, "IA 1", isHuman = false, cards = playerDeck.subList(13, 26), money = 100)
                val ai2 = Player(3, "IA 2", isHuman = false, cards = playerDeck.subList(26, 39), money = 100)
                _players.value = listOf(human, ai1, ai2)
            } else {
                _players.update { currentPlayers ->
                    currentPlayers.mapIndexed { index, player ->
                        val start = index * 13
                        val end = (index + 1) * 13
                        player.copy(cards = playerDeck.subList(start, end))
                    }
                }
            }
            
            _currentMessage.value = "Carte distribuite! Ora scegliamo i premi."
        }
    }
    
    fun startPrizesPhase() {
        viewModelScope.launch {
            _gameState.value = GamePhase.PRIZES
            val prizeValues = listOf(50, 30, 20, 10, 5)
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
            _currentMessage.value = "Premi stabiliti. Iniziamo l'eliminazione!"
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
        initializeGame(isFirstTime = false)
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