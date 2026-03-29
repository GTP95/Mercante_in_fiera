package eu.gtpware.mercanteinfiera

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.gtpware.mercanteinfiera.models.*
import eu.gtpware.mercanteinfiera.ui.GameCard
import eu.gtpware.mercanteinfiera.utils.SoundManager
import eu.gtpware.mercanteinfiera.utils.rememberClickable
import eu.gtpware.mercanteinfiera.viewmodel.GameViewModel
import eu.gtpware.mercanteinfiera.viewmodel.MultiplayerViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    multiplayerViewModel: MultiplayerViewModel = viewModel()
) {
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
    val showTutorial by viewModel.showTutorial.collectAsState()
    val isNewGameButtonEnabled by viewModel.isNewGameButtonEnabled.collectAsState()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    val difficultyLevel by viewModel.difficultyLevel.collectAsState()
    
    LaunchedEffect(isSoundEnabled) {
        SoundManager.setEnabled(isSoundEnabled)
    }
    
    // Multiplayer state
    val currentRoom by multiplayerViewModel.currentRoom.collectAsState()
    val mpError by multiplayerViewModel.error.collectAsState()
    val mpInspectingPlayer by multiplayerViewModel.inspectingPlayer.collectAsState()
    val mpTradeDialogTarget by multiplayerViewModel.tradeDialogTarget.collectAsState()
    val mpOfferingCard by multiplayerViewModel.offeringCard.collectAsState()

    // Auction states
    val auctionCard by viewModel.auctionCard.collectAsState()
    val currentBid by viewModel.currentBid.collectAsState()
    val highestBidder by viewModel.highestBidder.collectAsState()
    val merchantPot by viewModel.merchantPot.collectAsState()
    val auctionTimer by viewModel.auctionTimer.collectAsState()

    val humanPlayer = players.find { it.isHuman }
    val aiPlayers = players.filter { !it.isHuman }

    // Handle Multiplayer navigation
    LaunchedEffect(currentRoom?.status) {
        val room = currentRoom
        if (room != null) {
            if (room.status != RoomStatus.LOBBY && gameState == GamePhase.LOBBY) {
                // Transition to game phase handled by isMultiplayer check
            } else if (room.status == RoomStatus.LOBBY && (gameState == GamePhase.MULTIPLAYER_MENU || gameState == GamePhase.MENU)) {
                viewModel.goToLobby()
            }
        } else if (gameState == GamePhase.LOBBY) {
            viewModel.goToMenu()
        }
    }

    val isMultiplayer = currentRoom != null && currentRoom?.status != RoomStatus.LOBBY

    if (isMultiplayer) {
        MultiplayerGameLayout(
            room = currentRoom!!,
            onBackClick = { 
                multiplayerViewModel.leaveRoom()
                viewModel.goToMenu() 
            },
            multiplayerViewModel = multiplayerViewModel
        )
    } else {
        when (gameState) {
            GamePhase.MENU -> {
                MainMenu(
                    onSinglePlayerClick = { viewModel.startSinglePlayer() },
                    onMultiplayerClick = { viewModel.goToMultiplayerMenu() },
                    onSettingsClick = { viewModel.goToSettings() }
                )
            }
            GamePhase.SETTINGS -> {
                SettingsScreen(
                    currentName = playerName,
                    currentTheme = themeMode,
                    currentLanguage = language,
                    isSoundEnabled = isSoundEnabled,
                    currentDifficulty = difficultyLevel,
                    onNameChange = { viewModel.updatePlayerName(it) },
                    onThemeChange = { viewModel.updateThemeMode(it) },
                    onLanguageChange = { viewModel.updateLanguage(it) },
                    onSoundEnabledChange = { viewModel.updateSoundEnabled(it) },
                    onDifficultyChange = { viewModel.updateDifficultyLevel(it) },
                    onBackClick = { viewModel.goToMenu() }
                )
            }
            GamePhase.MULTIPLAYER_MENU -> {
                MultiplayerMenu(
                    playerName = playerName,
                    defaultName = stringResource(R.string.default_player_name),
                    onBackClick = { viewModel.goToMenu() },
                    onCreateRoom = { multiplayerViewModel.createRoom(it) },
                    onJoinRoom = { code, name -> multiplayerViewModel.joinRoom(code, name) },
                    onNameUpdate = { viewModel.updatePlayerName(it) },
                    errorMessage = mpError
                )
            }
            GamePhase.LOBBY -> {
                currentRoom?.let { room ->
                    LobbyScreen(
                        room = room,
                        onBackClick = { 
                            multiplayerViewModel.leaveRoom()
                            viewModel.goToMenu() 
                        },
                        onStartGame = { multiplayerViewModel.startGame() },
                        onReadyClick = { multiplayerViewModel.toggleReady() }
                    )
                } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                // Single player game layout
                SinglePlayerGameLayout(
                    humanPlayer = humanPlayer,
                    aiPlayers = aiPlayers,
                    gameState = gameState,
                    currentMessage = currentMessage,
                    auctionCard = auctionCard,
                    currentBid = currentBid,
                    highestBidder = highestBidder,
                    merchantPot = merchantPot,
                    auctionTimer = auctionTimer,
                    prizes = prizes,
                    eliminatedCards = eliminatedCards,
                    aiProposal = aiProposal,
                    isNewGameButtonEnabled = isNewGameButtonEnabled,
                    onBackClick = { viewModel.goToMenu() },
                    onBidClick = { viewModel.playerBid() },
                    onStartPrizes = { viewModel.startPrizesPhase() },
                    onDrawCard = { viewModel.drawEliminationCard() },
                    onNewGame = { viewModel.resetGame() },
                    onInspectPlayer = { viewModel.inspectPlayer(it) },
                    onOfferCard = { viewModel.openOfferDialog(it) }
                )
            }
        }
    }

    // Dialogs
    if (isMultiplayer) {
        mpInspectingPlayer?.let { player ->
            InspectionDialog(
                playerName = player.name,
                cards = player.cards,
                onDismiss = { multiplayerViewModel.stopInspecting() },
                onCardClick = { card -> multiplayerViewModel.openTradeDialog(player, card) }
            )
        }
        
        mpTradeDialogTarget?.let { (targetPlayer, targetCard) ->
            val me = currentRoom?.players?.get(Firebase.auth.currentUser?.uid)
            TradeDialog(
                targetPlayerName = targetPlayer.name,
                targetCard = targetCard,
                myCards = me?.cards ?: emptyList(),
                myBalance = me?.money ?: 0,
                onDismiss = { multiplayerViewModel.closeTradeDialog() },
                onMoneyOffer = { amount -> 
                    multiplayerViewModel.proposeMoneyTrade(targetPlayer, targetCard, amount)
                    multiplayerViewModel.closeTradeDialog()
                    multiplayerViewModel.stopInspecting()
                },
                onCardOffer = { card -> 
                    multiplayerViewModel.proposeCardTrade(targetPlayer, targetCard, card)
                    multiplayerViewModel.closeTradeDialog()
                    multiplayerViewModel.stopInspecting()
                }
            )
        }

        mpOfferingCard?.let { card ->
            val opponents = currentRoom?.players?.values?.filter { it.id != Firebase.auth.currentUser?.uid } ?: emptyList()
            OfferDialog(
                myCard = card,
                targetPlayers = opponents,
                onDismiss = { multiplayerViewModel.closeOfferDialog() },
                onSellClick = { target, amount -> 
                    if (target is RoomPlayer) {
                        multiplayerViewModel.sellCardForMoney(target, card, amount)
                    }
                    multiplayerViewModel.closeOfferDialog()
                },
                onSwapClick = { target, targetCard -> 
                    if (target is RoomPlayer) {
                        multiplayerViewModel.swapCardForCard(target, card, targetCard)
                    }
                    multiplayerViewModel.closeOfferDialog()
                }
            )
        }

        currentRoom?.tradeRequest?.let { request ->
            if (request.receiverId == Firebase.auth.currentUser?.uid) {
                TradeRequestDialog(
                    request = request,
                    room = currentRoom!!,
                    onAccept = { multiplayerViewModel.acceptTrade() },
                    onReject = { multiplayerViewModel.rejectTrade() }
                )
            }
        }
    } else {
        if (showTutorial) {
            TutorialDialog(
                onDismiss = { viewModel.markTutorialSeen() }
            )
        }

        inspectingPlayer?.let { player ->
            InspectionDialog(
                playerName = player.name,
                cards = player.cards,
                onDismiss = { viewModel.stopInspecting() },
                onCardClick = { card -> viewModel.openTradeDialog(player, card) }
            )
        }

        tradeDialogTarget?.let { (targetPlayer, targetCard) ->
            TradeDialog(
                targetPlayerName = targetPlayer.name,
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
                targetPlayers = aiPlayers,
                onDismiss = { viewModel.closeOfferDialog() },
                onSellClick = { target, amount -> 
                    if (target is Player) {
                        viewModel.sellCardForMoney(target, card, amount)
                    }
                },
                onSwapClick = { target, targetCard -> 
                    if (target is Player) {
                        viewModel.swapCardForCard(target, card, targetCard)
                    }
                }
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
}

@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    val onClick = rememberClickable(onDismiss)
    AlertDialog(
        onDismissRequest = onClick,
        title = {
            Text(
                text = stringResource(R.string.tutorial_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.tutorial_message)
            )
        },
        confirmButton = {
            Button(onClick = onClick) {
                Text(stringResource(R.string.ho_capito))
            }
        }
    )
}

@Composable
fun SinglePlayerGameLayout(
    humanPlayer: Player?,
    aiPlayers: List<Player>,
    gameState: GamePhase,
    currentMessage: String,
    auctionCard: CardModel?,
    currentBid: Int,
    highestBidder: Player?,
    merchantPot: Int,
    auctionTimer: Int,
    prizes: List<Prize>,
    eliminatedCards: Set<Int>,
    aiProposal: AIProposal?,
    isNewGameButtonEnabled: Boolean,
    onBackClick: () -> Unit,
    onBidClick: () -> Unit,
    onStartPrizes: () -> Unit,
    onDrawCard: () -> Unit,
    onNewGame: () -> Unit,
    onInspectPlayer: (Player) -> Unit,
    onOfferCard: (CardModel) -> Unit
) {
    val isWin = highestBidder?.id == humanPlayer?.id
    val onBackClicked = rememberClickable(onBackClick)
    val onBidClicked = rememberClickable(onBidClick)
    val onStartPrizesClicked = rememberClickable(onStartPrizes)
    val onDrawCardClicked = rememberClickable(onDrawCard)
    val onNewGameClicked = rememberClickable(onNewGame)

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
            IconButton(onClick = onBackClicked) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.torna_al_menu), tint = MaterialTheme.colorScheme.onBackground)
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
                card = auctionCard,
                currentBid = currentBid,
                highestBidderName = highestBidder?.name ?: stringResource(R.string.nessuno),
                timer = auctionTimer
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
                    val isWon = humanPlayer?.cards?.any { it.id == prize.card.id } == true
                    PrizeItem(
                        prize = prize,
                        isRevealed = gameState == GamePhase.FINISHED || eliminatedCards.contains(prize.card.id),
                        isWon = isWon
                    )
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
                    name = ai.name,
                    cardCount = ai.cards.size,
                    money = ai.money,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (gameState == GamePhase.ELIMINATION) {
                                Modifier.clickable(onClick = rememberClickable { onInspectPlayer(ai) })
                            } else Modifier
                        )
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
                            .aspectRatio(3f / 4f),
                        onClick = if (gameState == GamePhase.ELIMINATION) { { onOfferCard(card) } } else null
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
                        Button(onClick = onStartPrizesClicked, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.stabilisci_premi))
                        }
                    }
                    GamePhase.AUCTION -> {
                        Button(
                            onClick = onBidClicked,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isWin
                        ) {
                            Text(stringResource(R.string.fai_un_offerta))
                        }
                    }
                    GamePhase.ELIMINATION -> {
                        Button(
                            onClick = onDrawCardClicked,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = aiProposal == null
                        ) {
                            Text(stringResource(R.string.pesca_carta))
                        }
                    }
                    GamePhase.FINISHED -> {
                        Button(
                            onClick = onNewGameClicked,
                            enabled = isNewGameButtonEnabled
                        ) {
                            Text(stringResource(R.string.nuova_partita))
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun MultiplayerGameLayout(
    room: GameRoom,
    onBackClick: () -> Unit,
    multiplayerViewModel: MultiplayerViewModel
) {
    val myId = Firebase.auth.currentUser?.uid
    val me = room.players[myId]
    val opponents = room.players.filter { it.key != myId }.values.toList()
    val isHost = room.hostId == myId
    val isWin = room.highestBidderId == myId

    val onBackClicked = rememberClickable(onBackClick)
    val onStartPrizesClicked = rememberClickable { multiplayerViewModel.startPrizesPhase() }
    val onBidClicked = rememberClickable { multiplayerViewModel.playerBid() }
    val onDrawCardClicked = rememberClickable { multiplayerViewModel.drawEliminationCard() }
    val onResetGameClicked = rememberClickable { multiplayerViewModel.resetGame() }

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
                    text = "${stringResource(R.string.app_name)} (MP)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (me != null) {
                    Text(
                        stringResource(R.string.saldo, me.money),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onBackClicked) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.torna_al_menu), tint = MaterialTheme.colorScheme.onBackground)
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
                text = room.currentMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Sezione Asta
        if (room.status == RoomStatus.AUCTION && room.auctionCard != null) {
            val highestBidderName = room.players[room.highestBidderId]?.name ?: stringResource(R.string.nessuno)
            AuctionPanel(
                card = room.auctionCard!!,
                currentBid = room.currentBid,
                highestBidderName = highestBidderName,
                timer = room.auctionTimer
            )
        }

        // Sezione Premi
        if (room.prizes.isNotEmpty()) {
            Text(stringResource(R.string.premi_in_palio, room.merchantPot), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(room.prizes) { prize ->
                    val isWon = me?.cards?.any { it.id == prize.card.id } == true
                    PrizeItem(
                        prize = prize,
                        isRevealed = room.status == RoomStatus.FINISHED || room.eliminatedCardIds.contains(prize.card.id),
                        isWon = isWon
                    )
                }
            }
        }

        // Sezione Avversari
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            opponents.forEach { opp ->
                OpponentInfo(
                    name = opp.name,
                    cardCount = opp.cards.size,
                    money = opp.money,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (room.status == RoomStatus.ELIMINATION) {
                                Modifier.clickable(onClick = rememberClickable { multiplayerViewModel.inspectPlayer(opp) })
                            } else Modifier
                        )
                )
            }
        }

        // Sezione Giocatore
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(stringResource(R.string.le_tue_carte), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)

        if (me != null) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 70.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(me.cards) { card ->
                    GameCard(
                        card = card,
                        isFaceUp = true,
                        modifier = Modifier.aspectRatio(3f / 4f),
                        onClick = if (room.status == RoomStatus.ELIMINATION) { { multiplayerViewModel.openOfferDialog(card) } } else null
                    )
                }
            }
        }

        // Azioni (Solo Host può comandare le fasi del mercante)
        if (isHost) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                tonalElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (room.status) {
                        RoomStatus.DISTRIBUTION -> {
                            Button(onClick = onStartPrizesClicked, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.stabilisci_premi))
                            }
                        }
                        RoomStatus.AUCTION -> {
                            Button(
                                onClick = onBidClicked,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isWin
                            ) {
                                Text(stringResource(R.string.fai_un_offerta))
                            }
                        }
                        RoomStatus.ELIMINATION -> {
                            Button(
                                onClick = onDrawCardClicked,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = room.tradeRequest == null
                            ) {
                                Text(stringResource(R.string.pesca_carta))
                            }
                        }
                        RoomStatus.FINISHED -> {
                            Button(onClick = onResetGameClicked) {
                                Text(stringResource(R.string.nuova_partita))
                            }
                        }
                        else -> {}
                    }
                }
            }
        } else if (room.status == RoomStatus.AUCTION) {
            // Non-host can only bid during auction
             Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                tonalElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onBidClicked,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isWin
                    ) {
                        Text(stringResource(R.string.fai_un_offerta))
                    }
                }
            }
        }
    }
}

