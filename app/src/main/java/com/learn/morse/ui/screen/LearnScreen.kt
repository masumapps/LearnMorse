package com.learn.morse.ui.screen

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.morse.audio.KeyTone
import com.learn.morse.audio.MorsePlayer
import com.learn.morse.audio.SoundSettings
import com.learn.morse.progress.LessonProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LearnScreenRoute(lessonId: Long, resume: Boolean, onLessonComplete: () -> Unit, onBack: () -> Unit){
    val context = LocalContext.current
    val initialChapter = remember(lessonId) { if (resume) LessonProgress.savedStep(context, lessonId) else 0 }
    LearnScreen(
        lessonId = lessonId,
        initialChapter = initialChapter,
        onChapterChange = { LessonProgress.saveStep(context, lessonId, it) },
        onLessonComplete = onLessonComplete,
        onBack = onBack
    )
}

@Composable
private fun LearnScreen(
    lessonId: Long,
    initialChapter: Int = 0,
    onChapterChange: (Int) -> Unit = {},
    onLessonComplete: () -> Unit = {},
    onBack: () -> Unit = {}
){

    val lesson = remember(lessonId) { lessons.first { it.id == lessonId } }
    val learns = remember(lessonId) { learnsFor(lessonId) }

    var chapter by remember { mutableStateOf(initialChapter.coerceIn(0, learns.lastIndex)) }
    // Remember the step so "Continue learning" can resume from it.
    val currentOnChapterChange by rememberUpdatedState(onChapterChange)
    LaunchedEffect(chapter) { currentOnChapterChange(chapter) }
    val context = LocalContext.current
    val sound = remember { SoundSettings.load(context) }

    val learn=learns[ chapter]

    // The lesson waits behind the start dialog: no sound or tips until the user starts it.
    var started by rememberSaveable { mutableStateOf(false) }

    // Bumped on FAB click; plays on start, on chapter change and on every click (restarting any ongoing playback).
    var playRequest by remember { mutableIntStateOf(0) }
    // Keyed on chapter, not learn: consecutive chapters can hold equal LearnModels, which wouldn't restart the effect.
    LaunchedEffect(chapter, playRequest, started) {
        if (started && learn.isListen) MorsePlayer.play(learn.morse, sound.unitMs, sound.frequency.toDouble(), sound.volume)
    }

    val scope = rememberCoroutineScope()
    // Sounds while the circle button is held.
    val keyTone = remember { KeyTone(sound.frequency.toDouble(), sound.volume) }
    DisposableEffect(keyTone) { onDispose { keyTone.close() } }
    // Rotation of the letter card: 0 shows the letter, 180 shows the Right/Wrong result.
    val flip = remember { Animatable(0f) }
    var isCorrect by remember { mutableStateOf(false) }
    var isAnswering by remember { mutableStateOf(false) }

    // Morse typed with the circle button in tap chapters.
    var input by remember(chapter) { mutableStateOf("") }
    // Tips are shown only on the first tap chapter ever; the flag is persisted across launches.
    val prefs = context.getSharedPreferences("learn_morse", Context.MODE_PRIVATE)
    var tipsSeen by remember { mutableStateOf(prefs.getBoolean(KEY_TIPS_SEEN, false)) }
    fun dismissTips() {
        tipsSeen = true
        prefs.edit().putBoolean(KEY_TIPS_SEEN, true).apply()
    }
    val showTips = started && !learn.isListen && !tipsSeen

    fun answer(correct: Boolean, revealDelayMs: Long = 0) {
        if (isAnswering) return
        isAnswering = true
        isCorrect = correct
        scope.launch {
            delay(revealDelayMs)
            flip.animateTo(180f, tween(400))
            delay(700)
            // Switch chapter while the result side is showing, so flipping back reveals the next letter.
            if (isCorrect && chapter == learns.lastIndex) {
                onLessonComplete()
                return@launch
            }
            if (isCorrect) chapter++
            flip.animateTo(0f, tween(400))
            // Replay the sound so the user can try again.
            if (!isCorrect) playRequest++
            input = ""
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
                    MorsePlayer.play(morseMap.getValue(letter), sound.unitMs, sound.frequency.toDouble(), sound.volume)
                }
            },
            onStart = {
                previewJob?.cancel()
                started = true
            },
            onDismiss = onBack
        )
    }

    if (showTips) {
        AlertDialog(
            onDismissRequest = { dismissTips() },
            icon = { Icon(Icons.Filled.TouchApp, contentDescription = null) },
            title = { Text("How to type Morse") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Use the circle button at the bottom of the screen.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TipRow(action = "Tap", detail = "A quick press", symbol = "•")
                    TipRow(action = "Long press", detail = "Press and hold", symbol = "—")
                }
            },
            confirmButton = {
                Button(onClick = { dismissTips() }) { Text("Got it") }
            }
        )
    }

    Scaffold() {paddingValues ->
        Column(
            modifier = Modifier.padding(
                paddingValues
            ).fillMaxSize()
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {

            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = lesson.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            LinearProgressIndicator(
                modifier = Modifier.padding(top = 8.dp),
                progress = {
                    (chapter + 1) / learns.size.toFloat()
                }
            )

            Text(
                text = learn.id.toString()
            )

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                Box(){}
                Column(
                    modifier = Modifier.align(Alignment.Center)
                        .graphicsLayer {
                            rotationY = flip.value
                            cameraDistance = 12f * density
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    if (flip.value <= 90f && learn.isHidden) {
                        Text(
                            text = "?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 46.sp
                        )
                    } else if (flip.value <= 90f) {
                        Text(
                            text = learn.target,
                            fontWeight = FontWeight.Bold,
                            fontSize = 46.sp
                        )

                    if(learn.isMorseHidden.not())    Text(
                            text = morseMap.get(learn.target)?:"",
                            fontWeight = FontWeight.Bold,
                            fontSize = 46.sp
                        )
                    } else {
                        Text(
                            // Counter-rotate so the back side isn't mirrored.
                            modifier = Modifier.graphicsLayer { rotationY = 180f },
                            text = if (isCorrect) "Right" else "Wrong",
                            color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            fontSize = 46.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {

                    if (learn.isListen){
                        FloatingActionButton(
                            modifier = Modifier.padding(8.dp),
                            onClick = { playRequest++ }
                        ) {

                            Icon(Icons.AutoMirrored.Filled.VolumeUp,contentDescription = null)
                        }
                    }
                }

            }

            if(learn.isListen){

                val keys=lesson.options.split(",")
                FlowRow() {
                    keys.forEach {key->
                        KeyView(
                            key=key,
                            size=keys.size,
                            // Quiz steps hide the morse so the letter is recognised by sound, not by counting symbols.
                            showMorse = !learn.isHidden,
                            onClick = { answer(key == learn.target) }
                        )
                    }
                }
            }
            else {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Symbol for the press in progress: '.' on touch, turning into '-' once held long enough.
                    var pending by remember { mutableStateOf<Char?>(null) }
                    // Clear an unfinished input after 3 seconds without a new press.
                    LaunchedEffect(input, pending, isAnswering) {
                        if (input.isNotEmpty() && pending == null && !isAnswering) {
                            delay(INPUT_CLEAR_DELAY_MS)
                            input = ""
                        }
                    }
                    Text(
                        text = buildAnnotatedString {
                            append(input)
                            val preview = pending
                            if (preview != null && !isAnswering) {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(preview) }
                            }
                            if (length == 0) append(" ")
                        },
                        fontSize = 30.sp
                    )

                    fun type(symbol: Char) {
                        if (isAnswering) return
                        input += symbol
                        // Keep the typed morse on screen for a moment before flipping to the result.
                        if (input.length >= learn.morse.length) answer(input == learn.morse, revealDelayMs = 1000)
                    }

                    // Classify the press on release by how long it was held. Unlike combinedClickable this
                    // reacts immediately (no long-press timeout) and doesn't vibrate.
                    // A lambda, not ::type — references to the same local fun compare equal, so the state
                    // would keep the first chapter's `type` (stale `input`/`learn`) and never update.
                    val onSymbol by rememberUpdatedState<(Char) -> Unit>({ type(it) })
                    var pressed by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier.size(100.dp).clip(CircleShape)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown()
                                    pressed = true
                                    pending = '.'
                                    keyTone.start()
                                    var released = false
                                    val quickUp = withTimeoutOrNull(DASH_THRESHOLD_MS) {
                                        waitForUpOrCancellation().also { released = true }
                                    }
                                    val up = if (released) quickUp else {
                                        pending = '-'
                                        waitForUpOrCancellation()
                                    }
                                    keyTone.stop()
                                    pressed = false
                                    pending = null
                                    // Use the same decision as the preview, so what was shown is what gets typed.
                                    if (up != null) onSymbol(if (released) '.' else '-')
                                }
                            }
                            .background(
                                if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )
                }

            }




        }
    }
}

