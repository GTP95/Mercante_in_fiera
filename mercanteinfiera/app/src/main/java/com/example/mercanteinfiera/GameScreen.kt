package com.example.mercanteinfiera

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

    val humanPlayer = players.find { it.isHuman }
    val aiPlayers = players.filter { !it.isHuman }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mercante in Fiera",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (gameState == GamePhase.FINISHED) {
                IconButton(onClick = { viewModel.resetGame() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Nuova Partita")
                }
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

        // Sezione Premi
        if (prizes.isNotEmpty()) {
            Text("Premi in palio:", style = MaterialTheme.typography.titleSmall)
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
        Divider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Le tue carte:", style = MaterialTheme.typography.titleSmall)
            if (humanPlayer != null) {
                Text(
                    "Saldo: ${humanPlayer.money} €",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }
        }

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
                        modifier = Modifier.aspectRatio(3f / 4f)
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
            color = if (isRevealed) Color(0xFFD32F2F) else Color.DarkGray
        )
    }
}

@Composable
fun OpponentInfo(player: Player, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(player.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${player.cards.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("${player.money}€", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
            Text("Carte", style = MaterialTheme.typography.labelSmall)
        }
    }
}