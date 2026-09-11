package com.hoctin.gymtracker.ui.onboarding

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import com.hoctin.gymtracker.R
import com.hoctin.gymtracker.ui.components.formatWeekdays
import com.hoctin.gymtracker.ui.theme.auronScreenTopPadding
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onExit: (OnboardingExit) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    var receiptReady by remember(state.step, motionEnabled) {
        mutableStateOf(state.step != OnboardingStepV3.RECEIPT || !motionEnabled)
    }

    OnboardingSystemBars(view)

    BackHandler(enabled = state.step != OnboardingStepV3.WELCOME && !state.isSaving) {
        viewModel.back()
    }

    LaunchedEffect(viewModel, view) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.SuccessHaptic -> performSuccessHaptic(view)
                is OnboardingEffect.Exit -> onExit(effect.destination)
            }
        }
    }

    OnboardingBackground {
        Column(Modifier.fillMaxSize()) {
            OnboardingTopBar(
                step = state.step,
                progress = state.progressFraction,
                motionEnabled = motionEnabled,
                onBack = viewModel::back,
                modifier = Modifier.padding(
                    start = 10.dp,
                    end = 12.dp,
                    top = auronScreenTopPadding(8.dp),
                ),
            )

            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    if (!motionEnabled) {
                        fadeIn(tween(90)).togetherWith(fadeOut(tween(90)))
                    } else {
                        fadeIn(tween(430)).togetherWith(fadeOut(tween(310)))
                    }
                },
                contentKey = { it },
                label = "onboardingStep",
                modifier = Modifier.weight(1f),
            ) { visibleStep ->
                OnboardingStepBody(
                    step = visibleStep,
                    state = state,
                    motionEnabled = motionEnabled,
                    view = view,
                    viewModel = viewModel,
                    onReceiptReady = { receiptReady = true },
                )
            }

            OnboardingBottomActions(
                state = state,
                motionEnabled = motionEnabled,
                viewModel = viewModel,
                receiptReady = receiptReady,
            )
        }
    }
}

/**
 * Onboarding owns a dark canvas even when the rest of the app follows Light theme. Keep the
 * system icons readable while this route is visible, then restore the app-level bar appearance.
 */
@Composable
private fun OnboardingSystemBars(view: View) {
    if (view.isInEditMode) return
    val window = view.context.findActivity()?.window ?: return
    val controller = WindowCompat.getInsetsController(window, view)

    DisposableEffect(window, view) {
        val previousStatusColor = window.statusBarColor
        val previousNavigationColor = window.navigationBarColor
        val previousLightStatus = controller.isAppearanceLightStatusBars
        val previousLightNavigation = controller.isAppearanceLightNavigationBars
        val previousStatusContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced
        } else {
            false
        }
        val previousNavigationContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced
        } else {
            false
        }

        onDispose {
            window.statusBarColor = previousStatusColor
            window.navigationBarColor = previousNavigationColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = previousStatusContrast
                window.isNavigationBarContrastEnforced = previousNavigationContrast
            }
            controller.isAppearanceLightStatusBars = previousLightStatus
            controller.isAppearanceLightNavigationBars = previousLightNavigation
        }
    }

    SideEffect {
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Black.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun OnboardingStepBody(
    step: OnboardingStepV3,
    state: OnboardingUiState,
    motionEnabled: Boolean,
    view: View,
    viewModel: OnboardingViewModel,
    onReceiptReady: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontalPadding = when {
            maxWidth < 340.dp -> 16.dp
            maxWidth < 390.dp -> 20.dp
            else -> 24.dp
        }
        if (step == OnboardingStepV3.RECEIPT) {
            ReceiptStep(
                state = state,
                motionEnabled = motionEnabled,
                viewModel = viewModel,
                onReady = onReceiptReady,
                modifier = Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = 12.dp),
            )
            return@BoxWithConstraints
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            when (step) {
                OnboardingStepV3.WELCOME -> WelcomeStep(state, viewModel)
                OnboardingStepV3.NAME -> NameStep(state, viewModel)
                OnboardingStepV3.GENDER -> GenderStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.AGE -> AgeStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.GOAL -> GoalStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.EXPERIENCE -> ExperienceStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.COMPOUND_EXPERIENCE -> FrequencyStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.FREQUENCY -> FrequencyStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.SCHEDULE -> ScheduleStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.DURATION -> DurationStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.ENVIRONMENT -> EnvironmentStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.EQUIPMENT -> EquipmentStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.PRIORITY_MUSCLES -> PriorityMusclesStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.PHYSIQUE -> PhysiqueStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.MEASUREMENTS -> MeasurementsStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.TARGET_WEIGHT -> TargetWeightStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.RECOVERY -> ConstraintsStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.CONSTRAINTS -> ConstraintsStep(state, motionEnabled, view, viewModel)
                OnboardingStepV3.EVIDENCE,
                OnboardingStepV3.PROFILE_READY -> ReviewStep(state, viewModel)
                OnboardingStepV3.REVIEW -> ReviewStep(state, viewModel)
                OnboardingStepV3.RECEIPT -> Unit
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun WelcomeStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val palette = onboardingPalette()
    var showPreparing by remember(state.isPreparing) { mutableStateOf(false) }
    LaunchedEffect(state.isPreparing) {
        showPreparing = false
        if (state.isPreparing) {
            delay(300)
            showPreparing = true
        }
    }
    OnboardingHeroQuestion(
        imageRes = R.drawable.onboarding_hero_welcome,
        eyebrow = "AURON PROFILE",
        title = "Build a week you can keep.",
        support = WELCOME_SUPPORT_COPY,
        minHeight = 224.dp,
    )
    Text("Private on this device. No account or workout is created.", color = palette.textSecondary)
    if (showPreparing) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = palette.selectedBorder, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            Text("Preparing your starting plan…", color = palette.textSecondary)
        }
    }
    if (state.error == OnboardingError.PREPARATION_FAILED || state.error == OnboardingError.PLAN_UNAVAILABLE) {
        OnboardingInlineError("We couldn’t prepare your starting plan.", "Retry", viewModel::retryPreparation)
    }
}

