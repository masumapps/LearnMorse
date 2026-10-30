package com.learn.morse.ui.screen

import android.content.Context
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.morse.R
import com.learn.morse.analytics.Analytics
import com.learn.morse.audio.FeedbackSounds
import com.learn.morse.audio.Haptics
import com.learn.morse.audio.KeyTone
import com.learn.morse.audio.MorsePlayer
import com.learn.morse.audio.SoundSettings
import com.learn.morse.monetization.AppOpenAds
import com.learn.morse.progress.LessonProgress
import com.learn.morse.progress.Streak
import com.learn.morse.progress.SymbolStats
import kotlin.math.hypot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.content.edit
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import com.learn.morse.ui.theme.isDarkTheme

@Composable
fun LearnScreenRoute(lessonId: Long, resume: Boolean, onNextLesson: () -> Unit, onBack: () -> Unit){
    val context = LocalContext.current
    val initialChapter = remember(lessonId) { if (resume) LessonProgress.savedStep(context, lessonId) else 0 }
    val initialMistakes = remember(lessonId) { if (resume) LessonProgress.savedMistakes(context, lessonId) else 0 }
    val lesson = remember(lessonId) { lessons.first { it.id == lessonId } }
    val learns = remember(lessonId) { learnsFor(lessonId) }
    NoAppOpenAds()
    LearnScreen(
        lesson = lesson,
        learns = learns,
        isLastLesson = lesson == courseOf(lessonId).lessons.last(),
        initialChapter = initialChapter,
        initialMistakes = initialMistakes,
        onChapterChange = { LessonProgress.saveStep(context, lessonId, it) },
        onMistakesChange = { LessonProgress.saveMistakes(context, lessonId, it) },
        onAnswer = { step, correct ->
            // Words and signals aren't single symbols, so only symbol lessons feed weak-letter practice.
            if (lesson.kind == LessonKind.Symbols) SymbolStats.record(context, step.target, correct)
        },
        onLessonFinished = { stars ->
            LessonProgress.markCompleted(context, lessonId, stars)
            Streak.record(context)
            Analytics.logLessonComplete(lessonId, stars)
        },
        onNextLesson = onNextLesson,
        onBack = onBack
    )
}

/**
 * A short session of the symbols missed most lately, built when it opens; saves no step, only which
 * symbols are still weak and the streak.
 */
@Composable
fun PracticeScreenRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val session = remember {
        val learned = learnedSymbols(LessonProgress.completedCount(context))
        practiceSession(learned, SymbolStats.weak(context, learned), Random.Default)
    }
    if (session == null) {
        // Nothing learned yet; Home doesn't offer practice then, so just leave.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    NoAppOpenAds()
    LearnScreen(
        lesson = session.first,
        learns = session.second,
        isLastLesson = true,
        onAnswer = { step, correct -> SymbolStats.record(context, step.target, correct) },
        onLessonFinished = { stars ->
            Streak.record(context)
            Analytics.logPracticeComplete(stars)
        },
        onBack = onBack
    )
}

/** No app open ad while a lesson is on screen. */
@Composable
private fun NoAppOpenAds() {
    DisposableEffect(Unit) {
        AppOpenAds.enterLesson()
        onDispose { AppOpenAds.leaveLesson() }
    }
}

