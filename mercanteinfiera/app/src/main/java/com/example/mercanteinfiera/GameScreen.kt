package com.example.mercanteinfiera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mercanteinfiera.models.*
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
    val aiProposal by viewModel.aiProposal.collectAsState()
    val playerName by viewModel.playerName.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    
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
                currentLanguage = language,
                onNameChange = { viewModel.updatePlayerName(it) },
                onThemeChange = { viewModel.updateThemeMode(it) },
                onLanguageChange = { viewModel.updateLanguage(it) },
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
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (humanPlayer != null) {
                            Text(
                                stringResource(R.string.saldo, humanPlayer.money),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.goToMenu() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.torna_al_menu), tint = MaterialTheme.colorScheme.onBackground)
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
                    Text(stringResource(R.string.premi_in_palio, merchantPot), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
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
                Text(stringResource(R.string.le_tue_carte), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)

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
                                    Text(stringResource(R.string.stabilisci_premi))
                                }
                            }
                            GamePhase.AUCTION -> {
                                Button(onClick = { viewModel.playerBid() }, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.fai_un_offerta))
                                }
                            }
                            GamePhase.ELIMINATION -> {
                                Button(onClick = { viewModel.drawEliminationCard() }, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.pesca_carta))
                                }
                            }
                            GamePhase.FINISHED -> {
                                Button(onClick = { viewModel.resetGame() }) {
                                    Text(stringResource(R.string.nuova_partita))
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

    aiProposal?.let { proposal ->
        AIProposalDialog(
            proposal = proposal,
            onAccept = { viewModel.acceptAIProposal() },
            onReject = { viewModel.rejectAIProposal() }
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
            text = stringResource(R.string.app_name),
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
                Text(stringResource(R.string.giocatore_singolo), modifier = Modifier.padding(8.dp), fontSize = 18.sp)
            }
            
            Button(
                onClick = { /* Non fa nulla */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.multigiocatore), modifier = Modifier.padding(8.dp), fontSize = 18.sp)
            }
            
            Button(
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.impostazioni), modifier = Modifier.padding(8.dp), fontSize = 18.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentName: String,
    currentTheme: String,
    currentLanguage: String,
    onNameChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
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
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.indietro), tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = stringResource(R.string.impostazioni),
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
                    text = stringResource(R.string.profilo_giocatore),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.nome), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                TextField(
                    value = currentName,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.inserisci_nome)) }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = stringResource(R.string.aspetto),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.tema), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        stringResource(R.string.theme_system) to "System",
                        stringResource(R.string.theme_light) to "Light",
                        stringResource(R.string.theme_dark) to "Dark"
                    )
                    themes.forEach { (label, mode) ->
                        FilterChip(
                            selected = currentTheme == mode,
                            onClick = { onThemeChange(mode) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = stringResource(R.string.lingua), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                
                var expanded by remember { mutableStateOf(false) }
                val languages = listOf(
                    "it" to stringResource(R.string.lingua_it),
                    "en" to stringResource(R.string.lingua_en)
                )
                val currentLangLabel = languages.find { it.first == currentLanguage }?.second ?: "Italiano"

                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(currentLangLabel)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        languages.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onLanguageChange(code)
                                    expanded = false
                                }
                            )
                        }
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
                Text(stringResource(R.string.all_asta), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.offerta_attuale, currentBid), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    text = stringResource(R.string.miglior_offerente, highestBidder?.name ?: stringResource(R.string.nessuno)),
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
        title = { Text(stringResource(R.string.carte_di, player.name)) },
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
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.chiudi)) } }
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
        title = { Text(stringResource(R.string.offri_carta, myCard.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.a_chi_offrire))
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
                        Text(stringResource(R.string.chiedi_soldi, target.name, target.money))
                        TextField(
                            value = requestAmount,
                            onValueChange = { requestAmount = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = { isSwapMode = true }) {
                            Text(stringResource(R.string.chiedi_carta_scambio))
                        }
                    } else {
                        Text(stringResource(R.string.scegli_carta_chiedere, target.name))
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
                            Text(stringResource(R.string.chiedi_soldi_invece))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isSwapMode) {
                Button(onClick = { selectedTarget?.let { onSellClick(it, requestAmount.toIntOrNull() ?: 0) } }) {
                    Text(stringResource(R.string.offri_per, requestAmount.toIntOrNull() ?: 0))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.annulla)) } }
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
        title = { Text(stringResource(R.string.scambia_con, targetPlayer.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.vuoi_la_carta, targetCard.name))
                
                if (!showCardTrade) {
                    Text(stringResource(R.string.offri_soldi, myBalance))
                    TextField(
                        value = offerAmount,
                        onValueChange = { offerAmount = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = { showCardTrade = true }
                    ) {
                        Text(stringResource(R.string.vuoi_offrire_carta_invece))
                    }
                } else {
                    Text(stringResource(R.string.scegli_tua_carta_offrire))
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
                        Text(stringResource(R.string.torna_offerta_denaro))
                    }
                }
            }
        },
        confirmButton = {
            if (!showCardTrade) {
                Button(onClick = { onMoneyOffer(offerAmount.toIntOrNull() ?: 0) }) {
                    Text(stringResource(R.string.offri_amount, offerAmount.toIntOrNull() ?: 0))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.annulla)) } }
    )
}

@Composable
fun AIProposalDialog(
    proposal: AIProposal,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        title = { Text(stringResource(R.string.titolo_proposta_ia, proposal.getAI().name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val message = when (proposal) {
                    is AIProposal.Buy -> stringResource(R.string.msg_ia_vuole_comprare, proposal.ai.name, proposal.card.name, proposal.price)
                    is AIProposal.Sell -> stringResource(R.string.msg_ia_vuole_vendere, proposal.ai.name, proposal.card.name, proposal.price)
                    is AIProposal.Exchange -> stringResource(R.string.msg_ia_vuole_scambiare, proposal.ai.name, proposal.aiCard.name, proposal.playerCard.name)
                }
                Text(message, textAlign = TextAlign.Center)
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (proposal) {
                        is AIProposal.Buy -> {
                            GameCard(card = proposal.card, isFaceUp = true, modifier = Modifier.size(60.dp, 80.dp))
                        }
                        is AIProposal.Sell -> {
                            GameCard(card = proposal.card, isFaceUp = true, modifier = Modifier.size(60.dp, 80.dp))
                        }
                        is AIProposal.Exchange -> {
                            GameCard(card = proposal.aiCard, isFaceUp = true, modifier = Modifier.size(60.dp, 80.dp))
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.align(Alignment.CenterVertically))
                            GameCard(card = proposal.playerCard, isFaceUp = true, modifier = Modifier.size(60.dp, 80.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAccept) { Text(stringResource(R.string.accetta)) }
        },
        dismissButton = {
            TextButton(onClick = onReject) { Text(stringResource(R.string.rifiuta)) }
        }
    )
}

private fun AIProposal.getAI() = when(this) {
    is AIProposal.Buy -> ai
    is AIProposal.Sell -> ai
    is AIProposal.Exchange -> ai
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
            Text(stringResource(R.string.carte), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}