@Composable
private fun NameStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    OnboardingQuestion(
        eyebrow = "Let’s start",
        title = "What should AURON call you?",
        support = "Your name is optional and stays on this device.",
    )
    Spacer(Modifier.height(38.dp))
    OnboardingInput(
        value = state.profile.displayName,
        onValueChange = viewModel::setDisplayName,
        label = "Name",
        placeholder = "Your first name",
    )
}

@Composable
private fun GenderStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    var showInfo by remember { mutableStateOf(false) }
    OnboardingQuestion(
        title = "I am…",
        support = "Choose one profile option.",
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        OnboardingInfoIconButton(onClick = { showInfo = true }, contentDescription = "Why AURON asks")
    }
    Spacer(Modifier.height(12.dp))
    OnboardingBinaryChoiceGrid(
        selected = state.profile.gender,
        motionEnabled = motionEnabled,
        onSelect = { value ->
            if (viewModel.updateProfile {
                it.copy(gender = value, skippedFields = it.skippedFields - OptionalProfileField.GENDER)
            }) performToggleHaptic(view, true)
        },
    )
    if (showInfo) GenderPrivacyDialog(onDismiss = { showInfo = false }, motionEnabled = motionEnabled)
}

@Composable
private fun GenderPrivacyDialog(onDismiss: () -> Unit, motionEnabled: Boolean) {
    val palette = onboardingPalette()
    val shape = RoundedCornerShape(20.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .clip(shape)
                .background(palette.panelFill)
                .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Profile privacy", color = palette.textPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                "This answer stays on this device. It is saved as profile context for future calculations; it does not change your plan or make sex-based programming assumptions today.",
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
            )
            OnboardingPrimaryButton(
                text = "OK",
                enabled = true,
                onClick = onDismiss,
                motionEnabled = motionEnabled,
                showArrow = false,
            )
        }
    }
}

@Composable
private fun AgeStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    OnboardingQuestion(
        title = "Which age group fits you?",
        support = "Used only to keep measurement screening appropriate.",
    )
    Spacer(Modifier.height(24.dp))
    ChoiceList(
        values = AgeGroup.entries,
        selected = state.profile.ageGroup,
        title = { it.label },
        description = { if (it == AgeGroup.TEEN) "No adult BMI threshold" else "Adult BMI screening" },
        motionEnabled = motionEnabled,
        view = view,
        onSelect = { value -> viewModel.updateProfile { it.copy(ageGroup = value) } },
    )
}

