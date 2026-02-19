package com.example.mercanteinfiera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.lifecycle.viewmodel.compose.viewModel
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

    val humanPlayer = players.find { it.isHuman }
    val aiPlayers = players.filter { !it.isHuman }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header con Titolo e Reset
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
                OpponentInfo(ai, modifier = Modifier.weight(1f))
            }
        }

        // Sezione Giocatore (Le tue carte)
        Divider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Le tue carte:", style = MaterialTheme.typography.titleSmall)
            if (humanPlayer != null && humanPlayer.winnings > 0) {
                Text(
                    "Hai vinto: ${humanPlayer.winnings} €",
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
                        Button(
                            onClick = { viewModel.startPrizesPhase() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stabilisci Premi")
                        }
                    }
                    GamePhase.ELIMINATION -> {
                        Button(
                            onClick = { viewModel.drawEliminationCard() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pesca Carta dal Mercante")
                        }
                    }
                    GamePhase.FINISHED -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Gioco Terminato!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(onClick = { viewModel.resetGame() }) {
                                Text("Inizia Nuova Partita")
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
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
            Text(
                player.name, 
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${player.cards.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (player.winnings > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${player.winnings}€",
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text("Carte", style = MaterialTheme.typography.labelSmall)
        }
    }
}