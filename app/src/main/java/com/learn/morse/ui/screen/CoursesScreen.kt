package com.learn.morse.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.learn.morse.R
import com.learn.morse.monetization.Ads
import com.learn.morse.progress.LessonProgress

/** One course's progress, as the Learn menu shows it. */
private class CourseProgress(
    val course: Course,
    val completed: Int,
    val unlocked: Boolean,
    val stars: Int,
) {
    val total: Int get() = course.lessons.size
    val isDone: Boolean get() = completed == total
    val nextLesson: LessonModel? get() = course.lessons.getOrNull(completed)
    // Every lesson can earn up to 3 stars.
    val maxStars: Int get() = total * 3
}

/** The Learn menu: Alphabet, Numbers, Punctuation and Signals, each unlocked by finishing the one before. */
@Composable
fun CoursesScreen(onCourseClick: (courseId: Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    // Re-read on every visit: the screen leaves composition while a course is open.
    val progress = remember {
        courses.map { course ->
            CourseProgress(
                course = course,
                completed = LessonProgress.completedIn(context, course),
                unlocked = LessonProgress.isUnlocked(context, course),
                stars = course.lessons.sumOf { LessonProgress.stars(context, it.id) },
            )
        }
    }
    CoursesContent(progress, onCourseClick, onBack)
}

@Composable
private fun CoursesContent(
    progress: List<CourseProgress>,
    onCourseClick: (courseId: Int) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        // Shows the app's Morse pattern background, drawn in MainActivity.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            ProgressTopBar(
                title = stringResource(R.string.courses_title),
                subtitle = stringResource(R.string.courses_subtitle),
                lessonsDone = progress.sumOf { it.completed },
                lessonCount = lessons.size,
                starCount = progress.sumOf { it.stars },
                onBack = onBack,
                badgeIcon = Icons.Filled.School,
            )
        },
        bottomBar = { Ads.Banner() },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(progress, key = { it.course.id }) { course ->
                val index = progress.indexOf(course)
                when {
                    !course.unlocked -> LockedCourseCard(course, previousName = progress.getOrNull(index - 1)?.course?.name?.let { stringResource(it) })
                    course.isDone -> DoneCourseCard(course, onClick = { onCourseClick(course.course.id) })
                    else -> CurrentCourseCard(course, accent = accentFor(index), onClick = { onCourseClick(course.course.id) })
                }
            }
        }
    }
}

/** A gradient and the color of text on it, taken from the theme so dynamic color and dark mode follow. */
private class Accent(val from: Color, val to: Color, val content: Color)

@Composable
private fun accentFor(index: Int): Accent {
    val c = MaterialTheme.colorScheme
    return when (index % 3) {
        0 -> Accent(c.primary, c.tertiary, c.onPrimary)
        1 -> Accent(c.tertiary, c.primary, c.onTertiary)
        else -> Accent(c.secondary, c.tertiary, c.onSecondary)
    }
}

/** The course being worked on: a large card with the next lesson and a button straight into the course. */
@Composable
private fun CurrentCourseCard(progress: CourseProgress, accent: Accent, onClick: () -> Unit) {
    val course = progress.course
    val next = progress.nextLesson
    val isNew = progress.completed == 0
    val onCard = accent.content
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(accent.from, accent.to)))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(onCard.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(course.badge, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = onCard)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(course.name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onCard)
                    Text(stringResource(course.description), style = MaterialTheme.typography.bodyMedium, color = onCard.copy(alpha = 0.85f))
                }
                Pill(text = stringResource(if (isNew) R.string.courses_new else R.string.courses_in_progress), container = onCard.copy(alpha = 0.18f), content = onCard)
            }

            Spacer(Modifier.height(16.dp))
            SymbolPreview(course, color = onCard, chip = onCard.copy(alpha = 0.14f))

            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                progress = { progress.completed / progress.total.toFloat() },
                color = onCard,
                trackColor = onCard.copy(alpha = 0.25f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pluralStringResource(R.plurals.courses_lessons_of, progress.total, progress.completed, progress.total),
                    style = MaterialTheme.typography.bodySmall,
                    color = onCard.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f),
                )
                StarCount(progress.stars, progress.maxStars, color = onCard.copy(alpha = 0.85f))
            }

            if (next != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = onCard, contentColor = accent.from),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(if (isNew) R.string.courses_start_next else R.string.courses_continue_next, next.displayName()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** A finished course: compact, with its stars, and still open for practice. */
@Composable
private fun DoneCourseCard(progress: CourseProgress, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(progress.course.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.courses_completed_prefix),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    StarCount(progress.stars, progress.maxStars, color = colors.onSurfaceVariant)
                }
            }
            Text(stringResource(R.string.courses_practice), style = MaterialTheme.typography.labelLarge, color = colors.primary)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.primary)
        }
    }
}

/** A course not reached yet: muted, saying which course unlocks it, with a taste of what it teaches. */
@Composable
private fun LockedCourseCard(progress: CourseProgress, previousName: String?) {
    val colors = MaterialTheme.colorScheme
    val muted = colors.onSurfaceVariant.copy(alpha = 0.7f)
    val hint = previousName?.let { stringResource(R.string.courses_finish_to_unlock, it) } ?: stringResource(R.string.state_locked)
    val name = stringResource(progress.course.name)
    val lockedDescription = stringResource(R.string.courses_locked_cd, name, hint)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surfaceContainerLow)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(22.dp))
            .padding(16.dp)
            .clearAndSetSemantics { contentDescription = lockedDescription },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = muted)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = muted)
                Text("${stringResource(progress.course.description)} · ${pluralStringResource(R.plurals.lessons_count, progress.total, progress.total)}", style = MaterialTheme.typography.bodySmall, color = muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        SymbolPreview(progress.course, color = muted, chip = colors.surfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = muted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(hint, style = MaterialTheme.typography.labelMedium, color = muted)
        }
    }
}

/** The first few symbols a course teaches, each with its morse, e.g. "E ·" and "T –". */
@Composable
private fun SymbolPreview(course: Course, color: Color, chip: Color) {
    val symbols = remember(course.id) {
        val taught = course.lessons.filter { it.kind != LessonKind.Words }.flatMap { it.options }.distinct()
        when {
            // A course of words previews its first ones; their codes are long, so two fit.
            taught.isEmpty() -> course.lessons.first().options.take(2)
            // Signals like SOS are long in morse, so fewer fit on a line.
            taught.any { it.length > 1 } -> taught.take(2)
            else -> taught.take(4)
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clearAndSetSemantics {},
    ) {
        symbols.forEach { symbol ->
            Row(
                modifier = Modifier
                    .background(chip, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(symbol, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
                Spacer(Modifier.width(6.dp))
                MorseSymbols(codeOf(symbol), dotSize = 5.dp, color = color)
            }
        }
    }
}

@Composable
private fun StarCount(stars: Int, maxStars: Int, color: Color) {
    val description = pluralStringResource(R.plurals.stars_of, maxStars, stars, maxStars)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(3.dp))
        Text("$stars/$maxStars", style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun Pill(text: String, container: Color, content: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = content,
        modifier = Modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CoursesScreenPreview() {
    CoursesContent(
        progress = listOf(
            CourseProgress(courses[0], completed = courses[0].lessons.size, unlocked = true, stars = 80),
            CourseProgress(courses[1], completed = 3, unlocked = true, stars = 8),
            CourseProgress(courses[2], completed = 0, unlocked = false, stars = 0),
            CourseProgress(courses[3], completed = 0, unlocked = false, stars = 0),
        ),
        onCourseClick = {},
        onBack = {},
    )
}