private const val KEY_TIPS_SEEN = "tips_seen"
// Presses held at least this long count as a dash.
private const val DASH_THRESHOLD_MS = 150L
private const val INPUT_CLEAR_DELAY_MS = 3000L

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

@Composable
fun KeyView(key: String, size: Int, showMorse: Boolean = true, onClick: () -> Unit) {

    // Beyond 4 keys the FlowRow wraps, so keep them at the 4-key size instead of shrinking further.
    val scale = size.coerceAtMost(4)
    val fontSize= (50/scale).sp
    val horizontalPadding= (50/scale).dp
    val verticalPadding= (40/scale).dp
    OutlinedButton (
        modifier = Modifier
            .padding(3 .dp)
            .padding(horizontal = horizontalPadding,verticalPadding),
        shape = RoundedCornerShape(10),
     onClick = onClick
    )
   { Column(

       horizontalAlignment = Alignment.CenterHorizontally
   ){
        Text(
            text = key,
            fontSize = fontSize
        )
        if (showMorse) Text(
            text = morseMap.get(key)?:"",
            fontSize = fontSize
        )
    }}
}

@Preview
@Composable
private fun LearnScreenPreview(){
    LearnScreen(lessonId = 1)
}


data class LessonModel(
    val id:Long,
    val name: String,
    val options: String,
    val chapter:Int

)
data class  LearnModel(
    val id: Long,
    val lessonId: Long,
    val position:Int,
    val target: String,
    val morse: String,
    val isListen: Boolean,
    val isLocked: Boolean,
    // Quiz chapter: the letter is shown as "?" and the user has to recognise it by sound.
    val isHidden: Boolean = false,
    val isMorseHidden: Boolean= false
)

val morseMap=hashMapOf<String, String>(
    "E" to ".",
    "T" to "-",
    "A" to ".-",
    "N" to "-.",
    "I" to "..",
    "M" to "--",
    "S" to "...",
    "O" to "---"
)