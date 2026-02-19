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

    private var merchantCardsRemaining = mutableListOf<CardModel>()

    init {
        initializeGame()
    }
    
    private fun initializeGame() {
        viewModelScope.launch {
            val fullDeck = DeckManager.createFullDeck()
            _merchantDeck.value = fullDeck
            merchantCardsRemaining = fullDeck.toMutableList()
            
            val playerDeck = fullDeck.shuffled()
            
            val human = Player(1, "Tu (Giocatore)", isHuman = true, cards = playerDeck.subList(0, 13))
            val ai1 = Player(2, "IA 1", isHuman = false, cards = playerDeck.subList(13, 26))
            val ai2 = Player(3, "IA 2", isHuman = false, cards = playerDeck.subList(26, 39))
            
            _players.value = listOf(human, ai1, ai2)
            _currentMessage.value = "Carte distribuite! Ora scegliamo i premi."
        }
    }
    
    fun startPrizesPhase() {
        viewModelScope.launch {
            _gameState.value = GamePhase.PRIZES
            val prizeValues = listOf(50, 30, 20, 10, 5)
            val selectedPrizes = mutableListOf<Prize>()
            
            // Il mercante sceglie 5 carte a caso come premi e le toglie dal mazzo delle eliminazioni
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
                
                // Rimuovi la carta dai giocatori
                _players.update { currentPlayers ->
                    currentPlayers.map { player ->
                        player.copy(cards = player.cards.filter { it.id != card.id })
                    }
                }
                
                if (merchantCardsRemaining.isEmpty()) {
                    _gameState.value = GamePhase.FINISHED
                    _currentMessage.value = "Gioco terminato! Vediamo chi ha vinto i premi."
                }
            }
        }
    }
}