@Composable
private fun LearnScreen(
    lesson: LessonModel,
    learns: List<LearnModel>,
    isLastLesson: Boolean,
    initialChapter: Int = 0,
    initialMistakes: Int = 0,
    onChapterChange: (Int) -> Unit = {},
    onMistakesChange: (Int) -> Unit = {},
    // Every answer, right or wrong, with the step it answered.
    onAnswer: (LearnModel, Boolean) -> Unit = { _, _ -> },
    onLessonFinished: (stars: Int) -> Unit = {},
    onNextLesson: () -> Unit = {},
    onBack: () -> Unit = {}
){

    var chapter by remember { mutableIntStateOf(initialChapter.coerceIn(0, learns.lastIndex)) }
    // Remember the step so "Continue learning" can resume from it.
    val currentOnChapterChange by rememberUpdatedState(onChapterChange)
    LaunchedEffect(chapter) { currentOnChapterChange(chapter) }
    val context = LocalContext.current
    val sound = remember {
        SoundSettings.load(context).let { saved -> lesson.wpm?.let { saved.copy(wpm = it, farnsworthWpm = null) } ?: saved }
    }

    val learn=learns[ chapter]

    // The lesson waits behind the start dialog: no sound or tips until the user starts it.
    var started by rememberSaveable { mutableStateOf(false) }

    // Bumped on FAB click; plays on start, on chapter change and on every click (restarting any ongoing playback).
    var playRequest by remember { mutableIntStateOf(0) }
    // Keyed on chapter, not learn: consecutive chapters can hold equal LearnModels, which wouldn't restart the effect.
    LaunchedEffect(chapter, playRequest, started) {
        if (started && learn.isListen) MorsePlayer.play(context, learn.morse, sound)
    }

    val scope = rememberCoroutineScope()
    // Sounds while the circle button is held.
    val keyTone = remember { KeyTone(sound.frequency.toDouble(), sound.volume) }
    DisposableEffect(keyTone) { onDispose { keyTone.close() } }
    // The key sounds and buzzes only through the outputs the user turned on.
    fun keyDown() {
        if (sound.sound) keyTone.start()
        if (sound.vibrate) Haptics.keyDown(context)
    }
    fun keyUp() {
        keyTone.stop()
        if (sound.vibrate) Haptics.keyUp(context)
    }
    // Right/wrong chime played as the card flips to the result.
    val feedback = remember { FeedbackSounds(context, sound.volume) }
    DisposableEffect(feedback) { onDispose { feedback.release() } }
    // Rotation of the letter card: 0 shows the letter, 180 shows the Right/Wrong result.
    val flip = remember { Animatable(0f) }
    var isCorrect by remember { mutableStateOf(false) }
    var isAnswering by remember { mutableStateOf(false) }
    // Answer tile the user picked in a listen step, highlighted with the result until the card flips back.
    var picked by remember { mutableStateOf<String?>(null) }

    // Morse typed with the circle button in tap chapters.
    var input by remember(chapter) { mutableStateOf("") }
    // Tips are shown only on the first tap chapter ever; the flag is persisted across launches.
    val prefs = context.getSharedPreferences("learn_morse", Context.MODE_PRIVATE)
    var tipsSeen by remember { mutableStateOf(prefs.getBoolean(KEY_TIPS_SEEN, false)) }
    fun dismissTips() {
        tipsSeen = true
        prefs.edit { putBoolean(KEY_TIPS_SEEN, true) }
    }
    val showTips = started && !learn.isListen && !tipsSeen

    // Wrong answers on the current step; after REVEAL_AFTER_WRONG a quiz card shows its letter instead of 🎧.
    var wrongCount by remember(chapter) { mutableIntStateOf(0) }
    // Wrong answers across the whole lesson, which decide its stars. Saved so resuming keeps the count.
    var mistakes by remember { mutableIntStateOf(initialMistakes) }
    val currentOnMistakesChange by rememberUpdatedState(onMistakesChange)
    // Stars earned once the last step is answered; non-null shows the complete dialog.
    var finishedStars by rememberSaveable { mutableStateOf<Int?>(null) }

    val currentOnAnswer by rememberUpdatedState(onAnswer)
    fun answer(correct: Boolean, revealDelayMs: Long = 0) {
        if (isAnswering) return
        isAnswering = true
        currentOnAnswer(learn, correct)
        isCorrect = correct
        scope.launch {
            delay(revealDelayMs.milliseconds)
            feedback.play(isCorrect)
            val feedbackStartMs = SystemClock.elapsedRealtime()
            flip.animateTo(180f, tween(400))
            // Counted while the result side shows, so a reveal appears as the card flips back rather than before.
            if (!isCorrect) {
                wrongCount++
                mistakes++
                currentOnMistakesChange(mistakes)
            }
            delay(700.milliseconds)
            // Switch chapter while the result side is showing, so flipping back reveals the next letter.
            if (isCorrect && chapter == learns.lastIndex) {
                val stars = LessonProgress.starsFor(mistakes, learns.size)
                onLessonFinished(stars)
                finishedStars = stars
                return@launch
            }
            if (isCorrect) chapter++
            flip.animateTo(0f, tween(400))
            // Replay the sound so the user can try again, once the wrong chime has finished.
            if (!isCorrect) {
                delay((FeedbackSounds.WRONG_DURATION_MS - (SystemClock.elapsedRealtime() - feedbackStartMs)).milliseconds)
                playRequest++
            }
            input = ""
            picked = null
            isAnswering = false
        }
    }

    if (!started) {
        // Letter previews from the dialog; a new tap restarts playback.
        var previewJob by remember { mutableStateOf<Job?>(null) }
        LessonStartDialog(
            lesson = lesson,
            stepCount = learns.size,
            resumeStep = chapter,
            onPlayLetter = { letter ->
                previewJob?.cancel()
                previewJob = scope.launch {
                    MorsePlayer.play(context, codeOf(letter), sound)
                }
            },
            onStart = {
                previewJob?.cancel()
                started = true
            },
            onDismiss = onBack
        )
    }

    finishedStars?.let { stars ->
        LessonCompleteDialog(
            stars = stars,
            mistakes = mistakes,
            isLastLesson = isLastLesson,
            isPractice = lesson.isPractice,
            onNext = onNextLesson,
            onBackToLessons = onBack
        )
    }

    if (showTips) {
        AlertDialog(
            onDismissRequest = { dismissTips() },
            icon = { Icon(Icons.Filled.TouchApp, contentDescription = null) },
            title = { Text(stringResource(R.string.learn_tips_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.learn_tips_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TipRow(action = stringResource(R.string.learn_tip_tap), detail = stringResource(R.string.learn_tip_tap_detail), symbol = "•")
                    TipRow(action = stringResource(R.string.learn_tip_hold), detail = stringResource(R.string.learn_tip_hold_detail), symbol = "—")
                }
            },
            confirmButton = {
                Button(onClick = { dismissTips() }) { Text(stringResource(R.string.action_got_it)) }
            }
        )
    }

    // Transparent, so the app's Morse pattern background shows behind the lesson too.
    Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LessonTopBar(
                step = chapter,
                stepCount = learns.size,
                onClose = onBack
            )

            Spacer(Modifier.height(12.dp))
            StepHeader(learn)

            // The card takes the free space; everything below keeps its natural height.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                FlipCard(
                    learn = learn,
                    rotation = flip.value,
                    isCorrect = isCorrect,
                    revealed = wrongCount >= REVEAL_AFTER_WRONG,
                    lessonName = lesson.displayName()
                )
            }

            if (learn.isListen) {
                FilledTonalButton(onClick = { playRequest++ }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.learn_play_again))
                }
                Spacer(Modifier.height(16.dp))
                // A meaning quiz answers with what each signal means instead of its name.
                val meaningKeys = lesson.options.map { option -> signalOf(option)?.let { stringResource(it.meaning) }.orEmpty() }
                val meaning = learn.meaning?.let { stringResource(it) }
                val keys = learn.choices ?: if (learn.isMeaning) meaningKeys else lesson.options
                AnswerGrid(
                    keys = keys,
                    // Quiz steps hide the morse so the letter is recognized by sound, not by counting symbols.
                    // Words and signals never show it: their codes don't fit in a tile, and the card shows it anyway.
                    showMorse = !learn.isHidden && lesson.kind == LessonKind.Symbols,
                    // Phrases need the wide, smaller-type tiles that meanings use.
                    isTextAnswer = learn.isMeaning || keys.any { ' ' in it },
                    picked = picked,
                    isCorrect = isCorrect,
                    enabled = !isAnswering,
                    onPick = { key ->
                        picked = key
                        answer(key == if (learn.isMeaning) meaning else learn.target)
                    }
                )
                Spacer(Modifier.height(24.dp))
            } else {
                // Down time of the latest finger still on the key, or null when nothing is pressed.
                var pressStartMs by remember { mutableStateOf<Long?>(null) }
                // Symbol for the press in progress: '.' on touch, turning into '-' once held long enough.
                // Uses the same threshold as the release, so what was shown is what gets typed.
                var pending by remember { mutableStateOf<Char?>(null) }
                LaunchedEffect(pressStartMs) {
                    if (pressStartMs == null) {
                        pending = null
                    } else {
                        pending = '.'
                        delay(DASH_THRESHOLD_MS.milliseconds)
                        pending = '-'
                    }
                }
                // In a word, a pause after a release ends the letter: typed as a space, the letter gap.
                LaunchedEffect(input, pending, isAnswering) {
                    if (learn.hasLetterGaps && input.isNotEmpty() && !input.endsWith(' ') && pending == null && !isAnswering) {
                        delay(LETTER_GAP_MS.milliseconds)
                        input += " "
                    }
                }
                // Clear an unfinished input after 3 seconds without a new press.
                LaunchedEffect(input, pending, isAnswering) {
                    if (input.isNotEmpty() && pending == null && !isAnswering) {
                        delay(INPUT_CLEAR_DELAY_MS.milliseconds)
                        input = ""
                    }
                }

                TypedMorse(input = input, pending = pending.takeIf { !isAnswering })

                // When the first press of the current letter went down, for measuring sending speed.
                var letterStartMs by remember { mutableLongStateOf(0L) }
                // Last measured speed; the count restarts the flying animation even when the value repeats.
                var flyingWpm by remember { mutableStateOf<Int?>(null) }
                var flyingCount by remember { mutableIntStateOf(0) }

                fun type(symbol: Char, downMs: Long, upMs: Long) {
                    if (isAnswering) return
                    if (input.isEmpty()) letterStartMs = downMs
                    input += symbol
                    if (input.length >= learn.morse.length) {
                        val correct = input == learn.morse
                        if (correct) {
                            flyingWpm = sendingWpm(learn.morse, upMs - letterStartMs)
                            flyingCount++
                        }
                        // Keep the typed morse on screen for a moment before flipping to the result.
                        answer(correct, revealDelayMs = 1000)
                    }
                }

                // Classify the press on release by how long it was held. Unlike combinedClickable this
                // reacts immediately (no long-press timeout) and doesn't vibrate.
                // A lambda, not ::type — references to the same local fun compare equal, so the state
                // would keep the first chapter's `type` (stale `input`/`learn`) and never update.
                val onSymbol by rememberUpdatedState<(Char, Long, Long) -> Unit> { symbol, down, up ->
                    type(
                        symbol,
                        down,
                        up
                    )
                }
                val pressed = pressStartMs != null
                val keyScale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "keyScale")
                val keyColor = MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier.padding(top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .graphicsLayer {
                                scaleX = keyScale
                                scaleY = keyScale
                            }
                            .shadow(if (pressed) 2.dp else 10.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(keyColor.copy(alpha = if (pressed) 0.75f else 0.85f), keyColor)
                                )
                            )
                            .pointerInput(Unit) {
                                // Each finger is tracked on its own: its press starts on down and is typed on its
                                // own up. A per-gesture detector would wait for every finger to lift before seeing
                                // the next press, dropping fast overlapping taps (-.- came out as --).
                                awaitPointerEventScope {
                                    val downAt = mutableMapOf<PointerId, Long>()
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { change ->
                                            if (change.changedToDownIgnoreConsumed()) {
                                                if (downAt.isEmpty()) keyDown()
                                                downAt[change.id] = change.uptimeMillis
                                                pressStartMs = change.uptimeMillis
                                                change.consume()
                                            } else if (change.changedToUpIgnoreConsumed()) {
                                                // Up counts even if the finger slid off the key.
                                                val start = downAt.remove(change.id) ?: return@forEach
                                                if (downAt.isEmpty()) keyUp()
                                                pressStartMs = downAt.values.maxOrNull()
                                                val held = change.uptimeMillis - start
                                                onSymbol(if (held >= DASH_THRESHOLD_MS) '-' else '.', start, change.uptimeMillis)
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.TouchApp,
                            contentDescription = stringResource(R.string.learn_morse_key),
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    flyingWpm?.let { FlyingWpm(wpm = it, trigger = flyingCount) }
                }
                Text(
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                    text = when {
                        learn.hasLetterGaps -> stringResource(R.string.learn_hint_word)
                        // A prosign like SOS is one character: a pause would split it.
                        learn.target.length > 1 -> stringResource(R.string.learn_hint_prosign)
                        else -> stringResource(R.string.learn_hint_letter)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private const val KEY_TIPS_SEEN = "tips_seen"
// Presses held at least this long count as a dash.
private const val DASH_THRESHOLD_MS = 150L
// In a word, a pause at least this long between presses separates two letters.
private const val LETTER_GAP_MS = 500L
private const val INPUT_CLEAR_DELAY_MS = 3000L
private const val REVEAL_AFTER_WRONG = 3
// Longest code, in symbols and gaps, the letter card shows; five-letter words fit.
private const val MAX_CARD_MORSE = 26

/**
 * Sending speed for a letter or word in PARIS words per minute: a dot is 1 unit, a dash 3, the gaps
 * between symbols 1 each and between letters 3, so wpm = 1200 * units / elapsed Ms.
 * Measured from the first press to the last release.
 */
private fun sendingWpm(morse: String, elapsedMs: Long): Int {
    val letters = morse.split(' ')
    val units = letters.sumOf { letter -> letter.sumOf { if (it == '-') 3 else 1 } + (letter.length - 1) } +
        3 * (letters.size - 1)
    return (1200 * units / elapsedMs.coerceAtLeast(1)).toInt().coerceIn(1, 99)
}

// How far the WPM readout rises: from the key, past the typed code, up over the letter card.
private val FLY_DISTANCE = 320.dp

/** "18 WPM" rising from the key and fading out; restarts whenever [trigger] changes. */
@Composable
private fun FlyingWpm(wpm: Int, trigger: Int) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(2000, easing = LinearOutSlowInEasing))
    }
    Text(
        modifier = Modifier.graphicsLayer {
            translationY = -progress.value * FLY_DISTANCE.toPx()
            // Fade in quickly, then out over the rest of the flight.
            alpha = if (progress.value < 0.15f) progress.value / 0.15f else 1f - (progress.value - 0.15f) / 0.85f
            val scale = 0.8f + 0.4f * progress.value
            scaleX = scale
            scaleY = scale
        },
        text = stringResource(R.string.wpm_value, wpm),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.tertiary
    )
}

// Deep shades on light backgrounds, brighter ones on dark where the deep ones turn murky.
private val CorrectColor: Color @Composable get() = if (isDarkTheme()) Color(0xFF66BB6A) else Color(0xFF2E7D32)
private val WrongColor: Color @Composable get() = if (isDarkTheme()) Color(0xFFEF5350) else Color(0xFFC62828)

/** Close button, a rounded progress bar and the step counter. */
@Composable
private fun LessonTopBar(step: Int, stepCount: Int, onClose: () -> Unit) {
    val progress by animateFloatAsState(step / stepCount.toFloat(), label = "progress")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.learn_close))
        }
        LinearProgressIndicator(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(CircleShape),
            progress = { progress },
            drawStopIndicator = {},
            gapSize = 0.dp
        )
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = "${step + 1}/$stepCount",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** What kind of step this is, and what the user should do. */
@Composable
private fun StepHeader(learn: LearnModel) {
    // Whole sentences per kind of step, not a noun dropped into one, so each language words it its own way.
    val isSignal = learn.meaning != null
    fun pick(letter: Int, word: Int, signal: Int) = when {
        isSignal -> signal
        learn.isWord -> word
        else -> letter
    }
    val (icon, label, instruction) = when {
        learn.isMeaning -> Triple(Icons.Filled.Lightbulb, R.string.learn_label_meaning, R.string.learn_meaning_question)
        learn.isListen && learn.isHidden -> Triple(
            Icons.Filled.Hearing,
            R.string.learn_label_quiz,
            pick(R.string.learn_quiz_letter, R.string.learn_quiz_word, R.string.learn_quiz_signal),
        )
        learn.isListen -> Triple(
            Icons.AutoMirrored.Filled.VolumeUp,
            pick(R.string.learn_label_new_letter, R.string.learn_label_new_word, R.string.learn_label_new_signal),
            pick(R.string.learn_listen_letter, R.string.learn_listen_word, R.string.learn_listen_signal),
        )
        learn.isMorseHidden -> Triple(
            Icons.Filled.Psychology,
            R.string.learn_label_memory,
            pick(R.string.learn_tap_letter, R.string.learn_tap_word, R.string.learn_tap_signal),
        )
        else -> Triple(Icons.Filled.TouchApp, R.string.learn_label_tap, R.string.learn_tap_shown)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(instruction),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * The letter card. [rotation] 0 shows the letter (or headphones in a quiz) on a light card with a
 * light running around its border; 180 shows the result. Content switches at 90°, when the card is
 * edge-on. [revealed] shows a quiz letter and its morse after repeated misses.
 */
@Composable
private fun FlipCard(learn: LearnModel, rotation: Float, isCorrect: Boolean, revealed: Boolean, lessonName: String) {
    val showBack = rotation > 90f
    val hidden = learn.isHidden && !revealed
    val resultColor = if (isCorrect) CorrectColor else WrongColor
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    Surface(
        modifier = Modifier
            .sizeIn(maxWidth = 280.dp, maxHeight = 280.dp)
            .fillMaxHeight(0.9f)
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .then(if (showBack) Modifier else Modifier.runningBorder(CardShapeRadius, accent)),
        shape = RoundedCornerShape(CardShapeRadius),
        // Opaque, so the background pattern doesn't show through.
        color = (if (showBack) resultColor.copy(alpha = 0.12f) else accent.copy(alpha = 0.05f)).compositeOver(colors.surface),
        border = if (showBack) BorderStroke(2.dp, resultColor) else null,
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (showBack) {
                Column(
                    // Counter-rotate so the back side isn't mirrored.
                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = resultColor
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(if (isCorrect) R.string.learn_correct else R.string.learn_try_again),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = resultColor
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (hidden) {
                        // An icon, not "?", which is itself a symbol taught in Punctuation.
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .background(accent.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Headphones, contentDescription = null, tint = accent, modifier = Modifier.size(60.dp))
                        }
                    } else {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = learn.target,
                            // Longer words and phrases get smaller type so they fit the card.
                            fontSize = when {
                                learn.target.length <= 2 -> 96.sp
                                learn.target.length <= 5 -> 60.sp
                                else -> 36.sp
                            },
                            textAlign = TextAlign.Center,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    }
                    // A phrase's code is too long for one line on the card; it's heard instead.
                    if (!hidden && !learn.isMorseHidden && learn.morse.length <= MAX_CARD_MORSE) {
                        Spacer(Modifier.height(12.dp))
                        // Smaller dots for long codes (words, $) so they fit the card.
                        MorseSymbols(learn.morse, dotSize = if (learn.morse.length > 5) 10.dp else 16.dp)
                    }
                    if (!hidden && learn.meaning != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(learn.meaning),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .background(accent.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    text = lessonName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
        }
    }
}

/**
 * Answer tiles in equal-width rows: 2 per row up to 4 letters, then 3, then 4.
 * [isTextAnswer] tiles hold a phrase, like a signal's meaning, so they stay 2 per row in smaller type.
 */
@Composable
private fun AnswerGrid(
    keys: List<String>,
    showMorse: Boolean,
    isTextAnswer: Boolean = false,
    picked: String?,
    isCorrect: Boolean,
    enabled: Boolean,
    onPick: (String) -> Unit,
) {
    val columns = when {
        isTextAnswer || keys.size <= 4 -> 2
        keys.size <= 6 -> 3
        else -> 4
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        keys.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    AnswerTile(
                        modifier = Modifier.weight(1f),
                        key = key,
                        showMorse = showMorse,
                        isTextAnswer = isTextAnswer,
                        result = if (key == picked) isCorrect else null,
                        enabled = enabled,
                        onClick = { onPick(key) }
                    )
                }
                // Keep a short last row's tiles the same width as the others.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** [result] is null until this tile is picked, then whether it was the right answer. */
@Composable
private fun AnswerTile(
    key: String,
    showMorse: Boolean,
    isTextAnswer: Boolean,
    result: Boolean?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = when (result) {
        true -> CorrectColor
        false -> WrongColor
        null -> MaterialTheme.colorScheme.outlineVariant
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(if (showMorse) 76.dp else 64.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (result != null) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (result != null) 2.dp else 1.dp, accent)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = key,
                style = if (isTextAnswer) MaterialTheme.typography.titleSmall else MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontWeight = FontWeight.Bold,
                color = if (result != null) accent else MaterialTheme.colorScheme.onSurface
            )
            if (showMorse) {
                Spacer(Modifier.height(6.dp))
                MorseSymbols(morseOf(key), dotSize = 6.dp)
            }
        }
    }
}

private val CardShapeRadius = 32.dp
private val RunningBorderWidth = 3.dp

/**
 * A border with a light running around it: a sweep of [accent] and amber, turning once every
 * few seconds, over a faint full outline so the card's edge always shows.
 */
@Composable
private fun Modifier.runningBorder(radius: Dp, accent: Color): Modifier {
    val angle by rememberInfiniteTransition(label = "border").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "borderAngle",
    )
    val sweep = Brush.sweepGradient(
        listOf(Color.Transparent, accent.copy(alpha = 0.5f), accent, StarColor, Color.Transparent)
    )
    return drawWithCache {
        val width = RunningBorderWidth.toPx()
        val corner = CornerRadius(radius.toPx())
        // The ring between the card's edge and the inside of the border.
        val ring = Path().apply {
            op(
                Path().apply { addRoundRect(RoundRect(size.toRect(), corner)) },
                Path().apply { addRoundRect(RoundRect(size.toRect().deflate(width), CornerRadius(radius.toPx() - width))) },
                PathOperation.Difference,
            )
        }
        val reach = hypot(size.width, size.height)
        onDrawWithContent {
            drawContent()
            clipPath(ring) {
                drawPath(ring, accent.copy(alpha = 0.18f))
                rotate(angle) { drawCircle(sweep, radius = reach) }
            }
        }
    }
}

/** The mores typed so far, with the press in progress shown faded. Keeps its height when empty. */
@Composable
private fun TypedMorse(input: String, pending: Char?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            // A light wash of the theme color with an outline, so it reads as an input over the pattern.
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f).compositeOver(MaterialTheme.colorScheme.surface), RoundedCornerShape(16.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (input.isEmpty() && pending == null) {
            Text(
                text = stringResource(R.string.learn_code_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            MorseSymbols(input, dotSize = if (input.length > 6) 10.dp else 14.dp)
            if (pending != null) {
                MorseSymbols(
                    pending.toString(),
                    dotSize = 14.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun TipRow(action: String, detail: String, symbol: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = symbol,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview
@Composable
private fun LearnScreenPreview(){
    LearnScreen(lesson = lessons.first(), learns = learnsFor(lessons.first().id), isLastLesson = false)
}


data class LessonModel(
    val id:Long,
    val title: LessonTitle,
    // The symbols taught, which are also the answer tiles. A list, since "," is itself a symbol.
    val options: List<String>,
    val chapter:Int,
    val kind: LessonKind = LessonKind.Symbols,
    // Speed drills play at this speed, whatever the user's setting, and without Farnsworth gaps.
    val wpm: Int? = null,
)

enum class LessonKind {
    // Letters, digits or punctuation, one or two new per lesson, and their reviews.
    Symbols,
    // Two-letter words, typed with letter gaps.
    Words,
    // Signals like SOS, each with a meaning that is quizzed too.
    Signals,
}
data class  LearnModel(
    val id: Long,
    val lessonId: Long,
    val position:Int,
    val target: String,
    val morse: String,
    val isListen: Boolean,
    val isLocked: Boolean,
    // Quiz chapter: the letter is shown as 🎧 and the user has to recognize it by sound.
    val isHidden: Boolean = false,
    val isMorseHidden: Boolean= false,
    // What a signal means; null outside signal lessons.
    @StringRes val meaning: Int? = null,
    // Meaning quiz: the signal is heard and the user picks its meaning instead of its name.
    val isMeaning: Boolean = false,
    // The answer tiles for this step, when it offers a few of the lesson's items rather than all of them.
    val choices: List<String>? = null,
) {
    // A step in a word lesson: the target is a whole word, typed with letter gaps.
    val isWord: Boolean get() = target.length > 1 && meaning == null

    // Words and signals like CQ have gaps between their letters; prosigns like SOS don't.
    val hasLetterGaps: Boolean get() = ' ' in morse
}

val morseMap=hashMapOf(
    "A" to ".-", "B" to "-...", "C" to "-.-.", "D" to "-..", "E" to ".", "F" to "..-.",
    "G" to "--.", "H" to "....", "I" to "..", "J" to ".---", "K" to "-.-", "L" to ".-..",
    "M" to "--", "N" to "-.", "O" to "---", "P" to ".--.", "Q" to "--.-", "R" to ".-.",
    "S" to "...", "T" to "-", "U" to "..-", "V" to "...-", "W" to ".--", "X" to "-..-",
    "Y" to "-.--", "Z" to "--..",
    "0" to "-----", "1" to ".----", "2" to "..---", "3" to "...--", "4" to "....-",
    "5" to ".....", "6" to "-....", "7" to "--...", "8" to "---..", "9" to "----.",
    "." to ".-.-.-", "," to "--..--", "?" to "..--..", "'" to ".----.", "!" to "-.-.--",
    "/" to "-..-.", "(" to "-.--.", ")" to "-.--.-", "&" to ".-...", ":" to "---...",
    ";" to "-.-.-.", "=" to "-...-", "+" to ".-.-.", "-" to "-....-", "_" to "..--.-",
    "\"" to ".-..-.", "$" to "...-..-", "@" to ".--.-.",
)

/**
 * Morse for a symbol, word or phrase: a space between the letters of a word ("IT" is ".. -") and
 * " / " between words ("SO IT" is "... --- / .. -").
 */
fun morseOf(text: String): String =
    text.split(' ').filter { it.isNotEmpty() }.joinToString(" / ") { word ->
        word.map { morseMap.getValue(it.toString()) }.joinToString(" ")
    }
