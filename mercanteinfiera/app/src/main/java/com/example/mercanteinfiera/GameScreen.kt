package com.example.mercanteinfiera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mercanteinfiera.models.CardModel
import com.example.mercanteinfiera.models.GamePhase
import com.example.mercanteinfiera.models.Player
import com.example.mercanteinfiera.models.Prize
import com.example.mercanteinfiera.ui.GameCard
import com.example.mercanteinfiera.viewmodel.GameViewModel

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsState()
    val players by viewModel.players.collectAsState()
    val prizes by viewModel.prizes.collectAsState()
    val currentMessage by viewModel.currentMessage.collectAsState()
    val eliminatedCards by viewModel.eliminatedCards.collectAsState()
    val inspectingPlayer by viewModel.inspectingPlayer.collectAsState()
    val tradeDialogTarget by viewModel.tradeDialogTarget.collectAsState()
    val offeringCard by viewModel.offeringCard.collectAsState()
    val playerName by viewModel.playerName.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    
    // Auction states
    val auctionCard by viewModel.auctionCard.collectAsState()
    val currentBid by viewModel.currentBid.collectAsState()
    val highestBidder by viewModel.highestBidder.collectAsState()
    val merchantPot by viewModel.merchantPot.collectAsState()

    val humanPlayer = players.find { it.isHuman }
    val aiPlayers = players.filter { !it.isHuman }

    when (gameState) {
        GamePhase.MENU -> {
            MainMenu(
                onSinglePlayerClick = { viewModel.startSinglePlayer() },
                onSettingsClick = { viewModel.goToSettings() }
            )
        }
        GamePhase.SETTINGS -> {
            SettingsScreen(
                currentName = playerName,
                currentTheme = themeMode,
                onNameChange = { viewModel.updatePlayerName(it) },
                onThemeChange = { viewModel.updateThemeMode(it) },
                onBackClick = { viewModel.goToMenu() }
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mercante in Fiera",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (humanPlayer != null) {
                            Text(
                                "Saldo: ${humanPlayer.money} €",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.goToMenu() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Torna al Menu", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }

                // Messaggio del Mercante
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = currentMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Sezione Asta
                if (gameState == GamePhase.AUCTION && auctionCard != null) {
                    AuctionPanel(
                        card = auctionCard!!,
                        currentBid = currentBid,
                        highestBidder = highestBidder,
                        onBidClick = { viewModel.playerBid() }
                    )
                }

                // Sezione Premi
                if (prizes.isNotEmpty()) {
                    Text("Premi in palio (Pot: $merchantPot €):", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(prizes) { prize ->
                            PrizeItem(prize, isRevealed = gameState == GamePhase.FINISHED || eliminatedCards.contains(prize.card.id))
                        }
                    }
                }

                // Sezione Avversari
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    aiPlayers.forEach { ai ->
                        OpponentInfo(
                            ai, 
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.inspectPlayer(ai) }
                        )
                    }
                }

                // Sezione Giocatore
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Le tue carte:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)

                if (humanPlayer != null) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 70.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(humanPlayer.cards) { card ->
                            GameCard(
                                card = card,
                                isFaceUp = true,
                                modifier = Modifier
                                    .aspectRatio(3f / 4f)
                                    .clickable { viewModel.openOfferDialog(card) }
                            )
                        }
                    }
                }

                // Azioni
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    tonalElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (gameState) {
                            GamePhase.DISTRIBUTION -> {
                                Button(onClick = { viewModel.startPrizesPhase() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Stabilisci Premi")
                                }
                            }
                            GamePhase.AUCTION -> {
                                Button(onClick = { viewModel.playerBid() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Fai un'offerta (+5€)")
                                }
                            }
                            GamePhase.ELIMINATION -> {
                                Button(onClick = { viewModel.drawEliminationCard() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Pesca Carta dal Mercante")
                                }
                            }
                            GamePhase.FINISHED -> {
                                Button(onClick = { viewModel.resetGame() }) {
                                    Text("Inizia Nuova Partita")
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    // Dialoghi
    inspectingPlayer?.let { player ->
        InspectionDialog(
            player = player,
            onDismiss = { viewModel.stopInspecting() },
            onCardClick = { card -> viewModel.openTradeDialog(player, card) }
        )
    }

    tradeDialogTarget?.let { (targetPlayer, targetCard) ->
        TradeDialog(
            targetPlayer = targetPlayer,
            targetCard = targetCard,
            myCards = humanPlayer?.cards ?: emptyList(),
            myBalance = humanPlayer?.money ?: 0,
            onDismiss = { viewModel.closeTradeDialog() },
            onMoneyOffer = { amount -> viewModel.proposeMoneyTrade(targetPlayer, targetCard, amount) },
            onCardOffer = { card -> viewModel.proposeCardTrade(targetPlayer, targetCard, card) }
        )
    }

    offeringCard?.let { card ->
        OfferDialog(
            myCard = card,
            aiPlayers = aiPlayers,
            onDismiss = { viewModel.closeOfferDialog() },
            onSellClick = { target, amount -> viewModel.sellCardForMoney(target, card, amount) },
            onSwapClick = { target, targetCard -> viewModel.swapCardForCard(target, card, targetCard) }
        )
    }
}

@Composable
fun MainMenu(
    onSinglePlayerClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mercante in Fiera",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onSinglePlayerClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Giocatore singolo", modifier = Modifier.padding(8.dp), fontSize = 18.sp)
            }
            
            Button(
                onClick = { /* Non fa nulla */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Multigiocatore", modifier = Modifier.padding(8.dp), fontSize = 18.sp)
            }
            
            Button(
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Impostazioni", modifier = Modifier.padding(8.dp), fontSize = 18.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentName: String,
    currentTheme: String,
    onNameChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = "Impostazioni",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Profilo Giocatore",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Nome", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                TextField(
                    value = currentName,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Inserisci il tuo nome") }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Aspetto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Tema", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("System", "Light", "Dark").forEach { mode ->
                        FilterChip(
                            selected = currentTheme == mode,
                            onClick = { onThemeChange(mode) },
                            label = { Text(mode) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuctionPanel(
    card: CardModel,
    currentBid: Int,
    highestBidder: Player?,
    onBidClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GameCard(
                card = card,
                modifier = Modifier.size(60.dp, 80.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("All'asta!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Offerta attuale: $currentBid €", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    text = "Miglior offerente: ${highestBidder?.name ?: "Nessuno"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun InspectionDialog(player: Player, onDismiss: () -> Unit, onCardClick: (CardModel) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Carte di ${player.name}") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(300.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(player.cards) { card ->
                    GameCard(
                        card = card,
                        modifier = Modifier
                            .aspectRatio(3f / 4f)
                            .clickable { onCardClick(card) }
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Chiudi") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferDialog(
    myCard: CardModel,
    aiPlayers: List<Player>,
    onDismiss: () -> Unit,
    onSellClick: (Player, Int) -> Unit,
    onSwapClick: (Player, CardModel) -> Unit
) {
    var selectedTarget by remember { mutableStateOf(aiPlayers.firstOrNull()) }
    var requestAmount by remember { mutableStateOf("20") }
    var isSwapMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offri ${myCard.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("A chi vuoi offrire questa carta?")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    aiPlayers.forEach { player ->
                        FilterChip(
                            selected = selectedTarget == player,
                            onClick = { selectedTarget = player },
                            label = { Text(player.name) }
                        )
                    }
                }

                Divider()

                selectedTarget?.let { target ->
                    if (!isSwapMode) {
                        Text("Chiedi soldi (Saldo ${target.name}: ${target.money} €):")
                        TextField(
                            value = requestAmount,
                            onValueChange = { requestAmount = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = { isSwapMode = true }) {
                            Text("Chiedi una carta in cambio")
                        }
                    } else {
                        Text("Scegli una carta di ${target.name} da chiedere:")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(target.cards) { card ->
                                GameCard(
                                    card = card,
                                    modifier = Modifier
                                        .size(60.dp, 80.dp)
                                        .clickable { onSwapClick(target, card) }
                                )
                            }
                        }
                        TextButton(onClick = { isSwapMode = false }) {
                            Text("Chiedi soldi invece")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isSwapMode) {
                Button(onClick = { selectedTarget?.let { onSellClick(it, requestAmount.toIntOrNull() ?: 0) } }) {
                    Text("Offri per ${requestAmount} €")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
fun TradeDialog(
    targetPlayer: Player,
    targetCard: CardModel,
    myCards: List<CardModel>,
    myBalance: Int,
    onDismiss: () -> Unit,
    onMoneyOffer: (Int) -> Unit,
    onCardOffer: (CardModel) -> Unit
) {
    var offerAmount by remember { mutableStateOf("10") }
    var showCardTrade by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scambia con ${targetPlayer.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Vuoi la carta: ${targetCard.name}?")
                
                if (!showCardTrade) {
                    Text("Offri soldi (Saldo: $myBalance €):")
                    TextField(
                        value = offerAmount,
                        onValueChange = { offerAmount = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = { showCardTrade = true }
                    ) {
                        Text("Vuoi offrire una carta invece?")
                    }
                } else {
                    Text("Scegli una tua carta da offrire:")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(myCards) { card ->
                            GameCard(
                                card = card,
                                modifier = Modifier
                                    .size(60.dp, 80.dp)
                                    .clickable { onCardOffer(card) }
                            )
                        }
                    }
                    TextButton(onClick = { showCardTrade = false }) {
                        Text("Torna all'offerta in denaro")
                    }
                }
            }
        },
        confirmButton = {
            if (!showCardTrade) {
                Button(onClick = { onMoneyOffer(offerAmount.toIntOrNull() ?: 0) }) {
                    Text("Offri ${offerAmount} €")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
fun PrizeItem(prize: Prize, isRevealed: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        GameCard(
            card = prize.card,
            isFaceUp = isRevealed,
            modifier = Modifier.size(50.dp, 70.dp)
        )
        Text(
            text = "${prize.value} €",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isRevealed) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun OpponentInfo(player: Player, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(player.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${player.cards.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("${player.money}€", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
            Text("Carte", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}