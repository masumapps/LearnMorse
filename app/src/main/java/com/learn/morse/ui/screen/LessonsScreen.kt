package com.learn.morse.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.learn.morse.R
import com.learn.morse.monetization.Ads
import com.learn.morse.monetization.Entitlements
import com.learn.morse.progress.LessonProgress
import kotlinx.coroutines.launch

@Composable
fun LessonsScreen(courseId: Int, onLessonClick: (id: Long, resume: Boolean) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val course = remember(courseId) { courses.first { it.id == courseId } }
    // Re-read on every visit: the screen leaves composition while a lesson is open.
    val completedCount = remember { LessonProgress.completedIn(context, course) }
    val currentStep = remember {
        course.lessons.getOrNull(completedCount)?.let { LessonProgress.savedStep(context, it.id) } ?: 0
    }
    val stars = remember { course.lessons.associate { it.id to LessonProgress.stars(context, it.id) } }
    // Collected so the lock goes away as soon as the lesson is unlocked.
    val isPremium by Entitlements.isPremium.collectAsState()
    val unlockedLessons by Entitlements.unlockedLessons.collectAsState()
    val currentLocked = remember(isPremium, unlockedLessons) {
        course.lessons.getOrNull(completedCount)?.let { !Entitlements.canOpen(context, it.id) } ?: false
    }
    LessonsContent(course, completedCount, currentStep, stars, currentLocked, onLessonClick, onBack)
}

@Composable
private fun LessonsContent(
    course: Course,
    completedCount: Int,
    currentStep: Int,
    stars: Map<Long, Int>,
    currentLocked: Boolean,
    onLessonClick: (id: Long, resume: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val lessons = course.lessons
    val completed = lessons.take(completedCount)
    val current = lessons.getOrNull(completedCount)
    val locked = lessons.drop(completedCount + 1)
    var showMap by rememberSaveable { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val lockedMessage = current?.let { stringResource(R.string.lessons_finish_to_unlock, lessonNumber(it)) }
    val scope = rememberCoroutineScope()

    Scaffold(
        // Shows the app's Morse pattern background, drawn in MainActivity.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            ProgressTopBar(
                title = stringResource(course.name),
                subtitle = stringResource(course.description),
                lessonsDone = completedCount,
                lessonCount = lessons.size,
                starCount = stars.values.sum(),
                onBack = onBack,
                badge = course.badge,
                actions = {
                    IconButton(onClick = { showMap = !showMap }) {
                        if (showMap) Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.lessons_show_list))
                        else Icon(Icons.Filled.Map, contentDescription = stringResource(R.string.lessons_show_map))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { Ads.Banner() },
    ) { paddingValues ->
        if (showMap) {
            LessonMap(
                course = course,
                completedCount = completedCount,
                currentStep = currentStep,
                stars = stars,
                onLessonClick = onLessonClick,
                onLockedClick = {
                    val message = lockedMessage ?: return@LessonMap
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(message)
                    }
                },
                modifier = Modifier.padding(paddingValues),
                currentLocked = currentLocked,
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                if (current != null) {
                    CurrentLessonCard(
                        lesson = current,
                        lessonCount = lessons.size,
                        completedCount = completedCount,
                        currentStep = currentStep,
                        isLocked = currentLocked,
                        onClick = { onLessonClick(current.id, true) },
                    )
                } else {
                    AllCompleteCard(courseName = stringResource(course.name), onReview = { onLessonClick(lessons.first().id, false) })
                }
            }

            if (completed.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.lessons_completed), completed.size) }
                items(completed, key = { it.id }) { lesson ->
                    LessonRow(
                        lesson = lesson,
                        icon = Icons.Filled.CheckCircle,
                        iconTint = MaterialTheme.colorScheme.primary,
                        trailing = Icons.Filled.Replay,
                        stars = stars[lesson.id] ?: 0,
                        onClick = { onLessonClick(lesson.id, false) },
                    )
                }
            }

            if (locked.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.lessons_locked), locked.size) }
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
private fun CurrentLessonCard(
    lesson: LessonModel,
    lessonCount: Int,
    completedCount: Int,
    currentStep: Int,
    isLocked: Boolean,
    onClick: () -> Unit,
) {
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
                text = if (isStarted) stringResource(R.string.lessons_up_next, lessonNumber(lesson))
                else stringResource(R.string.lesson_number, lessonNumber(lesson)),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = lesson.displayName(),
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
                text = if (currentStep > 0) stringResource(R.string.step_of, currentStep + 1, stepCount)
                else pluralStringResource(R.plurals.steps_count, stepCount, stepCount),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = pluralStringResource(R.plurals.lessons_of_completed, lessonCount, completedCount, lessonCount),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = onClick,
            ) {
                Icon(if (isLocked) Icons.Filled.Lock else Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        isLocked -> stringResource(R.string.lessons_unlock)
                        isStarted -> stringResource(R.string.lessons_continue)
                        else -> stringResource(R.string.lessons_start)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun AllCompleteCard(courseName: String, onReview: () -> Unit) {
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
                text = stringResource(R.string.lessons_course_completed, courseName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.lessons_replay_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            Button(modifier = Modifier.fillMaxWidth(), onClick = onReview) {
                Icon(Icons.Filled.Replay, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.lessons_start_again))
            }
        }
    }
}

/** The lesson's letters with their morse, e.g. "E ." and "T -". */
@Composable
private fun LetterChips(lesson: LessonModel) {
    // Wraps onto more lines for review lessons instead of squeezing the last chips.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val letters = lesson.options
        // Big reviews show letters only, in small squares, so even A-Z stays about four lines tall.
        if (letters.size > 8) {
            letters.forEach { letter ->
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
            return@FlowRow
        }
        letters.forEach { letter ->
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(letter, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                MorseSymbols(codeOf(letter), dotSize = 6.dp)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Text(
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        text = stringResource(R.string.section_count, title, count),
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
    stars: Int = 0,
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
                    text = stringResource(R.string.lesson_number, lessonNumber(lesson)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = lesson.displayName(), style = MaterialTheme.typography.titleMedium)
                // Lessons finished before stars existed have none recorded, so show nothing rather than zero.
                if (stars > 0) StarRating(stars, modifier = Modifier.padding(top = 4.dp), size = 16.dp)
            }
            if (trailing != null) {
                Icon(trailing, contentDescription = stringResource(R.string.replay), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    LessonsContent(course = courses.first(), completedCount = 2, currentStep = 5, stars = mapOf(1L to 3, 2L to 2), currentLocked = false, onLessonClick = { _, _ -> }, onBack = {})
}
