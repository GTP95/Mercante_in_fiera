package eu.gtpware.mercanteinfiera.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckManagerTest {

    @Test
    fun createFullDeck_returnsCorrectNumberOfCards() {
        val deck = DeckManager.createFullDeck()
        assertEquals(40, deck.size)
    }

    @Test
    fun createFullDeck_containsUniqueIds() {
        val deck = DeckManager.createFullDeck()
        val ids = deck.map { it.id }
        assertEquals(deck.size, ids.distinct().size)
    }

    @Test
    fun createFullDeck_containsExpectedNames() {
        val deck = DeckManager.createFullDeck()
        val names = deck.map { it.name }
        assertTrue(names.contains("Il Bambino"))
        assertTrue(names.contains("La Bottega"))
    }
}
