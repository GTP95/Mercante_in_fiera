package eu.gtpware.mercanteinfiera.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eu.gtpware.mercanteinfiera.R
import eu.gtpware.mercanteinfiera.models.CardModel
import eu.gtpware.mercanteinfiera.utils.rememberClickable

@Composable
fun GameCard(
    card: CardModel,
    isFaceUp: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val density = LocalDensity.current.density
    val rotationAnimation by animateFloatAsState(
        targetValue = if (isFaceUp) 0f else 180f,
        label = "cardRotation"
    )

    var hasImageSucceeded by remember { mutableStateOf(false) }
    
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = rememberClickable(onClick))
    } else {
        modifier
    }

    Card(
        modifier = cardModifier
            .graphicsLayer {
                rotationY = rotationAnimation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color.Black),
        colors = CardDefaults.cardColors(
            containerColor = if (isFaceUp) card.placeholderColor else Color(0xFF37474F)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (rotationAnimation <= 90f) {
                // Faccia della carta
                if (isFaceUp) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("file:///android_asset/${card.imagePath}")
                            .crossfade(true)
                            .build(),
                        contentDescription = card.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { hasImageSucceeded = true },
                        onError = { hasImageSucceeded = false }
                    )

                    // Se l'immagine non è caricata o non esiste, mostra il nome e il colore
                    if (!hasImageSucceeded) {
                        Text(
                            text = card.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            } else {
                // Retro della carta (ruotato per essere leggibile)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.card_back_initial),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
