package com.learn.morse.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.learn.morse.analytics.Analytics
import com.learn.morse.monetization.Entitlements
import com.learn.morse.review.RatingPrompt
import com.learn.morse.ui.screen.ChartScreen
import com.learn.morse.ui.screen.CoursesScreen
import com.learn.morse.ui.screen.HomeScreen
import com.learn.morse.ui.screen.LearnScreenRoute
import com.learn.morse.ui.screen.LessonsScreen
import com.learn.morse.ui.screen.Onboarding
import com.learn.morse.ui.screen.OnboardingScreen
import com.learn.morse.ui.screen.PracticeScreenRoute
import com.learn.morse.ui.screen.PremiumScreen
import com.learn.morse.ui.screen.PrivacyPolicyScreen
import com.learn.morse.ui.screen.SettingsScreen
import com.learn.morse.ui.screen.TranslatorScreen
import com.learn.morse.ui.screen.UnlockLessonSheet
import com.learn.morse.ui.screen.courseOf
import com.learn.morse.ui.screen.lessons
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    // The locked lesson the user tried to open, while the unlock sheet is up.
    var lockedLessonId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Every way into a lesson goes through here, so a locked one offers the unlock sheet instead.
    fun openLesson(id: Long, resume: Boolean) {
        if (Entitlements.canOpen(context, id)) navController.navigate(Lesson(id.toInt(), resume))
        else lockedLessonId = id
    }

    // After leaving a lesson, so the review sheet never interrupts one.
    fun askForRating() {
        if (activity != null) scope.launch { RatingPrompt.maybeAsk(activity) }
    }

    lockedLessonId?.let { id ->
        UnlockLessonSheet(
            lesson = lessons.first { it.id == id },
            onUnlocked = {
                lockedLessonId = null
                navController.navigate(Lesson(id.toInt(), resume = true))
            },
            onDismiss = { lockedLessonId = null },
        )
    }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ -> Analytics.logScreen(destination) }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    // Read once: finishing onboarding replaces it with Home rather than changing the start.
    val startDestination: Any = remember { if (Onboarding.isDone(context)) Home else Welcome }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<Welcome> {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Home) { popUpTo<Welcome> { inclusive = true } }
                },
            )
        }
        composable<Home> {
            HomeScreen(
                onContinueLesson = { openLesson(it, resume = true) },
                onLearnClick = { navController.navigate(Learn) },
                onTranslatorClick = { navController.navigate(Translator) },
                onPracticeClick = { navController.navigate(Practice) },
                onChartClick = { navController.navigate(Chart) },
                onPremiumClick = { navController.navigate(Premium) },
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
                onLessonClick = { id, resume -> openLesson(id, resume) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Translator> {
            TranslatorScreen(onBack = { navController.popBackStack() })
        }
        composable<Settings> {
            SettingsScreen(
                onPrivacyPolicyClick = { navController.navigate(PrivacyPolicy) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Premium> {
            PremiumScreen(onBack = { navController.popBackStack() })
        }
        composable<Chart> {
            ChartScreen(onBack = { navController.popBackStack() })
        }
        composable<Practice> {
            PracticeScreenRoute(
                onBack = {
                    navController.popBackStack()
                    askForRating()
                },
            )
        }
        composable<PrivacyPolicy> {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable<Lesson> { backStackEntry ->
            val lesson: Lesson = backStackEntry.toRoute()
            LearnScreenRoute(
                lessonId = lesson.id.toLong(),
                resume = lesson.resume,
                onBack = {
                    navController.popBackStack()
                    askForRating()
                },
                onNextLesson = {
                    // Go to the next lesson of the course, replacing this one, or back to where the
                    // lesson was opened from (its lesson list or Home) after the last one. A locked
                    // next lesson goes back there too, and offers to unlock it.
                    val courseLessons = courseOf(lesson.id.toLong()).lessons
                    val next = courseLessons.getOrNull(courseLessons.indexOfFirst { it.id == lesson.id.toLong() } + 1)
                    if (next != null && Entitlements.canOpen(context, next.id)) {
                        navController.navigate(Lesson(next.id.toInt())) { popUpTo<Lesson> { inclusive = true } }
                    } else {
                        navController.popBackStack()
                        if (next != null) lockedLessonId = next.id
                    }
                    askForRating()
                }
            )
        }
    }
}
