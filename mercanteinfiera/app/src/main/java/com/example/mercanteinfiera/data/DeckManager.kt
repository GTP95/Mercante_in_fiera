package com.example.mercanteinfiera.data

import com.example.mercanteinfiera.models.CardModel
import com.example.mercanteinfiera.utils.ColorGenerator
import kotlin.random.Random

object DeckManager {
    
    private val cardNames = listOf(
        "Il Bambino", "La Gondola", "La Brocca d'Acqua", "Vino Rosso", "Il Pane e il Grano",
        "L'Olio", "L'Araba Fenice", "La Scala", "Il Campanile", "La Torre",
        "Il Castello", "La Fortezza", "La Rocca", "La Montagna", "La Valle",
        "Il Fiume", "Il Lago", "Il Bosco", "La Campagna", "La Città",
        "Il Villaggio", "La Piazza", "La Chiesa", "Il Monastero", "Il Convento",
        "Il Palazzo", "La Villa", "Il Teatro", "La Poesia", "La Musica",
        "La Pittura", "La Scultura", "La Danza", "Il Ballo", "La Festa",
        "Il Carnevale", "La Sagra", "La Fiera", "Il Mercato", "La Bottega"
    )
    
    fun createFullDeck(): List<CardModel> {
        val colors = ColorGenerator.generateUniqueColors(cardNames.size)
        return cardNames.mapIndexed { index, name ->
            CardModel(
                id = index + 1,
                name = name,
                placeholderColor = colors[index]
            )
        }
    }
}