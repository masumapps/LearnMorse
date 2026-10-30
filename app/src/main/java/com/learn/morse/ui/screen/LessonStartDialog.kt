package com.learn.morse.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.learn.morse.R

/**
 * Shown when a lesson opens: introduces its letters (tap one to hear it) and waits for the user to start.
 * [resumeStep] is the saved step index, 0 when the lesson starts from the beginning.
 */
@Composable
fun LessonStartDialog(
    lesson: LessonModel,
    stepCount: Int,
    resumeStep: Int,
    onPlayLetter: (String) -> Unit,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        // Pop in from slightly smaller.
        val scale = remember { Animatable(0.85f) }
        LaunchedEffect(Unit) {
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Header(lesson)

                // Only the letters scroll, so a long alphabet never pushes the Start button off screen.
                val letters = lesson.options
                val compact = letters.size > COMPACT_AFTER
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val (countText, tapText) = when (lesson.kind) {
                        LessonKind.Symbols -> R.plurals.start_count_letters to R.string.start_tap_letter
                        LessonKind.Words -> R.plurals.start_count_words to R.string.start_tap_word
                        LessonKind.Signals -> R.plurals.start_count_signals to R.string.start_tap_signal
                    }
                    Text(
                        text = if (compact) pluralStringResource(countText, letters.size, letters.size) else stringResource(tapText),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (lesson.kind == LessonKind.Signals) {
                        // Signals are learned by meaning, so they're listed with it rather than as cards.
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            letters.forEach { name ->
                                signalOf(name)?.let { SignalRow(it, onClick = { onPlayLetter(name) }) }
                            }
                        }
                    } else FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
                    ) {
                        letters.forEach { letter ->
                            if (compact) CompactLetterCard(letter, onClick = { onPlayLetter(letter) })
                            else LetterCard(letter, onClick = { onPlayLetter(letter) })
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(20.dp))
                    if (resumeStep > 0) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            progress = { resumeStep / stepCount.toFloat() },
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.start_resume_step, resumeStep + 1, stepCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = pluralStringResource(R.plurals.steps_listen_tap, stepCount, stepCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        onClick = onStart,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(if (resumeStep > 0) R.string.start_continue else R.string.start_start),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_not_now)) }
                }
            }
        }
    }
}

@Composable
private fun Header(lesson: LessonModel) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
            .padding(vertical = 24.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(colors.onPrimary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                // Practice isn't numbered: it belongs to no course.
                text = if (lesson.isPractice) "🎯" else lessonNumber(lesson).toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (lesson.isPractice) stringResource(R.string.start_practice_header)
            else stringResource(R.string.start_lesson_header, stringResource(courseOf(lesson.id).name).uppercase(), lessonNumber(lesson)),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onPrimary.copy(alpha = 0.8f),
        )
        Text(
            text = lesson.displayName(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onPrimary,
        )
    }
}

@Composable
private fun LetterCard(letter: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            // Grows past 72dp for long codes like J (.---) instead of clipping them.
            .widthIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.height(6.dp))
        val code = codeOf(letter)
        // A phrase's code would stretch the card past the dialog; the play button covers it.
        if (code.length <= MAX_PREVIEW_MORSE) MorseSymbols(code, modifier = Modifier.height(8.dp))
        Spacer(Modifier.height(8.dp))
        Icon(
            Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = stringResource(R.string.play_item, letter),
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** A signal with its code, meaning and note; tapping plays it. */
@Composable
private fun SignalRow(signal: Signal, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClickLabel = stringResource(R.string.play_item, signal.name), onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = signal.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(10.dp))
                MorseSymbols(signal.morse, dotSize = 5.dp)
            }
            Text(
                text = stringResource(signal.meaning),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            val detail = listOfNotNull(if (signal.isProsign) stringResource(R.string.start_prosign) else null, signal.noteText())
            if (detail.isNotEmpty()) {
                Text(
                    text = detail.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Smaller card for lessons with many letters: 5 fit per row in a phone-width dialog (~280dp of content),
 * so the whole alphabet takes 6 rows. 4dp dots keep the longest codes (.---, --.-) inside 48dp.
 */
@Composable
private fun CompactLetterCard(letter: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            // Grows for words, whose two codes don't fit in 48dp.
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClickLabel = stringResource(R.string.play_item, letter), onClick = onClick)
            .padding(vertical = 8.dp, horizontal = if (letter.length > 1) 6.dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.height(4.dp))
        val code = codeOf(letter)
        if (code.length <= MAX_PREVIEW_MORSE) MorseSymbols(code, modifier = Modifier.height(4.dp), dotSize = 4.dp)
    }
}

// Lessons with more letters than this use compact cards.
private const val COMPACT_AFTER = 8

// Longest code the letter cards show; five-letter words fit, phrases don't.
private const val MAX_PREVIEW_MORSE = 26

@Preview
@Composable
private fun LessonStartDialogAlphabetPreview() {
    LessonStartDialog(
        lesson = lessons.first().copy(title = LessonTitle.Review(1), options = ('A'..'Z').map { it.toString() }, chapter = 60),
        stepCount = 60,
        resumeStep = 0,
        onPlayLetter = {},
        onStart = {},
        onDismiss = {},
    )
}

@Preview
@Composable
private fun LessonStartDialogPreview() {
    LessonStartDialog(
        lesson = lessons[4],
        stepCount = 24,
        resumeStep = 5,
        onPlayLetter = {},
        onStart = {},
        onDismiss = {},
    )
}
