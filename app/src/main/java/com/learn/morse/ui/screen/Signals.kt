package com.learn.morse.ui.screen

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.learn.morse.R

/**
 * A signal operators send as a unit, taught with its [meaning]. A [isProsign] prosign is sent as one
 * character, its letters run together with no gap (SOS is ...---...); others are sent letter by letter.
 */
data class Signal(
    val name: String,
    // Short enough for an answer tile; every signal's meaning differs, so a meaning quiz has one answer.
    @StringRes val meaning: Int,
    val isProsign: Boolean = false,
    // Extra detail for the start dialog, like the punctuation mark that shares the code.
    @StringRes val note: Int? = null,
    // The punctuation mark with the same code, filled into the "Same code as" note.
    val sameCodeAs: String? = null,
) {
    val morse: String = if (isProsign) morseOf(name).replace(" ", "") else morseOf(name)
}

/** The extra detail shown under a signal, if it has any. */
@Composable
fun Signal.noteText(): String? = when {
    sameCodeAs != null -> stringResource(R.string.signal_note_same_as, sameCodeAs)
    note != null -> stringResource(note)
    else -> null
}

/** The signals course's lessons in order, each introducing a few signals that are used together. */
val signalGroups: List<Pair<Int, List<Signal>>> = listOf(
    R.string.signal_group_call to listOf(
        Signal("CQ", R.string.signal_cq, note = R.string.signal_note_cq),
        Signal("DE", R.string.signal_de),
        Signal("K", R.string.signal_k),
        Signal("AR", R.string.signal_ar, isProsign = true, sameCodeAs = "+"),
    ),
    R.string.signal_group_reply to listOf(
        Signal("R", R.string.signal_r),
        Signal("KN", R.string.signal_kn, isProsign = true, sameCodeAs = "("),
        Signal("73", R.string.signal_73),
        Signal("SK", R.string.signal_sk, isProsign = true),
    ),
    R.string.signal_group_help to listOf(
        Signal("SOS", R.string.signal_sos, isProsign = true, note = R.string.signal_note_sos),
        Signal("HH", R.string.signal_hh, isProsign = true, note = R.string.signal_note_hh),
        Signal("AS", R.string.signal_as, isProsign = true, sameCodeAs = "&"),
        Signal("BT", R.string.signal_bt, isProsign = true, sameCodeAs = "="),
    ),
    R.string.signal_group_qcodes to listOf(
        Signal("QTH", R.string.signal_qth),
        Signal("QRZ", R.string.signal_qrz),
        Signal("QSL", R.string.signal_qsl),
        Signal("88", R.string.signal_88),
    ),
)

/** Radio shorthand taught in the On the Air course, with their meanings, like signals. */
val abbreviationGroups: List<Pair<Int, List<Signal>>> = listOf(
    R.string.abbr_group_greetings to listOf(
        Signal("GM", R.string.abbr_gm),
        Signal("GE", R.string.abbr_ge),
        Signal("OM", R.string.abbr_om),
        Signal("YL", R.string.abbr_yl),
    ),
    R.string.abbr_group_courtesy to listOf(
        Signal("TNX", R.string.abbr_tnx),
        Signal("PSE", R.string.abbr_pse),
        Signal("FB", R.string.abbr_fb),
        Signal("AGN", R.string.abbr_agn),
    ),
    R.string.abbr_group_chat to listOf(
        Signal("UR", R.string.abbr_ur),
        Signal("HR", R.string.abbr_hr),
        Signal("ES", R.string.abbr_es),
        Signal("FER", R.string.abbr_fer),
    ),
    R.string.abbr_group_station to listOf(
        Signal("RIG", R.string.abbr_rig),
        Signal("ANT", R.string.abbr_ant),
        Signal("WX", R.string.abbr_wx),
        Signal("CUL", R.string.abbr_cul),
    ),
)

private val signalsByName: Map<String, Signal> =
    (signalGroups + abbreviationGroups).flatMap { it.second }.associateBy { it.name }

fun signalOf(name: String): Signal? = signalsByName[name]

/**
 * Morse for a lesson option: a signal's own code (prosigns without gaps), otherwise [morseOf].
 * K and R are signals and letters with the same code, so letter lessons are unaffected.
 */
fun codeOf(option: String): String = signalOf(option)?.morse ?: morseOf(option)
