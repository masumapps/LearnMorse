package com.learn.morse.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.learn.morse.R

/**
 * Shown when a lesson is finished: the stars earned and where to go next. [isLastLesson] hides
 * "Next lesson"; [isPractice] also drops the hint about replaying for stars, which practice doesn't keep.
 */
@Composable
fun LessonCompleteDialog(
    stars: Int,
    mistakes: Int,
    isLastLesson: Boolean,
    isPractice: Boolean = false,
    onNext: () -> Unit,
    onBackToLessons: () -> Unit,
) {
    Dialog(
        onDismissRequest = onBackToLessons,
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StarRating(stars = stars, size = 56.dp, animate = true)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = when (stars) {
                        3 -> stringResource(R.string.complete_excellent)
                        2 -> stringResource(R.string.complete_great)
                        else -> stringResource(if (isPractice) R.string.complete_practice else R.string.complete_lesson)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (mistakes == 0) stringResource(R.string.complete_perfect)
                    else pluralStringResource(R.plurals.mistakes_count, mistakes, mistakes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (stars < 3 && !isPractice) {
                    Text(
                        text = stringResource(R.string.complete_replay_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(24.dp))
                if (isLastLesson) {
                    Button(modifier = Modifier.fillMaxWidth().height(52.dp), onClick = onBackToLessons) {
                        Text(stringResource(R.string.action_done), style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(modifier = Modifier.fillMaxWidth().height(52.dp), onClick = onNext) {
                        Text(stringResource(R.string.complete_next), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                    TextButton(onClick = onBackToLessons) { Text(stringResource(R.string.complete_back)) }
                }
            }
        }
    }
}

@Preview
@Composable
private fun LessonCompleteDialogPreview() {
    LessonCompleteDialog(stars = 2, mistakes = 4, isLastLesson = false, onNext = {}, onBackToLessons = {})
}
