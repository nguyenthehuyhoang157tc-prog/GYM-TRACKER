package com.hoctin.gymtracker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.ThemeMode
import com.hoctin.gymtracker.data.UserPreferencesRepository
import com.hoctin.gymtracker.data.WorkoutDraftRepository
import com.hoctin.gymtracker.ui.exercise.ExerciseDetailScreen
import com.hoctin.gymtracker.ui.exercise.ExerciseDetailViewModel
import com.hoctin.gymtracker.ui.history.HistoryScreen
import com.hoctin.gymtracker.ui.history.HistoryViewModel
import com.hoctin.gymtracker.ui.home.HomeScreen
import com.hoctin.gymtracker.ui.home.HomeViewModel
import com.hoctin.gymtracker.ui.log.LogWorkoutScreen
import com.hoctin.gymtracker.ui.log.LogWorkoutViewModel
import com.hoctin.gymtracker.ui.onboarding.OnboardingScreen
import com.hoctin.gymtracker.ui.onboarding.OnboardingViewModel
import com.hoctin.gymtracker.ui.plan.PlanScreen
import com.hoctin.gymtracker.ui.plan.PlanViewModel
import com.hoctin.gymtracker.ui.settings.SettingsScreen
import com.hoctin.gymtracker.ui.settings.SettingsViewModel
import com.hoctin.gymtracker.ui.theme.GymTrackerTheme

private object Routes {
    const val HOME = "home"
    const val PLAN = "plan"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val LOG = "log/{dayId}"
    const val EXERCISE = "exercise/{exerciseId}"

    fun log(dayId: Long) = "log/$dayId"
    fun exercise(exerciseId: Long) = "exercise/$exerciseId"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        route = Routes.HOME,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    TopLevelDestination(
        route = Routes.PLAN,
        label = "Plan",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
    ),
    TopLevelDestination(
        route = Routes.HISTORY,
        label = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
    ),
    TopLevelDestination(
        route = Routes.SETTINGS,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    ),
)

@Composable
fun GymTrackerApp(
    repository: GymRepository,
    preferencesRepository: UserPreferencesRepository,
    workoutDraftRepository: WorkoutDraftRepository,
) {
    val themeMode by preferencesRepository.themeMode.collectAsStateWithLifecycle()
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    GymTrackerTheme(darkTheme = darkTheme) {
        val onboardingCompleted by preferencesRepository.onboardingCompleted.collectAsStateWithLifecycle()
        if (!onboardingCompleted) {
            // First-launch onboarding gates Home until training weekdays are chosen.
            val onboardingViewModel: OnboardingViewModel = viewModel(
                factory = AppViewModelFactory {
                    OnboardingViewModel(
                        repository = repository,
                        preferencesRepository = preferencesRepository,
                    )
                },
            )
            OnboardingScreen(viewModel = onboardingViewModel)
            return@GymTrackerTheme
        }

        val navController = rememberNavController()
        val backStackEntry = navController.currentBackStackEntryAsState().value
        val currentDestination = backStackEntry?.destination

        Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                        ) {
                            topLevelDestinations.forEach { destination ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == destination.route
                                } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigateTopLevel(destination.route)
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                            contentDescription = destination.label,
                                        )
                                    },
                                    label = { Text(destination.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(Routes.HOME) {
                            val homeViewModel: HomeViewModel = viewModel(
                                factory = AppViewModelFactory {
                                    HomeViewModel(
                                        repository = repository,
                                        preferencesRepository = preferencesRepository,
                                    )
                                },
                            )
                            HomeScreen(
                                viewModel = homeViewModel,
                                onStartWorkout = { dayId -> navController.navigate(Routes.log(dayId)) },
                                onOpenExercise = { exerciseId -> navController.navigate(Routes.exercise(exerciseId)) },
                            )
                        }

                        composable(Routes.PLAN) {
                            val planViewModel: PlanViewModel = viewModel(
                                factory = AppViewModelFactory { PlanViewModel(repository) },
                            )
                            PlanScreen(
                                viewModel = planViewModel,
                                onOpenExercise = { exerciseId -> navController.navigate(Routes.exercise(exerciseId)) },
                            )
                        }

                        composable(Routes.HISTORY) {
                            val historyViewModel: HistoryViewModel = viewModel(
                                factory = AppViewModelFactory { HistoryViewModel(repository) },
                            )
                            HistoryScreen(viewModel = historyViewModel)
                        }

                        composable(Routes.SETTINGS) {
                            val settingsViewModel: SettingsViewModel = viewModel(
                                factory = AppViewModelFactory {
                                    SettingsViewModel(
                                        repository = repository,
                                        preferencesRepository = preferencesRepository,
                                    )
                                },
                            )
                            SettingsScreen(viewModel = settingsViewModel)
                        }

                        composable(
                            route = Routes.LOG,
                            arguments = listOf(navArgument("dayId") { type = NavType.LongType }),
                        ) { entry ->
                            val dayId = entry.arguments?.getLong("dayId") ?: return@composable
                            val logViewModel: LogWorkoutViewModel = viewModel(
                                key = "log-$dayId",
                                factory = AppViewModelFactory {
                                    LogWorkoutViewModel(
                                        dayId = dayId,
                                        repository = repository,
                                        draftRepository = workoutDraftRepository,
                                        preferencesRepository = preferencesRepository,
                                    )
                                },
                            )
                            LogWorkoutScreen(
                                viewModel = logViewModel,
                                onSaved = {
                                    navController.navigate(Routes.HISTORY) {
                                        popUpTo(Routes.HOME) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                },
                                onDiscarded = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.HOME) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }

                        composable(
                            route = Routes.EXERCISE,
                            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType }),
                        ) { entry ->
                            val exerciseId = entry.arguments?.getLong("exerciseId") ?: return@composable
                            val detailViewModel: ExerciseDetailViewModel = viewModel(
                                key = "exercise-$exerciseId",
                                factory = AppViewModelFactory { ExerciseDetailViewModel(exerciseId, repository) },
                            )
                            ExerciseDetailScreen(viewModel = detailViewModel)
                        }
                    }
                }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    if (currentDestination?.route == route) return

    if (route == Routes.HOME) {
        if (!popBackStack(Routes.HOME, inclusive = false)) {
            navigate(Routes.HOME) {
                launchSingleTop = true
            }
        }
        return
    }

    navigate(route) {
        popUpTo(Routes.HOME) {
            inclusive = false
        }
        launchSingleTop = true
    }
}
