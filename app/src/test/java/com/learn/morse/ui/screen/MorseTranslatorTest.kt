package com.learn.morse.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class MorseTranslatorTest {

    @Test
    fun encodesLettersAndWords() {
        assertEquals("... --- ... / .... .. / -----", MorseTranslator.encode("sos  hi 0"))
    }

    @Test
    fun encodeSkipsUnknownCharacters() {
        assertEquals(".- / -...", MorseTranslator.encode("a # b"))
    }

    @Test
    fun decodesLettersAndWords() {
        assertEquals("SOS HI", MorseTranslator.decode("... --- ... / .... .."))
    }

    @Test
    fun decodeMarksUnknownCodes() {
        assertEquals("E?T", MorseTranslator.decode(". ....... -"))
    }

    @Test
    fun roundTrips() {
        val text = "HELLO, WORLD! 123?"
        assertEquals(text, MorseTranslator.decode(MorseTranslator.encode(text)))
    }
}