@Composable
fun MainMenu(
    onSinglePlayerClick: () -> Unit,
    onMultiplayerClick: () -> Unit,
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
                onClick = rememberClickable(onSinglePlayerClick),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.giocatore_singolo), modifier = Modifier.padding(8.dp), fontSize = 18.sp)
            }
            
            Button(
                onClick = rememberClickable(onMultiplayerClick),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.multigiocatore), modifier = Modifier.padding(8.dp), fontSize = 18.sp)
            }
            
            Button(
                onClick = rememberClickable(onSettingsClick),
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
fun MultiplayerMenu(
    playerName: String,
    defaultName: String,
    onBackClick: () -> Unit,
    onCreateRoom: (String) -> Unit,
    onJoinRoom: (String, String) -> Unit,
    onNameUpdate: (String) -> Unit,
    errorMessage: String?
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<((String) -> Unit)?>(null) }
    
    val onBackClicked = rememberClickable(onBackClick)

    if (showNameDialog) {
        var tempName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNameDialog = false; isLoading = false },
            title = { Text(stringResource(R.string.inserisci_nome)) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text(stringResource(R.string.nome)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    enabled = tempName.isNotBlank(),
                    onClick = rememberClickable {
                        onNameUpdate(tempName)
                        showNameDialog = false
                        pendingAction?.invoke(tempName)
                    }
                ) {
                    Text(stringResource(R.string.chiudi))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClicked) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.indietro))
            }
            Text(stringResource(R.string.multigiocatore), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading && errorMessage == null) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = rememberClickable { 
                        isLoading = true
                        if (playerName == defaultName || playerName.isBlank()) {
                            pendingAction = { onCreateRoom(it) }
                            showNameDialog = true
                        } else {
                            onCreateRoom(playerName)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.crea_tavolo), modifier = Modifier.padding(8.dp))
                }

                Divider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.unisciti_tavolo), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = code,
                        onValueChange = { if (it.length <= 6) code = it.uppercase() },
                        label = { Text(stringResource(R.string.codice_tavolo)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.inserisci_codice)) }
                    )
                    Button(
                        onClick = rememberClickable { 
                            if (code.length == 6) {
                                isLoading = true
                                if (playerName == defaultName || playerName.isBlank()) {
                                    pendingAction = { onJoinRoom(code, it) }
                                    showNameDialog = true
                                } else {
                                    onJoinRoom(code, playerName)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = code.length == 6,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.unisciti_tavolo))
                    }
                }
            }

            errorMessage?.let {
                isLoading = false
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun LobbyScreen(
    room: GameRoom,
    onBackClick: () -> Unit,
    onStartGame: () -> Unit,
    onReadyClick: () -> Unit
) {
    val context = LocalContext.current
    val shareMessage = stringResource(R.string.share_table_code_message, room.code)
    val myId = Firebase.auth.currentUser?.uid
    val isHost = room.hostId == myId

    val onBackClicked = rememberClickable(onBackClick)
    val onReadyClicked = rememberClickable(onReadyClick)
    val onStartGameClicked = rememberClickable(onStartGame)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.indietro))
                }
                Text(stringResource(R.string.codice_tavolo) + ": ${room.code}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            
            IconButton(onClick = rememberClickable { shareTableCode(context, shareMessage) }) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.condividi_codice))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(stringResource(R.string.giocatori_connessi, room.players.size), style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(room.players.values.toList()) { player ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(player.name, fontWeight = FontWeight.Bold)
                        if (player.isReady) {
                            Text(stringResource(R.string.pronto), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        } else {
                            Text(stringResource(R.string.non_pronto), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Host can start game when everyone is ready
        val everyoneReady = room.players.values.all { it.isReady }
        
        Button(
            onClick = onReadyClicked,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.pronto))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (isHost) {
            Button(
                onClick = onStartGameClicked,
                modifier = Modifier.fillMaxWidth(),
                enabled = everyoneReady,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.inizia_gioco))
            }
        } else {
            Text(
                stringResource(R.string.attesa_host),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun shareTableCode(context: Context, message: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentName: String,
    currentTheme: String,
    currentLanguage: String,
    isSoundEnabled: Boolean,
    currentDifficulty: DifficultyLevel,
    onNameChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onDifficultyChange: (DifficultyLevel) -> Unit,
    onBackClick: () -> Unit
) {
    val onBackClicked = rememberClickable(onBackClick)
    
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
            IconButton(onClick = onBackClicked) {
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
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text(
                        text = stringResource(R.string.profilo_giocatore),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
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
                }

                item {
                    Text(
                        text = stringResource(R.string.difficolta),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val levels = listOf(
                            DifficultyLevel.EASY to stringResource(R.string.difficolta_facile),
                            DifficultyLevel.MEDIUM to stringResource(R.string.difficolta_media),
                            DifficultyLevel.HARD to stringResource(R.string.difficolta_difficile)
                        )
                        levels.forEach { (level, label) ->
                            FilterChip(
                                selected = currentDifficulty == level,
                                onClick = rememberClickable { onDifficultyChange(level) },
                                label = { Text(label) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                item {
                    Text(
                        text = stringResource(R.string.aspetto),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
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
                                onClick = rememberClickable { onThemeChange(mode) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Text(text = stringResource(R.string.lingua), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    
                    var expanded by remember { mutableStateOf(false) }
                    val languages = listOf(
                        "it" to stringResource(R.string.lingua_it),
                        "en" to stringResource(R.string.lingua_en),
                        "de" to stringResource(R.string.lingua_de),
                        "fr" to stringResource(R.string.lingua_fr),
                        "es" to stringResource(R.string.lingua_es),
                        "pt" to stringResource(R.string.lingua_pt),
                        "nl" to stringResource(R.string.lingua_nl),
                        "pl" to stringResource(R.string.lingua_pl),
                        "sv" to stringResource(R.string.lingua_sv),
                        "da" to stringResource(R.string.lingua_da),
                        "fi" to stringResource(R.string.lingua_fi),
                        "el" to stringResource(R.string.lingua_el),
                        "cs" to stringResource(R.string.lingua_cs),
                        "hu" to stringResource(R.string.lingua_hu),
                        "ro" to stringResource(R.string.lingua_ro),
                        "bg" to stringResource(R.string.lingua_bg),
                        "sk" to stringResource(R.string.lingua_sk),
                        "hr" to stringResource(R.string.lingua_hr),
                        "lt" to stringResource(R.string.lingua_lt),
                        "sl" to stringResource(R.string.lingua_sl),
                        "lv" to stringResource(R.string.lingua_lv),
                        "et" to stringResource(R.string.lingua_et),
                        "ga" to stringResource(R.string.lingua_ga),
                        "mt" to stringResource(R.string.lingua_mt)
                    )
                    val currentLangLabel = languages.find { it.first == currentLanguage }?.second ?: stringResource(R.string.lingua_en)

                    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        OutlinedButton(
                            onClick = rememberClickable { expanded = true },
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
                                    onClick = rememberClickable {
                                        onLanguageChange(code)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Sound Effects", style = MaterialTheme.typography.titleLarge)
                        // The Switch is a special case: we cannot easily wrap onCheckedChange with rememberClickable 
                        // without it affecting the state toggling logic, so we directly trigger the sound playback here.
                        Switch(
                            checked = isSoundEnabled, 
                            onCheckedChange = { 
                                SoundManager.playClickSound()
                                onSoundEnabledChange(it) 
                            }
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
    highestBidderName: String,
    timer: Int
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
                    text = stringResource(R.string.miglior_offerente, highestBidderName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (timer <= 5) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$timer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (timer <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun InspectionDialog(playerName: String, cards: List<CardModel>, onDismiss: () -> Unit, onCardClick: (CardModel) -> Unit) {
    val onClick = rememberClickable(onDismiss)
    AlertDialog(
        onDismissRequest = onClick,
        title = { Text(stringResource(R.string.carte_di, playerName)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(300.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cards) { card ->
                    GameCard(
                        card = card,
                        isFaceUp = true,
                        modifier = Modifier
                            .aspectRatio(3f / 4f),
                        onClick = { onCardClick(card) }
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onClick) { Text(stringResource(R.string.chiudi)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferDialog(
    myCard: CardModel,
    targetPlayers: List<PlayerBase>,
    onDismiss: () -> Unit,
    onSellClick: (PlayerBase, Int) -> Unit,
    onSwapClick: (PlayerBase, CardModel) -> Unit
) {
    var selectedTarget by remember { mutableStateOf<PlayerBase?>(targetPlayers.firstOrNull()) }
    var requestAmount by remember { mutableStateOf("20") }
    var isSwapMode by remember { mutableStateOf(false) }

    val onDismissed = rememberClickable(onDismiss)

    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.offri_carta, myCard.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.a_chi_offrire))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    targetPlayers.forEach { player ->
                        FilterChip(
                            selected = selectedTarget?.id == player.id,
                            onClick = rememberClickable { selectedTarget = player },
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
                        TextButton(onClick = rememberClickable { isSwapMode = true }) {
                            Text(stringResource(R.string.chiedi_carta_scambio))
                        }
                    } else {
                        Text(stringResource(R.string.scegli_carta_chiedere, target.name))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(target.cards) { card ->
                                GameCard(
                                    card = card,
                                    modifier = Modifier
                                        .size(60.dp, 80.dp),
                                    onClick = { onSwapClick(target, card) }
                                )
                            }
                        }
                        TextButton(onClick = rememberClickable { isSwapMode = false }) {
                            Text(stringResource(R.string.chiedi_soldi_invece))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isSwapMode) {
                Button(
                    onClick = rememberClickable { selectedTarget?.let { onSellClick(it, requestAmount.toIntOrNull() ?: 0) } },
                    enabled = (requestAmount.toIntOrNull() ?: 0) > 0
                ) {
                    Text(stringResource(R.string.offri_per, requestAmount.toIntOrNull() ?: 0))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismissed) { Text(stringResource(R.string.annulla)) } }
    )
}

@Composable
fun TradeDialog(
    targetPlayerName: String,
    targetCard: CardModel,
    myCards: List<CardModel>,
    myBalance: Int,
    onDismiss: () -> Unit,
    onMoneyOffer: (Int) -> Unit,
    onCardOffer: (CardModel) -> Unit
) {
    var offerAmount by remember { mutableStateOf("10") }
    var showCardTrade by remember { mutableStateOf(false) }
    val onDismissed = rememberClickable(onDismiss)

    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.scambia_con, targetPlayerName)) },
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
                        onClick = rememberClickable { showCardTrade = true }
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
                                    .size(60.dp, 80.dp),
                                onClick = { onCardOffer(card) }
                            )
                        }
                    }
                    TextButton(onClick = rememberClickable { showCardTrade = false }) {
                        Text(stringResource(R.string.torna_offerta_denaro))
                    }
                }
            }
        },
        confirmButton = {
            if (!showCardTrade) {
                Button(
                    onClick = rememberClickable { onMoneyOffer(offerAmount.toIntOrNull() ?: 0) },
                    enabled = (offerAmount.toIntOrNull() ?: 0) > 0
                ) {
                    Text(stringResource(R.string.offri_amount, offerAmount.toIntOrNull() ?: 0))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismissed) { Text(stringResource(R.string.annulla)) } }
    )
}

@Composable
fun AIProposalDialog(
    proposal: AIProposal,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
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
            Button(onClick = rememberClickable(onAccept)) { Text(stringResource(R.string.accetta)) }
        },
        dismissButton = {
            TextButton(onClick = rememberClickable(onReject)) { Text(stringResource(R.string.rifiuta)) }
        }
    )
}

@Composable
fun TradeRequestDialog(
    request: TradeRequest,
    room: GameRoom,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val sender = room.players[request.senderId]
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Proposta di scambio") },
        text = {
            val message = when (request.type) {
                "BUY" -> "${sender?.name} vuole comprare la tua carta ${request.card1?.name} per ${request.money} €"
                "SELL" -> "${sender?.name} ti offre la sua carta ${request.card1?.name} per ${request.money} €"
                "EXCHANGE" -> "${sender?.name} vuole scambiare la sua carta ${request.card1?.name} con la tua ${request.card2?.name}"
                else -> ""
            }
            Text(message)
        },
        confirmButton = { Button(onClick = onAccept) { Text("Accetta") } },
        dismissButton = { TextButton(onClick = onReject) { Text("Rifiuta") } }
    )
}

private fun AIProposal.getAI() = when(this) {
    is AIProposal.Buy -> ai
    is AIProposal.Sell -> ai
    is AIProposal.Exchange -> ai
}

@Composable
fun PrizeItem(prize: Prize, isRevealed: Boolean, isWon: Boolean = false) {
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
            color = when {
                !isRevealed -> MaterialTheme.colorScheme.onSurfaceVariant
                isWon -> Color(0xFF2E7D32) // Green
                else -> Color(0xFFD32F2F) // Red
            }
        )
    }
}

@Composable
fun OpponentInfo(name: String, cardCount: Int, money: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$cardCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("${money}€", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.carte), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
