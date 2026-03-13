package eu.gtpware.mercanteinfiera.models

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test to ensure that models are correctly structured for Firebase serialization/deserialization.
 * This catches regressions where models might be changed to be incompatible with
 * Firebase's CustomClassMapper (e.g. missing no-arg constructor, private properties, etc.)
 */
class SerializationTest {

    @Test
    fun testCardModelSerializationProperties() {
        val card = CardModel(id = 1, name = "Test Card")
        
        // Firebase uses reflection to access properties. 
        // We ensure they are accessible and mutable if they need to be.
        assertEquals(1, card.id)
        assertEquals("Test Card", card.name)
        
        // Test no-arg constructor (implicit via default values)
        val emptyCard = CardModel()
        assertEquals(0, emptyCard.id)
        assertEquals("", emptyCard.name)
    }

    @Test
    fun testGameRoomSerialization() {
        // GameRoom should have all fields accessible for Firebase
        val room = GameRoom()
        assertEquals("", room.code)
        assertEquals(RoomStatus.LOBBY, room.status)
        assertEquals(0, room.cardsToAuction.size)
    }

    @Test
    fun testPrizeSerialization() {
        val prize = Prize()
        assertEquals(0, prize.value)
        assertEquals(0, prize.card.id)
    }
}
