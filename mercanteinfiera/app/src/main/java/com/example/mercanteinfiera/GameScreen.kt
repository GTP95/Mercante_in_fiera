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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Messaggio del Mercante
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Text(
                text = currentMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Sezione Premi
        if (prizes.isNotEmpty()) {
            Text("Premi in palio:", style = MaterialTheme.typography.titleMedium)
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
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            aiPlayers.forEach { ai ->
                OpponentInfo(ai, modifier = Modifier.weight(1f))
            }
        }

        // Sezione Giocatore (Le tue carte)
        Text("Le tue carte:", style = MaterialTheme.typography.titleMedium)
        if (humanPlayer != null) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
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
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (gameState) {
                GamePhase.DISTRIBUTION -> {
                    Button(onClick = { viewModel.startPrizesPhase() }) {
                        Text("Stabilisci Premi")
                    }
                }
                GamePhase.ELIMINATION -> {
                    Button(onClick = { viewModel.drawEliminationCard() }) {
                        Text("Pesca Carta dal Mercante")
                    }
                }
                GamePhase.FINISHED -> {
                    Button(onClick = { /* Potresti resettare il gioco qui */ }) {
                        Text("Gioco Terminato")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun PrizeItem(prize: Prize, isRevealed: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        GameCard(
            card = prize.card,
            isFaceUp = isRevealed,
            modifier = Modifier.size(60.dp, 80.dp)
        )
        Text(
            text = "${prize.value} €",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
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
            Text(player.name, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.LightGray, CircleShape)
                    .border(1.dp, Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${player.cards.size}", fontSize = 14.sp)
            }
            Text("Carte", style = MaterialTheme.typography.labelSmall)
        }
    }
}