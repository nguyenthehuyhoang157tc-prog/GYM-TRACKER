package com.hoctin.gymtracker.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.SavedWorkoutRepository
import com.hoctin.gymtracker.data.ThemeMode
import com.hoctin.gymtracker.data.UserPreferencesRepository
import com.hoctin.gymtracker.data.WorkoutDraftRepository
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.research.RecoveryResearchRepository
import com.hoctin.gymtracker.data.wearable.WearableShadowRepository
import com.hoctin.gymtracker.ui.catalog.CatalogPickerScreen
import com.hoctin.gymtracker.ui.catalog.CatalogPickerViewModel
import com.hoctin.gymtracker.ui.detail.WorkoutDetailScreen
import com.hoctin.gymtracker.ui.detail.WorkoutDetailViewModel
import com.hoctin.gymtracker.ui.exercise.ExerciseDetailScreen
import com.hoctin.gymtracker.ui.exercise.ExerciseDetailViewModel
import com.hoctin.gymtracker.ui.history.HistoryScreen
import com.hoctin.gymtracker.ui.history.HistoryViewModel
import com.hoctin.gymtracker.ui.home.ConsistencyPresentationStore
import com.hoctin.gymtracker.ui.home.ConsistencyScreen
import com.hoctin.gymtracker.ui.home.HomeScreen
import com.hoctin.gymtracker.ui.home.HomeViewModel
import com.hoctin.gymtracker.ui.log.LogWorkoutScreen
import com.hoctin.gymtracker.ui.log.LogWorkoutLaunch
import com.hoctin.gymtracker.ui.log.LogWorkoutViewModel
import com.hoctin.gymtracker.ui.onboarding.OnboardingExit
import com.hoctin.gymtracker.ui.onboarding.OnboardingScreen
import com.hoctin.gymtracker.ui.onboarding.OnboardingViewModel
import com.hoctin.gymtracker.ui.plan.PlanScreen
import com.hoctin.gymtracker.ui.plan.PlanViewModel
import com.hoctin.gymtracker.ui.plan.GenerateWorkoutUnavailableScreen
import com.hoctin.gymtracker.ui.plan.SavedWorkoutBuilderScreen
import com.hoctin.gymtracker.ui.plan.SavedWorkoutBuilderViewModel
import com.hoctin.gymtracker.ui.plan.SavedWorkoutsScreen
import com.hoctin.gymtracker.ui.plan.SavedWorkoutsViewModel
import com.hoctin.gymtracker.ui.review.SessionReviewScreen
import com.hoctin.gymtracker.ui.review.SessionReviewViewModel
import com.hoctin.gymtracker.ui.settings.AboutAuronScreen
import com.hoctin.gymtracker.ui.settings.NotificationsPlaceholderScreen
import com.hoctin.gymtracker.ui.settings.PersonalProfileScreen
import com.hoctin.gymtracker.ui.settings.ProfileDataScreen
import com.hoctin.gymtracker.ui.settings.ProfileDestination
import com.hoctin.gymtracker.ui.settings.ProfileHubScreen
import com.hoctin.gymtracker.ui.settings.ProfilePreferencesScreen
import com.hoctin.gymtracker.ui.settings.ProfilePrivacyScreen
import com.hoctin.gymtracker.ui.settings.ProfileResourcesScreen
import com.hoctin.gymtracker.ui.settings.ProfileTrainingScreen
import com.hoctin.gymtracker.ui.settings.RecoveryResearchScreen
import com.hoctin.gymtracker.ui.settings.SettingsViewModel
import com.hoctin.gymtracker.ui.settings.WorkoutInsightsScreen
import com.hoctin.gymtracker.ui.components.AuronGlass
import com.hoctin.gymtracker.ui.components.NavDockBackdropLens
import com.hoctin.gymtracker.ui.components.NavDockGlassLighting
import com.hoctin.gymtracker.ui.components.NavDockLens
import com.hoctin.gymtracker.ui.components.auronBackdropLens
import com.hoctin.gymtracker.ui.components.auronLiquidGlass
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.theme.GymTrackerTheme
import com.hoctin.gymtracker.ui.theme.isAuronDarkTheme
import kotlinx.coroutines.launch

/**
 * Bottom content padding scrollable top-level screens add so their last item can scroll clear of the
 * floating dock overlay (the dock is ~64dp tall + ~18dp margins above the system inset). The system
 * navigation-bar inset itself is already applied to content via the Scaffold.
 */
