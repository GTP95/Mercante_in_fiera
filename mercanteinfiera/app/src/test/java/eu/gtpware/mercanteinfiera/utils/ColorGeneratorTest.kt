package eu.gtpware.mercanteinfiera.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorGeneratorTest {

    @Test
    fun generateUniqueColors_returnsCorrectCount() {
        val count = 10
        val colors = ColorGenerator.generateUniqueColors(count)
        assertEquals(count, colors.size)
    }
}
