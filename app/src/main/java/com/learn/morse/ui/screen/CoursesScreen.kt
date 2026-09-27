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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.learn.morse.progress.LessonProgress

/** The Learn menu: Alphabet, Numbers and Punctuation, each unlocked by finishing the one before. */
@Composable
fun CoursesScreen(onCourseClick: (courseId: Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    // Re-read on every visit: the screen leaves composition while a course is open.
    val completed = remember { courses.associate { it.id to LessonProgress.completedIn(context, it) } }
    val unlocked = remember { courses.associate { it.id to LessonProgress.isUnlocked(context, it) } }
    CoursesContent(completed, unlocked, onCourseClick, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoursesContent(
    completed: Map<Int, Int>,
    unlocked: Map<Int, Boolean>,
    onCourseClick: (courseId: Int) -> Unit,
    onBack: () -> Unit,
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(courses, key = { it.id }) { course ->
                val previous = courses.getOrNull(courses.indexOf(course) - 1)
                CourseCard(
                    course = course,
                    completed = completed[course.id] ?: 0,
                    unlocked = unlocked[course.id] ?: false,
                    lockedHint = previous?.let { "Finish ${it.name} to unlock" },
                    onClick = { onCourseClick(course.id) },
                )
            }
        }
    }
}

@Composable
private fun CourseCard(
    course: Course,
    completed: Int,
    unlocked: Boolean,
    lockedHint: String?,
    onClick: () -> Unit,
) {
    val total = course.lessons.size
    val done = completed == total
    val colors = if (unlocked && !done) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    }
    Card(
        onClick = onClick,
        enabled = unlocked,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (unlocked) 1f else 0.6f),
        shape = RoundedCornerShape(20.dp),
        colors = colors,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    !unlocked -> Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    done -> Icon(Icons.Filled.CheckCircle, contentDescription = "Completed", tint = MaterialTheme.colorScheme.primary)
                    else -> Text(course.badge, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(course.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                if (unlocked) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { completed / total.toFloat() },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("$completed of $total lessons completed", style = MaterialTheme.typography.bodySmall)
                } else if (lockedHint != null) {
                    Text(lockedHint, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (unlocked) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CoursesScreenPreview() {
    CoursesContent(
        completed = mapOf(1 to courses[0].lessons.size, 2 to 3, 3 to 0),
        unlocked = mapOf(1 to true, 2 to true, 3 to false),
        onCourseClick = {},
        onBack = {},
    )
}
