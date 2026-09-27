package com.learn.morse.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.learn.morse.progress.LessonProgress

@Composable
fun LessonsScreen(onLessonClick: (id: Long, resume: Boolean) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    // Re-read on every visit: the screen leaves composition while a lesson is open.
    val completedCount = remember { LessonProgress.completedCount(context) }
    val currentStep = remember {
        lessons.getOrNull(completedCount)?.let { LessonProgress.savedStep(context, it.id) } ?: 0
    }
    LessonsContent(completedCount, currentStep, onLessonClick, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonsContent(
    completedCount: Int,
    currentStep: Int,
    onLessonClick: (id: Long, resume: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val completed = lessons.take(completedCount)
    val current = lessons.getOrNull(completedCount)
    val locked = lessons.drop(completedCount + 1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learn") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                if (current != null) {
                    CurrentLessonCard(
                        lesson = current,
                        completedCount = completedCount,
                        currentStep = currentStep,
                        onClick = { onLessonClick(current.id, true) },
                    )
                } else {
                    AllCompleteCard(onReview = { onLessonClick(lessons.first().id, false) })
                }
            }

            if (completed.isNotEmpty()) {
                item { SectionHeader("Completed", completed.size) }
                items(completed, key = { it.id }) { lesson ->
                    LessonRow(
                        lesson = lesson,
                        icon = Icons.Filled.CheckCircle,
                        iconTint = MaterialTheme.colorScheme.primary,
                        trailing = Icons.Filled.Replay,
                        onClick = { onLessonClick(lesson.id, false) },
                    )
                }
            }

            if (locked.isNotEmpty()) {
                item { SectionHeader("Locked", locked.size) }
                items(locked, key = { it.id }) { lesson ->
                    LessonRow(
                        lesson = lesson,
                        icon = Icons.Filled.Lock,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alpha(0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentLessonCard(lesson: LessonModel, completedCount: Int, currentStep: Int, onClick: () -> Unit) {
    val stepCount = remember(lesson.id) { learnsFor(lesson.id).size }
    val isStarted = completedCount > 0 || currentStep > 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = if (isStarted) "Up next · Lesson ${lesson.id}" else "Lesson ${lesson.id}",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = lesson.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            LetterChips(lesson)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { currentStep / stepCount.toFloat() },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (currentStep > 0) "Step ${currentStep + 1} of $stepCount" else "$stepCount steps",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "$completedCount of ${lessons.size} lessons completed",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = onClick,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isStarted) "Continue learning" else "Start learning",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun AllCompleteCard(onReview: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = "All lessons completed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Replay any lesson below to keep practising.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            Button(modifier = Modifier.fillMaxWidth(), onClick = onReview) {
                Icon(Icons.Filled.Replay, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start again")
            }
        }
    }
}

/** The lesson's letters with their morse, e.g. "E ." and "T -". */
@Composable
private fun LetterChips(lesson: LessonModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        lesson.options.split(",").forEach { letter ->
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(letter, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = morseMap[letter].orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Text(
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        text = "$title ($count)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun LessonRow(
    lesson: LessonModel,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    trailing: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lesson ${lesson.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = lesson.name, style = MaterialTheme.typography.titleMedium)
            }
            if (trailing != null) {
                Icon(trailing, contentDescription = "Replay", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    val shape = RoundedCornerShape(14.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors) { content() }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors) { content() }
    }
}

@Preview(showBackground = true)
@Composable
private fun LessonsScreenPreview() {
    LessonsContent(completedCount = 1, currentStep = 5, onLessonClick = { _, _ -> }, onBack = {})
}
