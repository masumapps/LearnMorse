package com.learn.morse.ui.screen

/**
 * Converts between text and morse. Morse uses '.' and '-', a space between letters and '/' between
 * words. Codes come from [morseMap].
 */
object MorseTranslator {
    private val textToMorse: Map<Char, String> = morseMap.mapKeys { it.key.single() }
    private val morseToText: Map<String, Char> = textToMorse.entries.associate { (char, code) -> code to char }

    /** Characters without a morse code are skipped. */
    fun encode(text: String): String =
        text.uppercase().trim().split(Regex("\\s+"))
            .map { word -> word.mapNotNull { textToMorse[it] }.joinToString(" ") }
            .filter { it.isNotEmpty() }
            .joinToString(" / ")

    /** Unknown codes become '?'. Accepts '·' / '•' for dots and '−' / '–' / '_' for dashes too. */
    fun decode(morse: String): String =
        morse.map {
            when (it) {
                '·', '•', '*' -> '.'
                '−', '–', '—', '_' -> '-'
                '|' -> '/'
                else -> it
            }
        }.joinToString("")
            .split('/')
            .map { word ->
                word.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                    .map { morseToText[it] ?: '?' }
                    .joinToString("")
            }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
}
