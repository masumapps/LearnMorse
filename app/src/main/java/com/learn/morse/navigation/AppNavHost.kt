package com.learn.morse.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.learn.morse.progress.LessonProgress
import com.learn.morse.ui.screen.HomeScreen
import com.learn.morse.ui.screen.LearnScreenRoute
import com.learn.morse.ui.screen.LessonsScreen
import com.learn.morse.ui.screen.SettingsScreen
import com.learn.morse.ui.screen.lessons

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier,
    ) {
        composable<Home> {
            HomeScreen(
                onLearnClick = { navController.navigate(Learn) },
                onSettingsClick = { navController.navigate(Settings) },
            )
        }
        composable<Learn> {
            LessonsScreen(
                onLessonClick = { id, resume -> navController.navigate(Lesson(id.toInt(), resume)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Settings> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<Lesson> { backStackEntry ->
            val lesson: Lesson = backStackEntry.toRoute()
            LearnScreenRoute(
                lessonId = lesson.id.toLong(),
                resume = lesson.resume,
                onBack = { navController.popBackStack() },
                onLessonComplete = {
                    LessonProgress.markCompleted(context, lesson.id.toLong())
                    // Go straight to the next lesson, or back to the lesson list after the last one.
                    val next = lessons.getOrNull(lessons.indexOfFirst { it.id == lesson.id.toLong() } + 1)
                    if (next != null) {
                        navController.navigate(Lesson(next.id.toInt())) { popUpTo<Learn>() }
                    } else {
                        navController.popBackStack<Learn>(inclusive = false)
                    }
                }
            )
        }
    }
}