val BottomNavSpace = 96.dp

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CONSISTENCY = "consistency"
    const val PLAN = "plan"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PROFILE_PERSONAL = "profile/personal"
    const val PROFILE_INSIGHTS = "profile/insights"
    const val PROFILE_PREFERENCES = "profile/preferences"
    const val PROFILE_TRAINING = "profile/training"
    const val PROFILE_DATA = "profile/data"
    const val PROFILE_RESEARCH = "profile/research"
    const val PROFILE_RESOURCES = "profile/resources"
    const val PROFILE_NOTIFICATIONS = "profile/notifications"
    const val PROFILE_PRIVACY = "profile/privacy"
    const val PROFILE_ABOUT = "profile/about"
    const val DETAIL = "detail/{dayId}"
    const val LOG = "log/{dayId}"
    const val LOG_SAVED = "log/saved/{savedWorkoutId}"
    const val SAVED_WORKOUTS = "plan/saved-workouts"
    const val GENERATE_WORKOUT = "plan/generate-workout"
    const val SAVED_WORKOUT_BUILDER = "plan/saved-workouts/builder/{savedWorkoutId}"
    const val EXERCISE = "exercise/{exerciseId}"
    const val CATALOG = "catalog/{dayId}"
    // MUSCLE-3: after-save / saved-session Muscle Heat Review (durationMillis 0 = unknown → hidden).
    const val SESSION_REVIEW = "sessionReview/{sessionId}/{durationMillis}"

    fun detail(dayId: Long) = "detail/$dayId"
    fun log(dayId: Long) = "log/$dayId"
    fun logSaved(savedWorkoutId: Long) = "log/saved/$savedWorkoutId"
    fun savedWorkoutBuilder(savedWorkoutId: Long = -1L) = "plan/saved-workouts/builder/$savedWorkoutId"
    fun exercise(exerciseId: Long) = "exercise/$exerciseId"
    fun catalog(dayId: Long) = "catalog/$dayId"
    fun sessionReview(sessionId: Long, durationMillis: Long = 0L) = "sessionReview/$sessionId/$durationMillis"
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
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
    ),
)

/**
 * Routes on which the floating dock is visible. Derived from [topLevelDestinations] so the two
 * lists cannot drift apart.
 *
 * Visibility must be an **exact** route match. It previously used `NavDestination.hierarchy`, which
 * walks the destination *and all its parents* — so any pushed route nested under a top-level
 * destination matched its parent and the dock stayed visible on screens (Workout Detail, Log
 * Workout) that are meant to own their own bottom bar.
 */
private val dockVisibleRoutes: Set<String> = topLevelDestinations.map { it.route }.toSet()

