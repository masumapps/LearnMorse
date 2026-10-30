package com.learn.morse.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.learn.morse.R
import com.learn.morse.monetization.Ads
import com.learn.morse.monetization.Entitlements
import com.learn.morse.progress.LessonProgress
import com.learn.morse.progress.Streak
import com.learn.morse.progress.SymbolStats
import com.learn.morse.reminder.DailyReminder
import com.learn.morse.ui.theme.isDarkTheme

/** What the home screen shows about the user's progress. */
private class HomeProgress(
    // The next lesson to take, or null once every course is finished.
    val nextLesson: LessonModel?,
    val nextStep: Int,
    val lessonsDone: Int,
    val starCount: Int,
    val symbolsLearned: Int,
    // Days in a row with a finished lesson or practice; 0 once a day is missed.
    val streak: Int = 0,
    // Symbols missed lately, for the practice tile; practice needs at least two symbols learned.
    val weakCount: Int = 0,
    val canPractice: Boolean = false,
)

@Composable
fun HomeScreen(
    onContinueLesson: (lessonId: Long) -> Unit,
    onLearnClick: () -> Unit,
    onTranslatorClick: () -> Unit,
    onPracticeClick: () -> Unit,
    onChartClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val context = LocalContext.current
    // Re-read on every visit: the screen leaves composition while a lesson is open.
    val progress = remember {
        val done = LessonProgress.completedCount(context)
        val next = lessons.getOrNull(done)
        val learned = learnedSymbols(done)
        HomeProgress(
            nextLesson = next,
            nextStep = next?.let { LessonProgress.savedStep(context, it.id) } ?: 0,
            lessonsDone = done,
            starCount = lessons.sumOf { LessonProgress.stars(context, it.id) },
            symbolsLearned = learned.size,
            streak = Streak.current(context),
            weakCount = SymbolStats.weak(context, learned).size,
            canPractice = learned.size >= 2,
        )
    }
    // Offered once, after the first lesson, when a reminder makes sense to the user.
    var offerReminder by remember {
        mutableStateOf(progress.lessonsDone > 0 && !DailyReminder.wasOffered(context) && !DailyReminder.isEnabled(context))
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) DailyReminder.enable(context) else DailyReminder.markOffered(context)
        offerReminder = false
    }
    // Collected so the lock goes away as soon as the lesson is unlocked.
    val isPremium by Entitlements.isPremium.collectAsState()
    val unlockedLessons by Entitlements.unlockedLessons.collectAsState()
    val nextLocked = remember(isPremium, unlockedLessons) {
        progress.nextLesson?.let { !Entitlements.canOpen(context, it.id) } ?: false
    }
    HomeContent(
        progress = progress,
        nextLocked = nextLocked,
        offerReminder = offerReminder,
        onEnableReminder = {
            if (DailyReminder.canNotify(context)) {
                DailyReminder.enable(context)
                offerReminder = false
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onDismissReminder = {
            DailyReminder.markOffered(context)
            offerReminder = false
        },
        onContinueLesson = onContinueLesson,
        onLearnClick = onLearnClick,
        onTranslatorClick = onTranslatorClick,
        onPracticeClick = onPracticeClick,
        onChartClick = onChartClick,
        onPremiumClick = if (isPremium) null else onPremiumClick,
        onSettingsClick = onSettingsClick,
    )
}

@Composable
private fun HomeContent(
    progress: HomeProgress,
    nextLocked: Boolean,
    offerReminder: Boolean,
    onEnableReminder: () -> Unit,
    onDismissReminder: () -> Unit,
    onContinueLesson: (lessonId: Long) -> Unit,
    onLearnClick: () -> Unit,
    onTranslatorClick: () -> Unit,
    onPracticeClick: () -> Unit,
    onChartClick: () -> Unit,
    // Null once the user has Premium, which hides the crown.
    onPremiumClick: (() -> Unit)?,
    onSettingsClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(6.dp))
                    // "LEARN" in morse.
                    MorseSymbols(".-.. . .- .-. -.", dotSize = 7.dp, modifier = Modifier.clearAndSetSemantics {})
                }
                if (progress.lessonsDone > 0) StreakChip(progress.streak)
                if (onPremiumClick != null) {
                    PremiumBadge(onClick = onPremiumClick)
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                }
            }

            Spacer(Modifier.height(24.dp))
            ContinueCard(progress, nextLocked, onContinueLesson, onLearnClick)

            if (offerReminder) {
                Spacer(Modifier.height(16.dp))
                ReminderCard(onEnable = onEnableReminder, onDismiss = onDismissReminder)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(Icons.Filled.CheckCircle, MaterialTheme.colorScheme.primary, "${progress.lessonsDone}", stringResource(R.string.home_stat_lessons), Modifier.weight(1f))
                StatTile(Icons.Filled.Star, StarColor, "${progress.starCount}", stringResource(R.string.home_stat_stars), Modifier.weight(1f))
                StatTile(Icons.Filled.GraphicEq, SymbolsColor, "${progress.symbolsLearned}", stringResource(R.string.home_stat_symbols), Modifier.weight(1f))
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.home_explore),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeatureTile(
                    icon = Icons.Filled.School,
                    title = stringResource(R.string.home_courses),
                    subtitle = pluralStringResource(R.plurals.home_courses_sub, lessons.size, courses.size, lessons.size),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    accent = MaterialTheme.colorScheme.secondary,
                    onClick = onLearnClick,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                FeatureTile(
                    icon = Icons.Filled.FitnessCenter,
                    title = stringResource(R.string.home_practice),
                    subtitle = when {
                        !progress.canPractice -> stringResource(R.string.home_practice_locked)
                        progress.weakCount > 0 -> pluralStringResource(R.plurals.tricky_symbols, progress.weakCount, progress.weakCount)
                        else -> stringResource(R.string.lesson_title_quick_review)
                    },
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = onPracticeClick,
                    enabled = progress.canPractice,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeatureTile(
                    icon = Icons.Filled.GridView,
                    title = stringResource(R.string.home_chart),
                    subtitle = stringResource(R.string.home_chart_sub),
                    container = ChartColor.copy(alpha = 0.18f).compositeOver(MaterialTheme.colorScheme.surface),
                    content = MaterialTheme.colorScheme.onSurface,
                    accent = ChartColor,
                    onClick = onChartClick,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                FeatureTile(
                    icon = Icons.Filled.SwapHoriz,
                    title = stringResource(R.string.home_translator),
                    subtitle = stringResource(R.string.home_translator_sub),
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                    accent = MaterialTheme.colorScheme.tertiary,
                    onClick = onTranslatorClick,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
        Ads.Banner()
    }
}

/** The next lesson with a button straight into it, or a trophy once every course is done. */
@Composable
private fun ContinueCard(progress: HomeProgress, isLocked: Boolean, onContinueLesson: (Long) -> Unit, onLearnClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val onHero = colors.onPrimary
    val lesson = progress.nextLesson
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
            .padding(20.dp),
    ) {
        if (lesson == null) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = StarColor, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.home_all_done), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = onHero)
            Text(stringResource(R.string.home_all_done_sub), style = MaterialTheme.typography.bodyMedium, color = onHero.copy(alpha = 0.85f))
            Spacer(Modifier.height(16.dp))
            HeroButton(stringResource(R.string.home_browse), onClick = onLearnClick)
            return@Column
        }
        val course = remember(lesson.id) { courseOf(lesson.id) }
        val stepCount = remember(lesson.id) { learnsFor(lesson.id).size }
        val started = progress.nextStep > 0
        Text(
            text = stringResource(if (started || progress.lessonsDone > 0) R.string.home_continue_label else R.string.home_start_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = onHero.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(onHero.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(course.badge, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = onHero)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.course_lesson, stringResource(course.name), lessonNumber(lesson)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onHero.copy(alpha = 0.85f),
                )
                Text(lesson.displayName(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onHero)
            }
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            progress = { progress.nextStep / stepCount.toFloat() },
            color = onHero,
            trackColor = onHero.copy(alpha = 0.25f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (started) stringResource(R.string.step_of, progress.nextStep + 1, stepCount)
            else pluralStringResource(R.plurals.steps_count, stepCount, stepCount),
            style = MaterialTheme.typography.bodySmall,
            color = onHero.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(16.dp))
        HeroButton(
            text = when {
                isLocked -> stringResource(R.string.action_unlock)
                started -> stringResource(R.string.action_continue)
                else -> stringResource(R.string.action_start)
            },
            icon = if (isLocked) Icons.Filled.Lock else Icons.Filled.PlayArrow,
            onClick = { onContinueLesson(lesson.id) },
        )
    }
}