@Composable
private fun GoalStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    OnboardingHeroQuestion(
        imageRes = R.drawable.onboarding_hero_goal,
        eyebrow = "Coaching goal",
        title = "What’s your main training goal?",
        support = "Pick what matters most. This sets AURON’s current coaching goal; it does not regenerate your starting split.",
    )
    ChoiceList(
        values = PrimaryTrainingGoal.entries,
        selected = state.profile.primaryGoal,
        title = { it.label },
        description = { it.description },
        icon = { it.icon },
        motionEnabled = motionEnabled,
        view = view,
        onSelect = { value -> viewModel.updateProfile { it.copy(primaryGoal = value) } },
    )
}

@Composable
private fun ExperienceStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    OnboardingHeroQuestion(
        imageRes = R.drawable.onboarding_hero_experience,
        eyebrow = "Training experience",
        title = "What’s your training experience?",
        support = "Choose the level that reflects consistent training, not your best single lift.",
    )
    Spacer(Modifier.height(28.dp))
    ChoiceList(
        values = TrainingExperience.entries,
        selected = state.profile.experience,
        title = { it.label },
        description = { it.description },
        motionEnabled = motionEnabled,
        view = view,
        onSelect = { value -> viewModel.updateProfile { it.copy(experience = value) } },
    )
}

@Composable
private fun FrequencyStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    val fallbackDays = state.plan?.requiredDays?.takeIf { it in 1..7 } ?: 4
    LaunchedEffect(state.profile.desiredDaysPerWeek) {
        if (state.profile.desiredDaysPerWeek == null) {
            viewModel.updateProfile { it.copy(desiredDaysPerWeek = fallbackDays) }
        }
    }
    OnboardingQuestion(
        title = "How many days per week?",
        support = "Choose a weekly target from 1 to 7 days.",
    )
    Spacer(Modifier.height(34.dp))
    val value = (state.profile.desiredDaysPerWeek ?: fallbackDays).toDouble()
    OnboardingRulerPicker(
        value = value,
        range = 1.0..7.0,
        step = 1.0,
        valueLabel = { days -> "${days.roundToInt()} ${if (days.roundToInt() == 1) "day" else "days"}" },
        semanticLabel = "training days per week",
        motionEnabled = motionEnabled,
        onValueChange = { raw ->
            val days = raw.roundToInt().coerceIn(1, 7)
            viewModel.updateProfile { profile ->
                profile.copy(
                    desiredDaysPerWeek = days,
                    preferredWeekdays = profile.preferredWeekdays.sorted().take(days).toSortedSet(),
                )
            }
        },
        onTick = { performSegmentTickHaptic(view) },
    )
}

@Composable
private fun ScheduleStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    val palette = onboardingPalette()
    val required = state.profile.desiredDaysPerWeek ?: 0
    val selected = state.profile.preferredWeekdays.size
    OnboardingQuestion(
        title = "Choose your training days",
        support = "Select exactly $required ${if (required == 1) "day" else "days"}.",
    )
    Text(
        "$selected of $required selected",
        color = if (selected == required && required > 0) palette.selectedBorder else palette.textSecondary,
        fontWeight = FontWeight.ExtraBold,
    )
    val weekdays = listOf(
        1 to "Monday",
        2 to "Tuesday",
        3 to "Wednesday",
        4 to "Thursday",
        5 to "Friday",
        6 to "Saturday",
        7 to "Sunday",
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        weekdays.forEachIndexed { index, (day, label) ->
            val isSelected = day in state.profile.preferredWeekdays
            OnboardingStaggeredItem(index, motionEnabled) {
                OnboardingToggleRow(
                    title = label,
                    selected = isSelected,
                    enabled = isSelected || selected < required,
                    motionEnabled = motionEnabled,
                    onClick = {
                        if (viewModel.toggleWeekday(day)) performToggleHaptic(view, !isSelected)
                    },
                )
            }
        }
    }
    Text(SCHEDULE_SCOPE_COPY, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun DurationStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    OnboardingQuestion(
        title = "How long should a session feel?",
        support = "Choose your preferred training window.",
    )
    Spacer(Modifier.height(34.dp))
    ChoiceList(
        values = SessionDuration.entries,
        selected = state.profile.sessionDuration,
        title = { it.label },
        description = { "" },
        motionEnabled = motionEnabled,
        view = view,
        onSelect = { value -> viewModel.updateProfile { it.copy(sessionDuration = value) } },
    )
}

