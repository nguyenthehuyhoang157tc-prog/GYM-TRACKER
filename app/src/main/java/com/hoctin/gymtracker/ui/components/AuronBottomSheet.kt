package com.hoctin.gymtracker.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hoctin.gymtracker.ui.theme.isAuronDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared AURON half-screen bottom sheet shell. Matches the Weekly Progress Hub: a plain Material
 * `surface` container (no dotted/star texture inside the popup), a clean drag handle, and a padded,
 * scrollable content column. `onDismiss` fires for swipe-down, back press and scrim taps — callers
 * should treat it purely as "cancel/close". Encapsulating the experimental Material3 API here keeps
 * call sites opt-in free.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuronModalSheet(
    onDismiss: () -> Unit,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 28.dp),
            verticalArrangement = verticalArrangement,
        ) {
            // Clean drag handle (same style as the Weekly Progress Hub sheet).
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 4.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)),
            )
            content()
        }
    }
}

/**
 * Compact floating modal used by the shared exercise detail and other content-first popups. Unlike
 * Material's full-height bottom sheet, this shell keeps a hard visible-height ceiling and scrolls
 * its content internally. The entrance follows AURON's restrained motion language: a slow upward
 * settle with a very small soft overshoot, plus a separate scrim fade. System-disabled animation
 * is respected.
 */
@Composable
fun AuronCompactModalSheet(
    onDismiss: () -> Unit,
    maxHeightFraction: Float,
    applyImePadding: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(14.dp),
    fixedFooter: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    var visible by remember { mutableStateOf(!motionEnabled) }
    var dismissing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrimInteraction = remember { MutableInteractionSource() }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * maxHeightFraction.coerceIn(0.30f, 0.80f)
    // Guaranteed gap between the sheet's bottom content and the system gesture bar. Honouring the
    // real inset is right on 3-button devices (~48dp), but gesture nav reports a much thinner bar,
    // which alone still leaves the action row visually fused to the screen edge — hence the floor.
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomClearance = maxOf(navigationBarBottom, 34.dp)
    val systemBars = WindowInsets.systemBars.asPaddingValues()

    fun dismiss() {
        if (dismissing) return
        dismissing = true
        if (!motionEnabled) {
            onDismiss()
        } else {
            visible = false
            scope.launch {
                delay(240)
                onDismiss()
            }
        }
    }

    LaunchedEffect(Unit) { visible = true }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.68f else 0f,
        animationSpec = tween(if (motionEnabled) 260 else 0),
        label = "auronCompactSheetScrim",
    )
    val enter = if (motionEnabled) {
        slideInVertically(
            initialOffsetY = { height -> height },
            animationSpec = spring(dampingRatio = 0.90f, stiffness = 245f),
        ) + fadeIn(tween(220))
    } else {
        EnterTransition.None
    }
    val exit = if (motionEnabled) {
        slideOutVertically(
            targetOffsetY = { height -> height },
            animationSpec = tween(230),
        ) + fadeOut(tween(180))
    } else {
        ExitTransition.None
    }

    Dialog(
        onDismissRequest = ::dismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // MUST stay false. With `true` the dialog decor consumes the window insets, so any
            // navigation-bar inset read below resolves to 0 and the pinned footer ends up clipped
            // by the system gesture bar. Raising the sheet height or the button height cannot fix
            // that, which is why the action row's visibility had to be solved here. Edge-to-edge
            // lets the real insets reach the content. The scrim is full-screen either way, so the
            // only visual difference is the extra bottom clearance.
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // The dialog is edge-to-edge so its content can read real insets, but the dim
                    // is kept out of the system-bar strips: a 0.68 black scrim over the status /
                    // nav icons would leave dark-on-dark in light theme (the activity sets
                    // isAppearanceLightStatusBars = !darkTheme). This also matches how the scrim
                    // looked while the window itself was inset.
                    .padding(
                        top = systemBars.calculateTopPadding(),
                        bottom = systemBars.calculateBottomPadding(),
                    )
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        onClick = ::dismiss,
                    ),
            )
            AnimatedVisibility(
                visible = visible,
                enter = enter,
                exit = exit,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                val shape = RoundedCornerShape(28.dp)
                val container = if (isAuronDarkTheme()) Color(0xFF050609) else MaterialTheme.colorScheme.surface
                val surfaceModifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .then(if (applyImePadding) Modifier.imePadding() else Modifier)
                    // The lift is applied BEFORE the height / clip / background chain, so the visible
                    // card itself clears the gesture bar. Padding after `clip` would only inset the
                    // content and leave the card's own bottom edge sitting in the gesture zone.
                    .padding(bottom = 8.dp + bottomClearance)
                    .heightIn(max = maxHeight)
                    .clip(shape)
                    .background(container)
                    .border(gymContainerBorder(alpha = 0.12f), shape)

                if (fixedFooter == null) {
                    Column(
                        modifier = surfaceModifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            // 56dp was tuned while navigationBarsPadding() silently resolved to 0;
                            // the inset is now real, so 40dp keeps the trailing gap comparable.
                            .padding(top = 14.dp, bottom = 40.dp),
                        verticalArrangement = verticalArrangement,
                    ) {
                        AuronSheetHandle()
                        content()
                    }
                } else {
                    // Tall information sheets keep their primary exit/action outside the scroll
                    // region. This avoids a modal that looks complete but hides its Close action.
                    Column(
                        modifier = surfaceModifier
                            .height(maxHeight),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp)
                                .padding(top = 14.dp, bottom = 18.dp),
                            verticalArrangement = verticalArrangement,
                        ) {
                            AuronSheetHandle()
                            content()
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(container)
                                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp),
                        ) {
                            fixedFooter()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.AuronSheetHandle() {
    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(bottom = 4.dp)
            .size(width = 44.dp, height = 5.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f)),
    )
}