@Composable
fun GymTrackerApp(
    repository: GymRepository,
    savedWorkoutRepository: SavedWorkoutRepository,
    preferencesRepository: UserPreferencesRepository,
    workoutDraftRepository: WorkoutDraftRepository,
    catalogRepository: ExerciseCatalogRepository,
    recoveryResearchRepository: RecoveryResearchRepository,
    wearableShadowRepository: WearableShadowRepository,
) {
    val appContext = LocalContext.current.applicationContext
    val consistencyPresentationStore = remember(appContext) {
        ConsistencyPresentationStore(appContext)
    }
    val themeMode by preferencesRepository.themeMode.collectAsStateWithLifecycle()
    val distanceUnitSystem by preferencesRepository.distanceUnitSystem.collectAsStateWithLifecycle()
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    GymTrackerTheme(darkTheme = darkTheme) {
        ConfigureSystemBars(darkTheme = darkTheme)
        val onboardingCompleted by preferencesRepository.onboardingCompleted.collectAsStateWithLifecycle()
        val onboardingReceiptPending by preferencesRepository.onboardingReceiptPending.collectAsStateWithLifecycle()
        val initialRoute = remember {
            if (!onboardingCompleted || onboardingReceiptPending) Routes.ONBOARDING else Routes.HOME
        }

        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val snackbarScope = rememberCoroutineScope()
        val backStackEntry = navController.currentBackStackEntryAsState().value
        val currentDestination = backStackEntry?.destination
        val onboardingRouteVisible = currentDestination?.route?.let { route ->
            route == Routes.ONBOARDING
        } ?: (initialRoute == Routes.ONBOARDING)

        // LOG-V4B: the dock reports its own rect so the backdrop lens knows where to refract.
        // These are written from onGloballyPositioned (once after layout settles) and read by the
        // lens modifier on the background layer. The dock is anchored to the screen, so this only
        // changes on layout/config change — never while scrolling.
        // Hoisted so the lens gate can see it. Identical to the dock's own gate, but the
        // lens must use the hoisted value (the inner one is in a nested scope).
        val currentRoute = currentDestination?.route
        val showDock = currentRoute != null && currentRoute in dockVisibleRoutes
        val backdropLensActive = AuronGlass.BACKDROP_AVAILABLE && showDock
        var dockCenterX by remember { mutableFloatStateOf(0f) }
        var dockCenterY by remember { mutableFloatStateOf(0f) }
        var dockLensHalfWidth by remember { mutableFloatStateOf(0f) }
        var dockLensHalfHeight by remember { mutableFloatStateOf(0f) }

        // One-line summary log so the QA report can be triaged at a glance. Only when DEBUG_TINT
        // is on. The "measured" flag flips the moment the dock has a real rect, so the message
        // fires exactly once: when the shader should start producing output.
        var lensMeasuredLogged by remember { mutableStateOf(false) }
        LaunchedEffect(dockLensHalfWidth, dockLensHalfHeight) {
            if (AuronGlass.DEBUG_TINT && !lensMeasuredLogged &&
                dockLensHalfWidth > 0f && dockLensHalfHeight > 0f
            ) {
                lensMeasuredLogged = true
                android.util.Log.d(
                    "AuronGlass",
                    "lens measured: dockCenter=($dockCenterX, $dockCenterY) " +
                        "dockHalf=($dockLensHalfWidth, $dockLensHalfHeight) — " +
                        "shader should now show debug tint if DEBUG_TINT is on",
                )
            }
        }
        // The lens shader's `coord` is in the background Box's OWN coordinate space, not root
        // space. Measure that Box's origin too and subtract, so the refraction lands on the dock
        // even if the Scaffold ever offsets its content area.
        var lensOriginX by remember { mutableFloatStateOf(0f) }
        var lensOriginY by remember { mutableFloatStateOf(0f) }
        val dockLensCenterX = dockCenterX - lensOriginX
        val dockLensCenterY = dockCenterY - lensOriginY

        // No bottomBar slot: the floating dock is an overlay (see below) so it never reserves a
        // rectangle that blocks content. Scaffold still provides the system-bar insets.
        Scaffold(
            // Android 15 draws the gesture-navigation area through the Scaffold container. Keep
            // that inset black while the onboarding-local dark canvas is visible, even when the
            // user's app theme is Light. Other routes retain the existing theme background.
            containerColor = if (onboardingRouteVisible) Color.Black else MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.navigationBars,
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                // LOG-V4B backdrop refraction. The lens runs on this Box — the layer that CONTAINS
                // the scrolling content — so the shader's `content` uniform receives real Home /
                // Plan / History pixels. Putting it on the dock instead would only ever bend the
                // dock's own fill, which is why every earlier attempt showed nothing.
                //
                // The dock is a sibling drawn afterwards, so its icons and labels are never fed
                // through the shader and stay crisp. The lens rect is measured from the dock and
                // is constant in screen space, so these uniforms update on layout change only.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            val origin = coordinates.positionInRoot()
                            lensOriginX = origin.x
                            lensOriginY = origin.y
                            if (AuronGlass.DEBUG_TINT) {
                                android.util.Log.d(
                                    "AuronGlass",
                                    "lens Box positionInRoot: origin=(${origin.x}, ${origin.y}) " +
                                        "size=${coordinates.size.width}x${coordinates.size.height}",
                                )
                            }
                        }
                        // Attached ONLY while the dock is on screen. The gate and the dock's own gate
                        // both read the same hoisted `showDock`, so they cannot disagree.
                        .then(
                            if (backdropLensActive) {
                                Modifier
                                    .auronBackdropLens(
                                        centerX = dockLensCenterX,
                                        centerY = dockLensCenterY,
                                        halfWidth = dockLensHalfWidth,
                                        halfHeight = dockLensHalfHeight,
                                        lens = NavDockBackdropLens,
                                    )
                            } else {
                                Modifier
                            }
                        ),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = initialRoute,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        composable(Routes.ONBOARDING) {
                            val onboardingViewModel: OnboardingViewModel = viewModel(
                                factory = AppViewModelFactory {
                                    OnboardingViewModel(
                                        repository = repository,
                                        preferencesRepository = preferencesRepository,
                                    )
                                },
                            )
                            OnboardingScreen(
                                viewModel = onboardingViewModel,
                                onExit = { destination ->
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    if (destination is OnboardingExit.ReviewDay1) {
                                        navController.navigate(Routes.detail(destination.dayId))
                                    }
                                },
                            )
                        }

                        composable(Routes.HOME) {
                            val homeViewModel: HomeViewModel = viewModel(
                                factory = AppViewModelFactory {
                                    HomeViewModel(
                                        repository = repository,
                                        preferencesRepository = preferencesRepository,
                                        catalogRepository = catalogRepository,
                                        consistencyPresentationStore = consistencyPresentationStore,
                                        wearableShadowRepository = wearableShadowRepository,
                                    )
                                },
                            )
                            HomeScreen(
                                viewModel = homeViewModel,
                                onViewDetail = { dayId -> navController.navigate(Routes.detail(dayId)) },
                                onViewConsistency = {
                                    navController.navigate(Routes.CONSISTENCY)
                                },
                            )
                        }

                        composable(Routes.CONSISTENCY) { entry ->
                            val homeEntry = remember(entry) {
                                navController.getBackStackEntry(Routes.HOME)
                            }
                            val homeViewModel: HomeViewModel = viewModel(
                                viewModelStoreOwner = homeEntry,
                                factory = AppViewModelFactory {
                                    HomeViewModel(
                                        repository = repository,
                                        preferencesRepository = preferencesRepository,
                                        catalogRepository = catalogRepository,
                                        consistencyPresentationStore = consistencyPresentationStore,
                                        wearableShadowRepository = wearableShadowRepository,
                                    )
                                },
                            )
                            ConsistencyScreen(
                                viewModel = homeViewModel,
                                onBack = { navController.popBackStack() },
                                onOpenSession = { sessionId ->
                                    navController.navigate(Routes.sessionReview(sessionId))
                                },
                            )
                        }

                        composable(
                            route = Routes.DETAIL,
                            arguments = listOf(navArgument("dayId") { type = NavType.LongType }),
                        ) { entry ->
                            val dayId = entry.arguments?.getLong("dayId") ?: return@composable
                            val detailViewModel: WorkoutDetailViewModel = viewModel(
                                key = "detail-$dayId",
                                factory = AppViewModelFactory {
                                    WorkoutDetailViewModel(dayId, repository, preferencesRepository)
                                },
                            )
                            WorkoutDetailScreen(
                                viewModel = detailViewModel,
                                onStartWorkout = { id -> navController.navigate(Routes.log(id)) },
                                onOpenExercise = { id -> navController.navigate(Routes.exercise(id)) },
                                onOpenCatalog = { id -> navController.navigate(Routes.catalog(id)) },
                                onOpenYourWorkouts = { navController.navigate(Routes.SAVED_WORKOUTS) },
                                onRoutineChanged = { nextId ->
                                    navController.navigate(Routes.detail(nextId)) {
                                        popUpTo(Routes.DETAIL) { inclusive = true }
                                    }
                                },
                                catalogRepository = catalogRepository,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(Routes.PLAN) {
                            val planViewModel: PlanViewModel = viewModel(
                                factory = AppViewModelFactory { PlanViewModel(repository) },
                            )
                            PlanScreen(
                                viewModel = planViewModel,
                                onOpenRoutine = { dayId -> navController.navigate(Routes.detail(dayId)) },
                                onOpenYourWorkouts = { navController.navigate(Routes.SAVED_WORKOUTS) },
                                onOpenGenerateWorkout = { navController.navigate(Routes.GENERATE_WORKOUT) },
                                catalogRepository = catalogRepository,
                            )
                        }

                        composable(Routes.GENERATE_WORKOUT) {
                            GenerateWorkoutUnavailableScreen(onBack = { navController.popBackStack() })
                        }

                        composable(Routes.SAVED_WORKOUTS) {
                            val savedViewModel: SavedWorkoutsViewModel = viewModel(
                                factory = AppViewModelFactory {
                                    SavedWorkoutsViewModel(savedWorkoutRepository)
                                },
                            )
                            SavedWorkoutsScreen(
                                viewModel = savedViewModel,
                                catalogRepository = catalogRepository,
                                onBack = { navController.popBackStack() },
                                onCreate = { navController.navigate(Routes.savedWorkoutBuilder()) },
                                onEdit = { id -> navController.navigate(Routes.savedWorkoutBuilder(id)) },
                                onStart = { id -> navController.navigate(Routes.logSaved(id)) },
                            )
                        }

                        composable(
                            route = Routes.SAVED_WORKOUT_BUILDER,
                            arguments = listOf(navArgument("savedWorkoutId") { type = NavType.LongType }),
                        ) { entry ->
                            val rawId = entry.arguments?.getLong("savedWorkoutId") ?: -1L
                            val editId = rawId.takeIf { it >= 0L }
                            val builderViewModel: SavedWorkoutBuilderViewModel = viewModel(
                                key = "saved-workout-builder-$rawId",
                                factory = AppViewModelFactory {
                                    SavedWorkoutBuilderViewModel(
                                        editId = editId,
                                        repository = savedWorkoutRepository,
                                        catalogRepository = catalogRepository,
                                    )
                                },
                            )
                            SavedWorkoutBuilderScreen(
                                viewModel = builderViewModel,
                                onBack = { navController.popBackStack() },
                                onSaved = {
                                    navController.popBackStack(Routes.SAVED_WORKOUTS, inclusive = false)
                                },
                            )
                        }

                        composable(
                            route = Routes.CATALOG,
                            arguments = listOf(navArgument("dayId") { type = NavType.LongType }),
                        ) { entry ->
                            val dayId = entry.arguments?.getLong("dayId") ?: return@composable
                            val catalogViewModel: CatalogPickerViewModel = viewModel(
                                key = "catalog-$dayId",
                                factory = AppViewModelFactory {
                                    CatalogPickerViewModel(
                                        dayId = dayId,
                                        catalogRepository = catalogRepository,
                                        gymRepository = repository,
                                    )
                                },
                            )
                            CatalogPickerScreen(
                                viewModel = catalogViewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(Routes.HISTORY) {
                            val historyViewModel: HistoryViewModel = viewModel(
                                factory = AppViewModelFactory { HistoryViewModel(repository) },
                            )
                            HistoryScreen(
                                viewModel = historyViewModel,
                                catalogRepository = catalogRepository,
                                distanceUnitSystem = distanceUnitSystem,
                                onOpenSession = { sessionId ->
                                    navController.navigate(Routes.sessionReview(sessionId))
                                },
                            )
                        }

                        composable(Routes.SETTINGS) {
                            val settingsViewModel: SettingsViewModel = viewModel(
                                factory = AppViewModelFactory {
                                    SettingsViewModel(
                                        repository = repository,
                                        preferencesRepository = preferencesRepository,
                                        catalogRepository = catalogRepository,
                                        recoveryResearchRepository = recoveryResearchRepository,
                                        wearableShadowRepository = wearableShadowRepository,
                                    )
                                },
                            )
                            val settingsState = settingsViewModel.uiState.collectAsStateWithLifecycle().value
                            val displayName = remember(settingsState.trainingProfileJson) {
                                com.hoctin.gymtracker.ui.onboarding.OnboardingProfileJson
                                    .decodeDraft(settingsState.trainingProfileJson)
                                    ?.displayName
                            }
                            ProfileHubScreen(
                                displayName = displayName,
                                onOpen = { destination ->
                                    when (destination) {
                                        ProfileDestination.CONSISTENCY -> navController.navigate(Routes.CONSISTENCY)
                                        ProfileDestination.INSIGHTS -> navController.navigate(Routes.PROFILE_INSIGHTS)
                                        ProfileDestination.PLAN -> navController.navigateTopLevel(Routes.PLAN)
                                        ProfileDestination.TRAINING -> navController.navigate(Routes.PROFILE_TRAINING)
                                        ProfileDestination.PREFERENCES -> navController.navigate(Routes.PROFILE_PREFERENCES)
                                        ProfileDestination.NOTIFICATIONS -> navController.navigate(Routes.PROFILE_NOTIFICATIONS)
                                        ProfileDestination.PERSONAL_PROFILE -> navController.navigate(Routes.PROFILE_PERSONAL)
                                        ProfileDestination.HISTORY -> navController.navigateTopLevel(Routes.HISTORY)
                                        ProfileDestination.DATA -> navController.navigate(Routes.PROFILE_DATA)
                                        ProfileDestination.RECOVERY_RESEARCH -> navController.navigate(Routes.PROFILE_RESEARCH)
                                        ProfileDestination.RESOURCES -> navController.navigate(Routes.PROFILE_RESOURCES)
                                        ProfileDestination.PRIVACY -> navController.navigate(Routes.PROFILE_PRIVACY)
                                        ProfileDestination.ABOUT -> navController.navigate(Routes.PROFILE_ABOUT)
                                    }
                                },
                            )
                        }

                        composable(Routes.PROFILE_PERSONAL) { entry ->
                            val settingsViewModel = profileSettingsViewModel(
                                entry = entry,
                                navController = navController,
                                repository = repository,
                                preferencesRepository = preferencesRepository,
                                catalogRepository = catalogRepository,
                                recoveryResearchRepository = recoveryResearchRepository,
                                wearableShadowRepository = wearableShadowRepository,
                            )
                            PersonalProfileScreen(
                                state = settingsViewModel.uiState.collectAsStateWithLifecycle().value,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(Routes.PROFILE_INSIGHTS) {
                            val historyViewModel: HistoryViewModel = viewModel(
                                key = "profile-insights",
                                factory = AppViewModelFactory { HistoryViewModel(repository) },
                            )
                            WorkoutInsightsScreen(
                                historyState = historyViewModel.uiState.collectAsStateWithLifecycle().value,
                                onBack = { navController.popBackStack() },
                                onOpenHistory = { navController.navigateTopLevel(Routes.HISTORY) },
                            )
                        }

                        composable(Routes.PROFILE_PREFERENCES) { entry ->
                            val settingsViewModel = profileSettingsViewModel(
                                entry, navController, repository, preferencesRepository,
                                catalogRepository, recoveryResearchRepository,
                                wearableShadowRepository,
                            )
                            val state = settingsViewModel.uiState.collectAsStateWithLifecycle().value
                            ProfilePreferencesScreen(
                                state = state,
                                onThemeChanged = settingsViewModel::setThemeMode,
                                onDistanceChanged = settingsViewModel::setDistanceUnitSystem,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(Routes.PROFILE_TRAINING) { entry ->
                            val settingsViewModel = profileSettingsViewModel(
                                entry, navController, repository, preferencesRepository,
                                catalogRepository, recoveryResearchRepository,
                                wearableShadowRepository,
                            )
                            ProfileTrainingScreen(
                                state = settingsViewModel.uiState.collectAsStateWithLifecycle().value,
                                onSaveWeekdays = settingsViewModel::setTrainingWeekdays,
                                onSaveGoal = settingsViewModel::setWorkoutGoal,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(Routes.PROFILE_DATA) { entry ->
                            val settingsViewModel = profileSettingsViewModel(
                                entry, navController, repository, preferencesRepository,
                                catalogRepository, recoveryResearchRepository,
                                wearableShadowRepository,
                            )
                            ProfileDataScreen(
                                state = settingsViewModel.uiState.collectAsStateWithLifecycle().value,
                                onUpdateImportText = settingsViewModel::updateImportText,
                                onGenerateExport = settingsViewModel::exportData,
                                onImport = settingsViewModel::importData,
                                onMessage = settingsViewModel::setMessage,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(Routes.PROFILE_RESEARCH) { entry ->
                            val settingsViewModel = profileSettingsViewModel(
                                entry, navController, repository, preferencesRepository,
                                catalogRepository, recoveryResearchRepository,
                                wearableShadowRepository,
                            )
                            RecoveryResearchScreen(
                                state = settingsViewModel.uiState.collectAsStateWithLifecycle().value,
                                onEnabledChanged = settingsViewModel::setRecoveryResearchEnabled,
                                onPrepareExport = settingsViewModel::prepareRecoveryResearchExport,
                                onDelete = settingsViewModel::deleteRecoveryResearchData,
                                onHealthPermissionResult = settingsViewModel::onHealthPermissionResult,
                                onRefreshHealthContext = settingsViewModel::refreshHealthContext,
                                onDisconnectHealthConnect = settingsViewModel::disconnectHealthConnect,
                                onDeleteImportedHealthData = settingsViewModel::deleteImportedHealthData,
                                onManageHealthAccess = settingsViewModel::healthConnectManageAccessIntent,
                                onMessage = settingsViewModel::setMessage,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(Routes.PROFILE_RESOURCES) {
                            ProfileResourcesScreen(onBack = { navController.popBackStack() })
                        }

                        composable(Routes.PROFILE_NOTIFICATIONS) {
                            NotificationsPlaceholderScreen(onBack = { navController.popBackStack() })
                        }

                        composable(Routes.PROFILE_PRIVACY) {
                            ProfilePrivacyScreen(onBack = { navController.popBackStack() })
                        }

                        composable(Routes.PROFILE_ABOUT) {
                            AboutAuronScreen(onBack = { navController.popBackStack() })
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
                                        launch = LogWorkoutLaunch.Routine(dayId),
                                        repository = repository,
                                        savedWorkoutRepository = savedWorkoutRepository,
                                        draftRepository = workoutDraftRepository,
                                        preferencesRepository = preferencesRepository,
                                        catalogRepository = catalogRepository,
                                        recoveryResearchRepository = recoveryResearchRepository,
                                        wearableShadowRepository = wearableShadowRepository,
                                    )
                                },
                            )
                            LogWorkoutScreen(
                                viewModel = logViewModel,
                                catalogRepository = catalogRepository,
                                distanceUnitSystem = distanceUnitSystem,
                                onSaved = { sessionId, durationMillis ->
                                    snackbarScope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Workout saved. Great job today - get some rest!",
                                        )
                                    }
                                    // Existing behavior preserved: History stays underneath, the
                                    // MUSCLE-3 review opens on top (back returns to History).
                                    navController.navigate(Routes.HISTORY) {
                                        popUpTo(Routes.HOME) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                    navController.navigate(Routes.sessionReview(sessionId, durationMillis))
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
                            route = Routes.LOG_SAVED,
                            arguments = listOf(navArgument("savedWorkoutId") { type = NavType.LongType }),
                        ) { entry ->
                            val savedWorkoutId = entry.arguments?.getLong("savedWorkoutId") ?: return@composable
                            val logViewModel: LogWorkoutViewModel = viewModel(
                                key = "log-saved-$savedWorkoutId",
                                factory = AppViewModelFactory {
                                    LogWorkoutViewModel(
                                        launch = LogWorkoutLaunch.SavedOneOff(savedWorkoutId),
                                        repository = repository,
                                        savedWorkoutRepository = savedWorkoutRepository,
                                        draftRepository = workoutDraftRepository,
                                        preferencesRepository = preferencesRepository,
                                        catalogRepository = catalogRepository,
                                        recoveryResearchRepository = recoveryResearchRepository,
                                        wearableShadowRepository = wearableShadowRepository,
                                    )
                                },
                            )
                            LogWorkoutScreen(
                                viewModel = logViewModel,
                                catalogRepository = catalogRepository,
                                distanceUnitSystem = distanceUnitSystem,
                                onSaved = { sessionId, durationMillis ->
                                    snackbarScope.launch {
                                        snackbarHostState.showSnackbar("One-off workout saved to History.")
                                    }
                                    navController.navigate(Routes.HISTORY) {
                                        popUpTo(Routes.PLAN) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    navController.navigate(Routes.sessionReview(sessionId, durationMillis))
                                },
                                onDiscarded = { navController.popBackStack() },
                            )
                        }

                        composable(
                            route = Routes.SESSION_REVIEW,
                            arguments = listOf(
                                navArgument("sessionId") { type = NavType.LongType },
                                navArgument("durationMillis") { type = NavType.LongType },
                            ),
                        ) { entry ->
                            val sessionId = entry.arguments?.getLong("sessionId") ?: return@composable
                            val durationMillis = entry.arguments?.getLong("durationMillis") ?: 0L
                            val reviewViewModel: SessionReviewViewModel = viewModel(
                                key = "sessionReview-$sessionId",
                                factory = AppViewModelFactory {
                                    SessionReviewViewModel(
                                        sessionId = sessionId,
                                        repository = repository,
                                        catalogRepository = catalogRepository,
                                    )
                                },
                            )
                            SessionReviewScreen(
                                viewModel = reviewViewModel,
                                durationMillis = durationMillis,
                                onBack = { navController.popBackStack() },
                                catalogRepository = catalogRepository,
                                distanceUnitSystem = distanceUnitSystem,
                            )
                        }

                        composable(
                            route = Routes.EXERCISE,
                            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType }),
                        ) { entry ->
                            val exerciseId = entry.arguments?.getLong("exerciseId") ?: return@composable
                            val detailViewModel: ExerciseDetailViewModel = viewModel(
                                key = "exercise-$exerciseId",
                                factory = AppViewModelFactory {
                                    ExerciseDetailViewModel(
                                        exerciseId,
                                        repository,
                                        catalogRepository,
                                        preferencesRepository,
                                    )
                                },
                            )
                            ExerciseDetailScreen(
                                viewModel = detailViewModel,
                                distanceUnitSystem = distanceUnitSystem,
                            )
                        }
                    }
                }
                    // Floating dock overlay (top-level routes only): never reserves a rectangle, so
                    // content scrolls under it. Hidden on pushed screens (log/exercise) which keep
                    // their own bottom bars.
                    //
                    // Exact-route allowlist. Do NOT switch this back to `hierarchy`: that matches
                    // the parent destination too, so pushed screens inherited their parent's
                    // visibility and the dock leaked onto detail/log screens. (Selection state may
                    // still use `hierarchy` — see `GymBottomBar` — since it only runs when the dock
                    // is already visible on an exact top-level route.)
                    // currentRoute + showDock are hoisted above (the Box scope can't redefine
                    // them or the lens gate would no longer see the value).
                    if (AuronGlass.DEBUG_NAV) {
                        android.util.Log.d(
                            "AURON_Nav",
                            "route=$currentRoute showDock=$showDock " +
                                "lens=$backdropLensActive stackTop=" +
                                "${navController.currentBackStackEntry?.destination?.route}",
                        )
                    }
                    if (showDock) {
                        GymBottomBar(
                            destinations = topLevelDestinations,
                            currentDestination = currentDestination,
                            onSelect = { route -> navController.navigateTopLevel(route) },
                            glassModifier = Modifier.auronLiquidGlass(
                                if (backdropLensActive) NavDockGlassLighting else NavDockLens,
                            ),
                            backdropLensActive = backdropLensActive,
                            onLensMeasured = { cx, cy, hw, hh ->
                                dockCenterX = cx
                                dockCenterY = cy
                                dockLensHalfWidth = hw
                                dockLensHalfHeight = hh
                                if (AuronGlass.DEBUG_TINT) {
                                    android.util.Log.d(
                                        "AuronGlass",
                                        "dock measured: center=($cx,$cy) halfSize=($hw,$hh)",
                                    )
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 20.dp, end = 20.dp, bottom = BottomNavSpace),
                    )
                }
            }
    }
}

@Composable
private fun profileSettingsViewModel(
    entry: NavBackStackEntry,
    navController: NavHostController,
    repository: GymRepository,
    preferencesRepository: UserPreferencesRepository,
    catalogRepository: ExerciseCatalogRepository,
    recoveryResearchRepository: RecoveryResearchRepository,
    wearableShadowRepository: WearableShadowRepository,
): SettingsViewModel {
    val settingsEntry = remember(entry) {
        navController.getBackStackEntry(Routes.SETTINGS)
    }
    return viewModel(
        viewModelStoreOwner = settingsEntry,
        factory = AppViewModelFactory {
            SettingsViewModel(
                repository = repository,
                preferencesRepository = preferencesRepository,
                catalogRepository = catalogRepository,
                recoveryResearchRepository = recoveryResearchRepository,
                wearableShadowRepository = wearableShadowRepository,
            )
        },
    )
}

@Composable
private fun ConfigureSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = backgroundColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Custom bottom navigation with a single rounded pill that slides between tabs. Items are equal
 * width, so the pill position is computed as itemWidth × animatedIndex — no per-item measurement.
 * Preserves the navigation-bar safe-area inset, label/icon, hitboxes (full-slot clickable) and the
 * Scaffold's bottom inset (it lives in the bottomBar slot so content is never covered).
 */
@Composable
private fun GymBottomBar(
    destinations: List<TopLevelDestination>,
    currentDestination: NavDestination?,
    onSelect: (String) -> Unit,
    // LOG-V4B: opt-in liquid glass for the dock. Defaults to a no-op so the protected bar body is
    // untouched when the effect is off; disable globally via AuronGlass.ENABLED.
    glassModifier: Modifier = Modifier,
    /**
     * True when the true backdrop refraction is running on the layer behind this dock.
     *
     * When true the dock drops its opaque fill and its synthetic gradient, because the refracted
     * Home-screen content now shows through it. Those two must change together — keeping the
     * opaque fill would hide the refraction, and keeping the fake gradient would double up with
     * the real content behind it.
     */
    backdropLensActive: Boolean = false,
    /** Reports this dock's rect (centre + half size, px) so the backdrop lens knows where to bend. */
    onLensMeasured: (centerX: Float, centerY: Float, halfWidth: Float, halfHeight: Float) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    val selectedIndex = destinations
        .indexOfFirst { dest -> currentDestination?.hierarchy?.any { it.route == dest.route } == true }
        .coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "navIndicator",
    )

    // Floating rounded dock (Apple Music style): side + bottom margins so it visually floats above the
    // page background, with the navigation-bar safe-area inset preserved. As an overlay it wraps its
    // own height (not fillMaxHeight), so it never blocks content. Hitboxes stay inside the rounded
    // dock — the transparent side margins are not clickable, so taps there fall through to content.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
    ) {
        Surface(
            // With the backdrop lens active the dock must let the refracted content through, so it
            // becomes a light tint instead of a solid fill. The tint is what keeps the light-grey
            // icons legible when a bright card scrolls underneath; it is the AURON equivalent of
            // iOS's "tinted" glass variant.
            //
            // Without the lens (kill switch off, or API < 33) we keep the original opaque colour so
            // the degraded path looks exactly like it did before this feature existed.
            color = when {
                backdropLensActive -> Color(0x1F000000)
                isAuronDarkTheme() -> Color(0xFF0F1115)
                else -> MaterialTheme.colorScheme.surface
            },
            shape = RoundedCornerShape(30.dp),
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            // The hairline is what separates the glass from the page, so it is strengthened a
            // little once the fill behind it goes translucent.
            border = gymContainerBorder(alpha = if (backdropLensActive) 0.14f else 0.06f),
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    // Reported in root coordinates; the background lens layer also spans the whole
                    // screen from (0,0), so the two coordinate spaces line up directly.
                    val position = coordinates.positionInRoot()
                    val size = coordinates.size
                    onLensMeasured(
                        position.x + size.width / 2f,
                        position.y + size.height / 2f,
                        size.width / 2f,
                        size.height / 2f,
                    )
                },
        ) {
            // Surface lighting only. The synthetic vertical gradient is intentionally gone while
            // the backdrop lens is active: the shader now bends *real* content behind the dock,
            // and layering a fake gradient on top of that would just muddy it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .then(if (backdropLensActive) Modifier else Modifier.background(
                        Brush.verticalGradient(
                            0f to Color(0xFF1A1D26),     // top: lighter, catches ambient light
                            0.5f to Color(0xFF0F1115),   // middle: original bar colour
                            1f to Color(0xFF06070A),     // bottom: darker, gravity shadow
                        )
                    ))
                    .then(glassModifier),
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    val itemWidth = maxWidth / destinations.size
                    // Sliding indicator, drawn behind the items.
                    Box(
                        modifier = Modifier
                            .offset(x = itemWidth * animatedIndex)
                            .width(itemWidth)
                            .fillMaxHeight()
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Row(modifier = Modifier.fillMaxSize()) {
                    destinations.forEachIndexed { index, destination ->
                        val selected = index == selectedIndex
                        val contentColor by animateColorAsState(
                            targetValue = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(220),
                            label = "navItemColor$index",
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .clickable { onSelect(destination.route) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.label,
                                tint = contentColor,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
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