@Composable
private fun EnvironmentStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    OnboardingHeroQuestion(
        imageRes = R.drawable.onboarding_hero_environment,
        eyebrow = "Training space",
        title = "Where do you exercise most?",
        support = "Choose your usual training space.",
    )
    Spacer(Modifier.height(28.dp))
    TrainingEnvironment.entries.forEachIndexed { index, value ->
        OnboardingStaggeredItem(index, motionEnabled) {
            OnboardingChoiceRow(
                title = value.label,
                description = "",
                icon = value.icon,
                selected = state.profile.environment == value,
                motionEnabled = motionEnabled,
                onClick = {
                    if (viewModel.selectEnvironment(value)) performToggleHaptic(view, true)
                },
            )
        }
    }
}

@Composable
private fun EquipmentStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    OnboardingQuestion(
        title = "Available equipment",
        support = "Select what you can reliably use.",
    )
    Text(
        "${state.profile.equipment.size} selected",
        color = onboardingPalette().selectedBorder,
        fontWeight = FontWeight.ExtraBold,
    )
    OnboardingMultiChoiceGrid(
        items = AvailableEquipment.entries.map { Triple(it.label, it.icon, it in state.profile.equipment) },
        motionEnabled = motionEnabled,
        onToggle = { index ->
            val value = AvailableEquipment.entries[index]
            if (viewModel.toggleEquipment(value)) performToggleHaptic(view, value in viewModel.uiState.value.profile.equipment)
        },
    )
}

@Composable
private fun PriorityMusclesStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    OnboardingQuestion(
        title = "What would you like to emphasize?",
        support = "Choose any muscle groups that matter most to you. This is optional future-plan context.",
    )
    OnboardingMultiChoiceGrid(
        items = PriorityMuscle.entries.map { Triple(it.label, it.icon, it in state.profile.priorityMuscles) },
        motionEnabled = motionEnabled,
        onToggle = { index ->
            val value = PriorityMuscle.entries[index]
            if (viewModel.togglePriorityMuscle(value)) performToggleHaptic(view, value in viewModel.uiState.value.profile.priorityMuscles)
        },
    )
}

@Composable
private fun PhysiqueStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    OnboardingQuestion(
        title = "Do you have a physique direction?",
        support = "Optional aesthetic intent only. AURON makes no body-composition promise from this answer.",
    )
    ChoiceList(
        values = PhysiqueDirection.entries,
        selected = state.profile.physiqueDirection,
        title = { it.label },
        description = { it.description },
        motionEnabled = motionEnabled,
        view = view,
        onSelect = { value ->
            viewModel.updateProfile {
                it.copy(
                    physiqueDirection = value,
                    skippedFields = it.skippedFields - OptionalProfileField.PHYSIQUE_DIRECTION,
                )
            }
        },
    )
}

