package com.learn.morse.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.learn.morse.audio.MorsePlayer
import com.learn.morse.audio.SoundSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var toMorse by rememberSaveable { mutableStateOf(true) }
    var input by rememberSaveable { mutableStateOf("") }
    val output = if (toMorse) MorseTranslator.encode(input) else MorseTranslator.decode(input)
    val morse = if (toMorse) output else input

    val scope = rememberCoroutineScope()
    var playJob by remember { mutableStateOf<Job?>(null) }
    val isPlaying = playJob?.isActive == true
    DisposableEffect(Unit) { onDispose { playJob?.cancel() } }

    fun setMode(morseMode: Boolean) {
        if (morseMode == toMorse) return
        // Carry the translation over so switching direction continues from it.
        input = output
        toMorse = morseMode
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Morse ⇄ Text") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = toMorse,
                    onClick = { setMode(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("Text → Morse") }
                SegmentedButton(
                    selected = !toMorse,
                    onClick = { setMode(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Morse → Text") }
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                label = { Text(if (toMorse) "Text" else "Morse") },
                placeholder = {
                    Text(if (toMorse) "Type a message" else "... --- ... / .... ..")
                },
                supportingText = {
                    if (!toMorse) Text("Space between letters, / between words")
                },
                textStyle = if (toMorse) MaterialTheme.typography.bodyLarge
                else MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(
                    capitalization = if (toMorse) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
                    keyboardType = if (toMorse) KeyboardType.Text else KeyboardType.Ascii,
                    autoCorrectEnabled = false,
                ),
                trailingIcon = {
                    if (input.isNotEmpty()) {
                        IconButton(onClick = { input = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
            )

            if (!toMorse) {
                MorseKeypad(
                    onKey = { input += it },
                    onBackspace = { input = input.dropLast(1) },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (toMorse) "Morse" else "Text",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { setMode(!toMorse) }) {
                    Icon(Icons.Filled.SwapVert, contentDescription = "Swap direction")
                }
                IconButton(
                    onClick = { clipboard.setText(AnnotatedString(output)) },
                    enabled = output.isNotEmpty(),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                }
                IconButton(
                    onClick = {
                        playJob?.cancel()
                        if (!isPlaying) {
                            val sound = SoundSettings.load(context)
                            playJob = scope.launch {
                                MorsePlayer.play(morse, sound.unitMs, sound.frequency.toDouble(), sound.volume)
                            }.also { job -> job.invokeOnCompletion { if (playJob == job) playJob = null } }
                        } else {
                            playJob = null
                        }
                    },
                    enabled = morse.any { it == '.' || it == '-' },
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play morse",
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                SelectionContainer(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = output.ifEmpty { "Translation appears here" },
                        style = if (toMorse) MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace)
                        else MaterialTheme.typography.titleLarge,
                        color = if (output.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** Quick entry for dots, dashes and gaps so morse can be typed without hunting the keyboard. */
@Composable
private fun MorseKeypad(onKey: (String) -> Unit, onBackspace: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("·" to ".", "−" to "-", "␣" to " ", "/" to " / ").forEach { (label, value) ->
            FilledTonalButton(onClick = { onKey(value) }, modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleLarge)
            }
        }
        FilledTonalButton(onClick = onBackspace, modifier = Modifier.weight(1f)) {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TranslatorScreenPreview() {
    TranslatorScreen(onBack = {})
}
