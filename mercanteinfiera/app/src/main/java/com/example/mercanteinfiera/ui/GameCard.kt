package com.example.mercanteinfiera.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mercanteinfiera.models.CardModel

@Composable
fun GameCard(
    card: CardModel,
    isFaceUp: Boolean = true,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    var rotation by remember { mutableFloatStateOf(0f) }
    
    val rotationAnimation by animateFloatAsState(
        targetValue = if (isFaceUp) 0f else 180f,
        label = "cardRotation"
    )
    
    Card(
        modifier = modifier
            .size(120.dp, 160.dp)
            .graphicsLayer {
                rotationY = rotationAnimation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, Color.Black),
        colors = CardDefaults.cardColors(
            containerColor = if (isFaceUp) card.placeholderColor else Color.Gray
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isFaceUp) {
                Text(
                    text = card.name,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                // Visualizzazione retro della carta
                Text(
                    text = "?",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
        }
    }
}