@Composable
private fun MeasurementsStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    val profile = state.profile
    LaunchedEffect(Unit) {
        if (OptionalProfileField.MEASUREMENTS !in profile.skippedFields &&
            (profile.heightCm == null || profile.weightKg == null)
        ) {
            viewModel.updateProfile {
                it.copy(
                    heightCm = it.heightCm ?: 175.0,
                    weightKg = it.weightKg ?: 70.0,
                    skippedFields = it.skippedFields - OptionalProfileField.MEASUREMENTS,
                )
            }
        }
    }
    OnboardingQuestion(
        title = "Set your height and weight",
        support = "Swipe each ruler. You can change units without changing the saved value.",
    )
    OnboardingSegmentedControl(
        options = listOf("cm", "ft / in"),
        selectedIndex = if (profile.heightUnit == HeightUnit.CM) 0 else 1,
        onSelected = { index ->
            val value = if (index == 0) HeightUnit.CM else HeightUnit.FEET_INCHES
            if (viewModel.updateProfile { it.copy(heightUnit = value) }) performToggleHaptic(view, true)
        },
    )
    OnboardingRulerPicker(
        value = profile.heightCm ?: 175.0,
        range = 120.0..230.0,
        step = 1.0,
        valueLabel = { heightLabel(it, profile.heightUnit) },
        semanticLabel = "height",
        motionEnabled = motionEnabled,
        onValueChange = viewModel::setHeightCm,
        onTick = { performSegmentTickHaptic(view) },
    )
    OnboardingSegmentedControl(
        options = listOf("kg", "lb"),
        selectedIndex = if (profile.weightUnit == WeightUnit.KG) 0 else 1,
        onSelected = { index ->
            val value = if (index == 0) WeightUnit.KG else WeightUnit.LB
            if (viewModel.updateProfile { it.copy(weightUnit = value) }) performToggleHaptic(view, true)
        },
    )
    OnboardingRulerPicker(
        value = profile.weightKg ?: 70.0,
        range = 35.0..250.0,
        step = 0.1,
        valueLabel = { weightLabel(it, profile.weightUnit) },
        semanticLabel = "current weight",
        motionEnabled = motionEnabled,
        onValueChange = viewModel::setWeightKg,
        onTick = { performSegmentTickHaptic(view) },
    )
    val bmi = state.bmi
    if (bmi.value != null) {
        Text(
            text = if (bmi.status == BmiStatus.ADULT_SCREENING_ONLY) {
                "Adult BMI screening: ${bmi.value}. BMI is not a diagnosis."
            } else {
                "Adult BMI thresholds are not applied to this age group."
            },
            color = onboardingPalette().textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TargetWeightStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    val profile = state.profile
    val defaultKg = profile.targetWeightKg ?: profile.weightKg ?: 70.0
    LaunchedEffect(Unit) {
        if (OptionalProfileField.TARGET_WEIGHT !in profile.skippedFields && profile.targetWeightKg == null) {
            viewModel.setTargetWeightKg(defaultKg)
        }
    }
    OnboardingQuestion(
        title = "What weight feels right for you?",
        support = "Set a target to complete your training profile.",
    )
    Spacer(Modifier.height(28.dp))
    OnboardingRulerPicker(
        value = defaultKg,
        range = 35.0..250.0,
        step = 0.1,
        valueLabel = { weightLabel(it, profile.weightUnit) },
        semanticLabel = "target weight",
        motionEnabled = motionEnabled,
        onValueChange = viewModel::setTargetWeightKg,
        onTick = { performSegmentTickHaptic(view) },
        contextLabel = targetWeightDirection(profile.weightKg, defaultKg),
    )
    val screening = state.targetBmiScreening
    screening.referenceRange?.let { range ->
        TargetWeightRangePanel(range = range, unit = profile.weightUnit)
    }
    when {
        screening.blocksContinue -> TargetWeightWarningPanel(
            height = profile.heightCm?.let { heightLabel(it, profile.heightUnit) }.orEmpty(),
            bmi = screening.value,
        )
        screening.isAboveReferenceRange -> TargetWeightHighAdvisory(
            bmi = screening.value,
        )
        screening.value != null -> Text(
            if (screening.adultThresholdApplied) {
                "BMI ${screening.value} is within the adult screening reference range. BMI is not a diagnosis."
            } else {
                "Adult BMI thresholds are not applied to the 13–19 age group."
            },
            color = onboardingPalette().textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TargetWeightRangePanel(
    range: AdultWeightReferenceRange,
    unit: WeightUnit,
) {
    val palette = onboardingPalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.20f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 17.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = "RECOMMENDED WEIGHT RANGE",
            color = palette.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.45.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = referenceWeightRangeLabel(range, unit),
            color = palette.textPrimary,
            fontSize = 24.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Adult BMI screening reference only",
            color = palette.textSecondary.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TargetWeightWarningPanel(
    height: String,
    bmi: Double?,
) {
    val palette = onboardingPalette()
    val message = buildAnnotatedString {
        append("Based on your height of ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = palette.textPrimary)) { append(height) }
        append(", this goal weight gives a BMI of ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = palette.textPrimary)) {
            append(bmi?.let(::compactNumber) ?: "—")
        }
        append(". AURON isn't designed to reach a goal weight below ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = palette.textPrimary)) { append("BMI 18.5") }
        append(" (WHO's underweight threshold).")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.error.copy(alpha = 0.095f))
            .border(1.dp, palette.error.copy(alpha = 0.62f), RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            color = palette.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
        )
        Text(
            text = "Please select a higher goal weight to continue.",
            color = palette.error,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TargetWeightHighAdvisory(bmi: Double?) {
    val palette = onboardingPalette()
    val amber = Color(0xFFFFC857)
    Text(
        text = "This target gives an adult BMI screening value of ${bmi?.let(::compactNumber) ?: "—"}, above the reference range. BMI is not a diagnosis, and Continue remains available.",
        color = palette.textSecondary,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = 22.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(amber.copy(alpha = 0.08f))
            .border(1.dp, amber.copy(alpha = 0.42f), RoundedCornerShape(20.dp))
            .padding(18.dp),
    )
}

@Composable
private fun ConstraintsStep(state: OnboardingUiState, motionEnabled: Boolean, view: View, viewModel: OnboardingViewModel) {
    OnboardingQuestion(
        title = "Any injuries?",
        support = "Optional. Add only what you want AURON to remember.",
    )
    OnboardingInput(
        value = state.profile.injuryNotes,
        onValueChange = viewModel::setInjuryNotes,
        label = "Injuries or limitations",
        placeholder = "Example: previous ankle sprain…",
        singleLine = false,
        minLines = 4,
    )
    Text(
        INJURY_DISCLAIMER_COPY,
        color = onboardingPalette().textSecondary,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun ReviewStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val palette = onboardingPalette()
    val profile = state.profile
    val plan = state.plan
    OnboardingQuestion(
        eyebrow = "Review",
        title = "Review your training profile",
        support = "Nothing is logged until you complete and save a real workout.",
    )
    ReviewGroup(
        title = "Goal and experience",
        value = listOfNotNull(profile.primaryGoal?.label, profile.experience?.label).joinToString(" · "),
        onEdit = viewModel::editIntent,
    )
    ReviewGroup(
        title = "Schedule and time",
        value = "${profile.desiredDaysPerWeek ?: "—"} preferred · ${formatWeekdays(profile.preferredWeekdays)} · ${profile.sessionDuration?.label ?: "—"}",
        onEdit = viewModel::editSchedule,
    )
    ReviewGroup(
        title = "Environment and focus",
        value = "${profile.environment?.label ?: "—"} · ${profile.equipment.size} equipment · ${profile.priorityMuscles.size} priorities",
        onEdit = viewModel::editEquipment,
    )
    ReviewGroup(
        title = "Body metrics",
        value = measurementsSummary(profile, state.bmi),
        onEdit = viewModel::editMeasurements,
    )
    ReviewGroup(
        title = "Injuries",
        value = profile.injuryNotes.trim().ifBlank { "None provided" },
        onEdit = viewModel::editRecovery,
    )
    HorizontalDivider(color = palette.hairline)
    OnboardingReceiptRow(
        label = "Current starting split",
        value = plan?.dayNames?.joinToString(" · ").orEmpty().ifBlank { "Plan unavailable" },
    )
    OnboardingReceiptRow(
        label = "Current coaching goal",
        value = profile.primaryGoal?.label ?: "Auto",
    )
    OnboardingReceiptRow(
        label = "Current Weekly Progress",
        value = "${profile.preferredWeekdays.size} workout days · workout-only",
    )
    OnboardingInfoPanel("Truth boundary", REVIEW_TRUTH_COPY)
}

@Composable
private fun ReviewGroup(title: String, value: String, onEdit: () -> Unit) {
    OnboardingReceiptRow(
        label = title,
        value = value,
        action = { OnboardingTextAction("Edit", onEdit, color = onboardingPalette().selectedBorder) },
    )
    HorizontalDivider(color = onboardingPalette().hairline)
}

@Composable
private fun ReceiptStep(
    state: OnboardingUiState,
    motionEnabled: Boolean,
    viewModel: OnboardingViewModel,
    onReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = onboardingPalette()
    val profile = state.profile
    val startedAt = rememberSaveable { SystemClock.elapsedRealtime() }
    val cadenceSeed = rememberSaveable { (startedAt xor (startedAt ushr 32)).toInt() }
    var elapsedMillis by rememberSaveable {
        mutableLongStateOf(if (motionEnabled) 0L else 10_000L)
    }
    LaunchedEffect(startedAt, motionEnabled) {
        if (!motionEnabled) {
            elapsedMillis = 10_000L
            onReady()
            return@LaunchedEffect
        }
        while (elapsedMillis < 10_000L) {
            elapsedMillis = (SystemClock.elapsedRealtime() - startedAt).coerceIn(0L, 10_000L)
            delay(32L)
        }
        onReady()
    }
    val frame = receiptPreparationFrame(elapsedMillis, cadenceSeed)
    LaunchedEffect(frame.complete) {
        if (frame.complete) onReady()
    }
    Crossfade(
        targetState = frame.complete,
        animationSpec = tween(if (motionEnabled) 520 else 0),
        label = "receiptPreparationToSuccess",
        modifier = modifier,
    ) { complete ->
        if (!complete) {
            ReceiptPreparation(frame = frame, modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(142.dp)
                        .clip(CircleShape)
                        .background(palette.selectedBorder.copy(alpha = 0.70f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(76.dp))
                }
                Spacer(Modifier.height(34.dp))
                Text(
                    text = "Your training profile is ready",
                    color = palette.textPrimary,
                    fontSize = 31.sp,
                    lineHeight = 37.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (profile.displayName.isNotBlank()) {
                        "Your schedule, equipment and experience are saved, ${profile.displayName.trim()}. Review your starting week before you begin."
                    } else {
                        "Your schedule, equipment and experience are saved. Review your starting week before you begin."
                    },
                    color = palette.textSecondary,
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                OnboardingTextAction(
                    "Adjust setup",
                    viewModel::adjustSetup,
                    color = palette.selectedBorder,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ReceiptPreparation(
    frame: ReceiptPreparationFrame,
    modifier: Modifier = Modifier,
) {
    val palette = onboardingPalette()
    val (title, support) = when (frame.phase) {
        ReceiptPreparationPhase.CALCULATING ->
            "Calculating…" to "Parsing your goals and equipment…"
        ReceiptPreparationPhase.BUILDING ->
            "Building your setup…" to "Crafting a foundation around your schedule and level…"
        ReceiptPreparationPhase.PERSONALIZING ->
            "Personalizing…" to "Optimizing your profile structure for streamlined progress…"
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { frame.progress / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF2CF68C),
                    trackColor = Color(0xFF405B74),
                    strokeWidth = 18.dp,
                )
                Text(
                    text = "${frame.progress}%",
                    color = palette.textPrimary,
                    fontSize = 52.sp,
                    lineHeight = 58.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(34.dp))
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = 27.sp,
                lineHeight = 33.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = support,
                color = palette.textSecondary.copy(alpha = 0.76f),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OnboardingBottomActions(
    state: OnboardingUiState,
    motionEnabled: Boolean,
    viewModel: OnboardingViewModel,
    receiptReady: Boolean,
) {
    if (state.step == OnboardingStepV3.RECEIPT && !receiptReady) return
    val palette = onboardingPalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, palette.canvasBottom.copy(alpha = 0.97f))))
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state.error) {
            OnboardingError.SAVE_FAILED -> OnboardingInlineError("Your profile wasn’t saved. Try again.")
            OnboardingError.RECEIPT_EXIT_FAILED -> OnboardingInlineError("Your profile is saved, but this screen could not close. Try again.")
            OnboardingError.SCHEDULE_CHANGED -> OnboardingInlineError("Your plan changed. Review the starting weekdays again.")
            else -> Unit
        }
        val hasDayOne = state.plan?.firstDayId != null
        val buttonText = when (state.step) {
            OnboardingStepV3.WELCOME -> "Build my profile"
            OnboardingStepV3.REVIEW -> if (state.isSaving) "Saving profile…" else "Save profile"
            OnboardingStepV3.RECEIPT -> if (state.isSaving) "Opening…" else if (hasDayOne) "Review Day 1" else "Go to Home"
            else -> if (state.isEditingReviewSection && state.draft.editReturnAfter == state.step) "Save changes" else "Continue"
        }
        OnboardingPrimaryButton(
            text = buttonText,
            enabled = state.canContinue,
            motionEnabled = motionEnabled,
            showArrow = !state.isSaving,
            onClick = {
                when (state.step) {
                    OnboardingStepV3.REVIEW -> viewModel.confirmSetup()
                    OnboardingStepV3.RECEIPT -> if (hasDayOne) viewModel.finishToReviewDay1() else viewModel.finishToHome()
                    else -> viewModel.next()
                }
            },
        )
        when {
            state.step == OnboardingStepV3.RECEIPT && hasDayOne -> {
                Spacer(Modifier.height(9.dp))
                OnboardingSecondaryButton("Go to Home", motionEnabled, viewModel::finishToHome)
            }
            state.step == OnboardingStepV3.REVIEW -> {
                Spacer(Modifier.height(9.dp))
                OnboardingSecondaryButton("Start over", motionEnabled, viewModel::startOver)
            }
            state.step in OPTIONAL_SKIP_STEPS -> {
                Spacer(Modifier.height(9.dp))
                OnboardingSecondaryButton("Skip", motionEnabled, viewModel::skipCurrentStep)
            }
        }
    }
}

@Composable
private fun <T> ChoiceList(
    values: List<T>,
    selected: T?,
    title: (T) -> String,
    description: (T) -> String,
    motionEnabled: Boolean,
    view: View,
    onSelect: (T) -> Boolean,
    icon: (T) -> String? = { null },
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        values.forEachIndexed { index, value ->
            OnboardingStaggeredItem(index, motionEnabled) {
                OnboardingChoiceRow(
                    title = title(value),
                    description = description(value),
                    selected = selected == value,
                    icon = icon(value),
                    motionEnabled = motionEnabled,
                    onClick = {
                        if (onSelect(value)) performToggleHaptic(view, true)
                    },
                )
            }
        }
    }
}

@Composable
private fun OnboardingInlineError(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val palette = onboardingPalette()
    Row(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = palette.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) OnboardingTextAction(action, onAction, color = palette.error)
    }
}

private fun measurementsSummary(profile: OnboardingTrainingProfileDraft, bmi: BmiResult): String {
    if (!MeasurementConversion.validHeightCm(profile.heightCm) || !MeasurementConversion.validWeightKg(profile.weightKg)) {
        return "Skipped"
    }
    val height = if (profile.heightUnit == HeightUnit.CM) {
        "${compactNumber(profile.heightCm!!)} cm"
    } else {
        val inches = MeasurementConversion.centimetersToInches(profile.heightCm!!)
        "${floor(inches / 12).toInt()} ft ${(inches % 12).roundToInt()} in"
    }
    val displayWeight = if (profile.weightUnit == WeightUnit.KG) profile.weightKg!! else MeasurementConversion.kilogramsToPounds(profile.weightKg!!)
    val bmiText = bmi.value?.let { " · BMI $it screening" }.orEmpty()
    return "$height · ${compactNumber(displayWeight)} ${profile.weightUnit.storageValue}$bmiText"
}

private fun heightLabel(heightCm: Double, unit: HeightUnit): String = if (unit == HeightUnit.CM) {
    "${heightCm.roundToInt()} cm"
} else {
    val totalInches = MeasurementConversion.centimetersToInches(heightCm).roundToInt()
    "${totalInches / 12} ft ${totalInches % 12} in"
}

private fun weightLabel(weightKg: Double, unit: WeightUnit): String {
    val value = if (unit == WeightUnit.KG) weightKg else MeasurementConversion.kilogramsToPounds(weightKg)
    return "${compactNumber(value)} ${unit.storageValue}"
}

private fun targetWeightDirection(currentWeightKg: Double?, targetWeightKg: Double): String = when {
    currentWeightKg == null -> "Target weight"
    targetWeightKg < currentWeightKg - 0.05 -> "Lose weight"
    targetWeightKg > currentWeightKg + 0.05 -> "Gain weight"
    else -> "Maintain weight"
}

private fun referenceWeightRangeLabel(
    range: AdultWeightReferenceRange,
    unit: WeightUnit,
): String = if (unit == WeightUnit.KG) {
    "${range.minimumKg} kg – ${range.maximumKg} kg"
} else {
    val minimumLb = MeasurementConversion.kilogramsToPounds(range.minimumKg.toDouble()).roundToInt()
    val maximumLb = MeasurementConversion.kilogramsToPounds(range.maximumKg.toDouble()).roundToInt()
    "$minimumLb lb – $maximumLb lb"
}

private fun compactNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

private fun performToggleHaptic(view: View, selected: Boolean) {
    val feedback = if (Build.VERSION.SDK_INT >= 34) {
        if (selected) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
    } else {
        HapticFeedbackConstants.VIRTUAL_KEY
    }
    view.performHapticFeedback(feedback)
}

private fun performSegmentTickHaptic(view: View) {
    val feedback = if (Build.VERSION.SDK_INT >= 34) {
        HapticFeedbackConstants.SEGMENT_TICK
    } else {
        HapticFeedbackConstants.CLOCK_TICK
    }
    view.performHapticFeedback(feedback)
}

private fun performSuccessHaptic(view: View) {
    val feedback = if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
    view.performHapticFeedback(feedback)
}

private val OPTIONAL_SKIP_STEPS = setOf(
    OnboardingStepV3.NAME,
    OnboardingStepV3.PRIORITY_MUSCLES,
    OnboardingStepV3.PHYSIQUE,
    OnboardingStepV3.CONSTRAINTS,
)
