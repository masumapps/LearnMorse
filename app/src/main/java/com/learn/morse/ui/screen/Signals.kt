package com.learn.morse.ui.screen

/**
 * A signal operators send as a unit, taught with its [meaning]. A [isProsign] prosign is sent as one
 * character, its letters run together with no gap (SOS is ...---...); others are sent letter by letter.
 */
data class Signal(
    val name: String,
    // Short enough for an answer tile; every signal's meaning differs, so a meaning quiz has one answer.
    val meaning: String,
    val isProsign: Boolean = false,
    // Extra detail for the start dialog, like the punctuation mark that shares the code.
    val note: String? = null,
) {
    val morse: String = if (isProsign) morseOf(name).replace(" ", "") else morseOf(name)
}

/** The signals course's lessons in order, each introducing a few signals that are used together. */
val signalGroups: List<Pair<String, List<Signal>>> = listOf(
    "Making a call" to listOf(
        Signal("CQ", "Calling anyone", note = "Starts a call: CQ CQ DE <callsign> K"),
        Signal("DE", "From / this is"),
        Signal("K", "Over, go ahead"),
        Signal("AR", "End of message", isProsign = true, note = "Same code as +"),
    ),
    "Replying" to listOf(
        Signal("R", "Received"),
        Signal("KN", "Go ahead, only you", isProsign = true, note = "Same code as ("),
        Signal("73", "Best regards"),
        Signal("SK", "End of contact", isProsign = true),
    ),
    "Help and errors" to listOf(
        Signal("SOS", "I need help", isProsign = true, note = "The distress signal"),
        Signal("HH", "I made a mistake", isProsign = true, note = "Eight dots; resend the last word"),
        Signal("AS", "Wait", isProsign = true, note = "Same code as &"),
        Signal("BT", "New paragraph", isProsign = true, note = "Same code as ="),
    ),
    "Q-codes" to listOf(
        Signal("QTH", "My location"),
        Signal("QRZ", "Who is calling me?"),
        Signal("QSL", "I confirm receipt"),
        Signal("88", "Love and kisses"),
    ),
)

private val signalsByName: Map<String, Signal> = signalGroups.flatMap { it.second }.associateBy { it.name }

fun signalOf(name: String): Signal? = signalsByName[name]

/**
 * Morse for a lesson option: a signal's own code (prosigns without gaps), otherwise [morseOf].
 * K and R are signals and letters with the same code, so letter lessons are unaffected.
 */
fun codeOf(option: String): String = signalOf(option)?.morse ?: morseOf(option)
