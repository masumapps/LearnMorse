package com.learn.morse.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.learn.morse.ui.screen.CoursesScreen
import com.learn.morse.ui.screen.HomeScreen
import com.learn.morse.ui.screen.LearnScreenRoute
import com.learn.morse.ui.screen.LessonsScreen
import com.learn.morse.ui.screen.SettingsScreen
import com.learn.morse.ui.screen.TranslatorScreen
import com.learn.morse.ui.screen.courseOf

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier,
    ) {
        composable<Home> {
            HomeScreen(
                onLearnClick = { navController.navigate(Learn) },
                onTranslatorClick = { navController.navigate(Translator) },
                onSettingsClick = { navController.navigate(Settings) },
            )
        }
        composable<Learn> {
            CoursesScreen(
                onCourseClick = { navController.navigate(CourseLessons(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<CourseLessons> { backStackEntry ->
            val route: CourseLessons = backStackEntry.toRoute()
            LessonsScreen(
                courseId = route.courseId,
                onLessonClick = { id, resume -> navController.navigate(Lesson(id.toInt(), resume)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Translator> {
            TranslatorScreen(onBack = { navController.popBackStack() })
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
                onNextLesson = {
                    // Go to the next lesson of the course, or back to its lesson list after the last one.
                    val courseLessons = courseOf(lesson.id.toLong()).lessons
                    val next = courseLessons.getOrNull(courseLessons.indexOfFirst { it.id == lesson.id.toLong() } + 1)
                    if (next != null) {
                        navController.navigate(Lesson(next.id.toInt())) { popUpTo<CourseLessons>() }
                    } else {
                        navController.popBackStack<CourseLessons>(inclusive = false)
                    }
                }
            )
        }
    }
}