@Composable
private fun HeroButton(text: String, onClick: () -> Unit, icon: ImageVector = Icons.Filled.PlayArrow) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onPrimary,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatTile(icon: ImageVector, iconTint: Color, value: String, label: String, modifier: Modifier = Modifier) {
    val container = iconTint.copy(alpha = 0.16f).compositeOver(MaterialTheme.colorScheme.surface)
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            // A soft wash of the icon's own color, so each stat reads as its own card rather than grey.
            .background(container, shape)
            .border(CardBorderWidth, cardBorder(container, iconTint), shape)
            .padding(vertical = 14.dp, horizontal = 12.dp)
            .clearAndSetSemantics { contentDescription = "$value $label" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The gold crown that opens the Premium offer: a large crown in a gold circle with a bold gold ring. */
@Composable
private fun PremiumBadge(onClick: () -> Unit) {
    val container = StarColor.copy(alpha = 0.16f).compositeOver(MaterialTheme.colorScheme.surface)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        border = BorderStroke(2.5.dp, lerp(container, StarColor, 0.85f)),
        modifier = Modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_crown),
                contentDescription = stringResource(R.string.home_premium),
                tint = StarColor,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

/** Days in a row practiced, shown grey once the streak has lapsed. */
@Composable
private fun StreakChip(days: Int) {
    val active = days > 0
    val description = pluralStringResource(R.plurals.streak_days, days, days)
    Row(
        modifier = Modifier
            .background(
                if (active) StreakColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(50),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = if (active) StreakColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text("$days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private val StreakColor: Color @Composable get() = if (isDarkTheme()) Color(0xFFFF9E40) else Color(0xFFFF6D00)

private val CardBorderWidth = 1.5.dp

/**
 * A deeper shade of a card's own color for its outline, mixed toward the card's [accent] (its icon
 * or text color, the same hue but stronger) rather than black, which would turn it grey.
 */
private fun cardBorder(container: Color, accent: Color): Color = lerp(container, accent, 0.45f)

// Hues the theme's own containers don't use, so these cards stand apart from the others.
// Lighter tints in dark theme, so they stay vivid against dark surfaces.
internal val ChartColor: Color @Composable get() = if (isDarkTheme()) Color(0xFF4DB6AC) else Color(0xFF00897B)
private val SymbolsColor: Color @Composable get() = if (isDarkTheme()) Color(0xFFF06292) else Color(0xFFD81B60)

@Composable
private fun ReminderCard(onEnable: () -> Unit, onDismiss: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.home_reminder_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.home_reminder_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEnable) { Text(stringResource(R.string.home_reminder_yes)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_reminder_no)) }
            }
        }
    }
}

@Composable
private fun FeatureTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // The hue the border deepens toward: the theme color the container is a pale version of.
    accent: Color = content,
) {
    val disabledContainer = container.copy(alpha = 0.5f).compositeOver(MaterialTheme.colorScheme.surface)
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        border = BorderStroke(CardBorderWidth, cardBorder(if (enabled) container else disabledContainer, accent)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content,
            // Opaque, so the background pattern doesn't show through a locked tile.
            disabledContainerColor = disabledContainer,
            disabledContentColor = content.copy(alpha = 0.6f),
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null)
            }
            Spacer(Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.8f))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeContent(
        progress = HomeProgress(lessons[4], nextStep = 6, lessonsDone = 4, starCount = 11, symbolsLearned = 6, streak = 3, weakCount = 2, canPractice = true),
        nextLocked = false,
        offerReminder = true,
        onEnableReminder = {},
        onDismissReminder = {},
        onContinueLesson = {},
        onLearnClick = {},
        onTranslatorClick = {},
        onPracticeClick = {},
        onChartClick = {},
        onPremiumClick = {},
        onSettingsClick = {},
    )
}
