package com.learn.morse.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.learn.morse.audio.MorsePlayer
import com.learn.morse.audio.SoundSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// "MORSE" — played by the test button.
private const val TEST_MORSE = "-- --- .-. ... ."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(SoundSettings.load(context)) }
    fun update(new: SoundSettings) {
        settings = new
        new.save(context)
    }

    val scope = rememberCoroutineScope()
    var testJob by remember { mutableStateOf<Job?>(null) }
    val isPlaying = testJob?.isActive == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Sound", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            SettingSlider(
                label = "Tone",
                valueText = "${settings.frequency} Hz",
                value = settings.frequency.toFloat(),
                range = SoundSettings.FREQUENCY_RANGE.first.toFloat()..SoundSettings.FREQUENCY_RANGE.last.toFloat(),
                steps = (SoundSettings.FREQUENCY_RANGE.last - SoundSettings.FREQUENCY_RANGE.first) / 50 - 1,
                onChange = { update(settings.copy(frequency = it.roundToInt())) }
            )
            SettingSlider(
                label = "Speed",
                valueText = "${settings.wpm} WPM",
                value = settings.wpm.toFloat(),
                range = SoundSettings.WPM_RANGE.first.toFloat()..SoundSettings.WPM_RANGE.last.toFloat(),
                steps = SoundSettings.WPM_RANGE.last - SoundSettings.WPM_RANGE.first - 1,
                onChange = { update(settings.copy(wpm = it.roundToInt())) }
            )
            SettingSlider(
                label = "Volume",
                valueText = "${(settings.volume * 100).roundToInt()}%",
                value = settings.volume,
                range = 0.1f..1f,
                onChange = { update(settings.copy(volume = it)) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { update(SoundSettings()) }) { Text("Reset") }
                Button(
                    onClick = {
                        testJob?.cancel()
                        if (!isPlaying) {
                            testJob = scope.launch {
                                MorsePlayer.play(TEST_MORSE, settings.unitMs, settings.frequency.toDouble(), settings.volume)
                            }.also { job -> job.invokeOnCompletion { if (testJob == job) testJob = null } }
                        } else {
                            testJob = null
                        }
                    }
                ) {
                    Icon(if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPlaying) "Stop" else "Test sound")
                }
            }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(valueText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}
