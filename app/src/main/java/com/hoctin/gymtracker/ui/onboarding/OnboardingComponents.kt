package com.hoctin.gymtracker.ui.onboarding

import android.os.SystemClock
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
internal fun OnboardingTopBar(
    step: OnboardingStepV3,
    progress: Float,
    motionEnabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = onboardingPalette()
    val activeStep = normalizeActiveOnboardingStep(step)
    val activeIndex = ACTIVE_ONBOARDING_STEPS.indexOf(activeStep).coerceAtLeast(0)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(if (motionEnabled) 260 else 0),
        label = "onboardingProgress",
    )
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (step != OnboardingStepV3.WELCOME) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.11f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            color = palette.selectedBorder,
            trackColor = Color.White.copy(alpha = 0.14f),
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .semantics {
                    stateDescription = "Step ${activeIndex + 1} of ${ACTIVE_ONBOARDING_STEPS.size}, ${activeStep.accessibleName}"
                },
        )
        Spacer(Modifier.width(12.dp))
    }
}

/**
 * A selective, image-led chapter header inspired by the approved Figma Make prototype.
 * The image is decorative; the heading remains the accessibility focus target.
 */
@Composable
internal fun OnboardingHeroQuestion(
    @DrawableRes imageRes: Int,
    title: String,
    support: String,
    eyebrow: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 172.dp,
) {
    val palette = onboardingPalette()
    val focusRequester = remember(title) { FocusRequester() }
    LaunchedEffect(title) { focusRequester.requestFocus() }
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(shape)
            .background(palette.panelFill)
            .border(1.dp, Color.White.copy(alpha = 0.13f), shape),
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xF20A0710),
                        0.58f to Color(0xB80A0710),
                        1f to Color(0x250A0710),
                    ),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xD9000000)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.88f)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = eyebrow.uppercase(),
                color = palette.selectedBorder,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.35.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.34f))
                    .border(1.dp, palette.selectedBorder.copy(alpha = 0.28f), RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = 27.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.45).sp,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .semantics { heading() },
            )
            Text(
                text = support,
                color = Color(0xFFE0D8E5),
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun OnboardingQuestion(
    title: String,
    support: String,
    eyebrow: String? = null,
) {
    val palette = onboardingPalette()
    val focusRequester = remember(title) { FocusRequester() }
    LaunchedEffect(title) { focusRequester.requestFocus() }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val metrics = onboardingQuestionMetrics(
            availableWidthDp = maxWidth.value,
            fontScale = LocalConfiguration.current.fontScale,
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    color = palette.selectedBorder,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                )
            }
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = metrics.titleSp.sp,
                lineHeight = metrics.titleLineHeightSp.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.7).sp,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .semantics { heading() },
            )
            Text(
                text = support,
                color = palette.textSecondary,
                fontSize = metrics.supportSp.sp,
                lineHeight = metrics.supportLineHeightSp.sp,
            )
        }
    }
}

internal data class OnboardingQuestionMetrics(
    val titleSp: Float,
    val titleLineHeightSp: Float,
    val supportSp: Float,
    val supportLineHeightSp: Float,
)

/**
 * Scales the nominal type tier from the width that remains after page padding. System font
 * scaling still applies on top, while the compact tier prevents ordinary titles from wrapping
 * into narrow, visually orphaned lines on smaller phones.
 */
internal fun onboardingQuestionMetrics(
    availableWidthDp: Float,
    fontScale: Float,
): OnboardingQuestionMetrics {
    val base = when {
        availableWidthDp < 285f -> OnboardingQuestionMetrics(27f, 32f, 15f, 22f)
        availableWidthDp < 330f -> OnboardingQuestionMetrics(30f, 35f, 16f, 23f)
        else -> OnboardingQuestionMetrics(34f, 39f, 17f, 25f)
    }
    return if (fontScale >= 1.6f) {
        base.copy(
            titleSp = (base.titleSp - 2f).coerceAtLeast(25f),
            titleLineHeightSp = (base.titleLineHeightSp - 2f).coerceAtLeast(30f),
        )
    } else {
        base
    }
}

@Composable
internal fun OnboardingStaggeredItem(
    index: Int,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var entered by remember(index, motionEnabled) { mutableStateOf(!motionEnabled) }
    LaunchedEffect(index, motionEnabled) {
        if (motionEnabled) {
            entered = false
            delay((index.coerceAtMost(8) * 34L))
            entered = true
        }
    }
    val itemAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(if (motionEnabled) 360 else 0),
        label = "onboardingItemAlpha",
    )
    val itemOffset by animateDpAsState(
        targetValue = if (entered) 0.dp else 10.dp,
        animationSpec = tween(if (motionEnabled) 390 else 0),
        label = "onboardingItemOffset",
    )
    Box(modifier = modifier.alpha(itemAlpha).offset(y = itemOffset)) {
        content()
    }
}

@Composable
internal fun OnboardingPrimaryButton(
    text: String,
    enabled: Boolean,
    motionEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArrow: Boolean = true,
) {
    RaisedActionButton(
        text = text,
        enabled = enabled,
        motionEnabled = motionEnabled,
        onClick = onClick,
        modifier = modifier,
        showArrow = showArrow,
        primary = true,
    )
}

@Composable
internal fun OnboardingSecondaryButton(
    text: String,
    motionEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    RaisedActionButton(
        text = text,
        enabled = enabled,
        motionEnabled = motionEnabled,
        onClick = onClick,
        modifier = modifier,
        showArrow = false,
        primary = false,
    )
}

@Composable
private fun RaisedActionButton(
    text: String,
    enabled: Boolean,
    motionEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    showArrow: Boolean,
    primary: Boolean,
) {
    val palette = onboardingPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val faceOffset by animateDpAsState(
        targetValue = if (motionEnabled && pressed && enabled) 4.dp else 0.dp,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.82f, stiffness = 760f) else tween(0),
        label = "onboardingButtonDepth",
    )
    val scale by animateFloatAsState(
        targetValue = if (motionEnabled && pressed && enabled) 0.985f else 1f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.86f, stiffness = 720f) else tween(0),
        label = "onboardingPrimaryPress",
    )
    val container = when {
        primary && enabled -> palette.primaryButton
        primary -> palette.primaryButton.copy(alpha = 0.48f)
        enabled -> Color(0xF2120E16)
        else -> Color(0x80120E16)
    }
    val content = when {
        primary && enabled -> palette.primaryButtonText
        primary -> palette.primaryButtonText.copy(alpha = 0.55f)
        enabled -> palette.textPrimary
        else -> palette.textSecondary.copy(alpha = 0.5f)
    }
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(x = 1.dp, y = 5.dp)
                .clip(shape)
                .background(Color(0xFF000000)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = faceOffset)
                .scale(scale)
                .heightIn(min = 60.dp)
                .clip(shape)
                .background(container)
                .background(if (pressed && enabled) Color.Black.copy(alpha = 0.14f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (primary) Color.White.copy(alpha = 0.68f) else Color.White.copy(alpha = 0.19f),
                    shape = shape,
                )
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 22.dp, vertical = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = content,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            if (showArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.align(Alignment.CenterEnd).size(26.dp),
                )
            }
        }
    }
}

@Composable
internal fun OnboardingTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = onboardingPalette().textSecondary,
) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text = text, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun OnboardingInfoIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val palette = onboardingPalette()
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.34f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Info, contentDescription, tint = palette.textSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun OnboardingChoiceRow(
    title: String,
    description: String,
    selected: Boolean,
    motionEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    recommended: Boolean = false,
) {
    val palette = onboardingPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val frontOffset by animateDpAsState(
        targetValue = if (selected || (motionEnabled && pressed)) 3.5.dp else 0.dp,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.82f, stiffness = 720f) else tween(0),
        label = "choiceDepth",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (motionEnabled && pressed) 0.985f else 1f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.88f, stiffness = 760f) else tween(0),
        label = "choicePress",
    )
    val fill by animateColorAsState(
        targetValue = if (selected) palette.selectedFill else palette.unselectedFill,
        animationSpec = tween(if (motionEnabled) 170 else 0),
        label = "choiceFill",
    )
    val outline by animateColorAsState(
        targetValue = if (selected) palette.selectedBorder.copy(alpha = 0.74f) else Color.White.copy(alpha = 0.19f),
        animationSpec = tween(if (motionEnabled) 170 else 0),
        label = "choiceOutline",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = modifier.fillMaxWidth().padding(bottom = 5.dp)) {
        Box(
            Modifier
                .matchParentSize()
                .offset(x = 1.dp, y = 5.dp)
                .clip(shape)
                .background(Color.Black.copy(alpha = 0.98f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = frontOffset)
                .scale(pressScale)
                .clip(shape)
                .background(fill)
                .background(if (pressed) Color.Black.copy(alpha = 0.14f) else Color.Transparent)
                .border(1.dp, outline, shape)
                .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 17.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) palette.selectedBorder.copy(alpha = 0.11f) else Color.Black.copy(alpha = 0.28f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = icon,
                        color = if (selected) palette.selectedBorder else palette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (recommended) {
                        Text(
                            text = "RECOMMENDED",
                            color = palette.selectedBorder,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp,
                        )
                    }
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) palette.selectedBorder else Color.Transparent)
                    .border(1.dp, if (selected) palette.selectedBorder else Color.White.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = Color(0xFF18071D), modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
internal fun OnboardingBinaryChoiceGrid(
    selected: GenderIdentity?,
    motionEnabled: Boolean,
    onSelect: (GenderIdentity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val choices = listOf(GenderIdentity.MAN to "♂", GenderIdentity.WOMAN to "♀")
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val oneColumn = binaryChoicesUseSingleColumn(
            availableWidthDp = maxWidth.value,
            fontScale = LocalConfiguration.current.fontScale,
        )
        if (oneColumn) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                choices.forEachIndexed { index, (value, icon) ->
                    OnboardingStaggeredItem(index, motionEnabled, Modifier.fillMaxWidth()) {
                        BinaryChoiceCard(
                            value = value,
                            icon = icon,
                            selected = selected == value,
                            motionEnabled = motionEnabled,
                            onClick = { onSelect(value) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                choices.forEachIndexed { index, (value, icon) ->
                    OnboardingStaggeredItem(index, motionEnabled, Modifier.weight(1f)) {
                        BinaryChoiceCard(
                            value = value,
                            icon = icon,
                            selected = selected == value,
                            motionEnabled = motionEnabled,
                            onClick = { onSelect(value) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Keeps the commercial two-card composition on normal phones. [availableWidthDp] is the
 * post-page-padding width, so a 350dp threshold incorrectly classified common 360dp devices
 * (about 312dp of content width) as compact. The stacked fallback is reserved for genuinely
 * narrow content or accessibility-sized text.
 */
internal fun binaryChoicesUseSingleColumn(
    availableWidthDp: Float,
    fontScale: Float,
): Boolean = availableWidthDp < 270f || fontScale >= 1.6f

@Composable
private fun BinaryChoiceCard(
    value: GenderIdentity,
    icon: String,
    selected: Boolean,
    motionEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = onboardingPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val frontOffset by animateDpAsState(
        targetValue = if (selected || (motionEnabled && pressed)) 4.dp else 0.dp,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.82f, stiffness = 720f) else tween(0),
        label = "binaryChoiceDepth",
    )
    val scale by animateFloatAsState(
        targetValue = if (motionEnabled && pressed) 0.985f else 1f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.86f, stiffness = 760f) else tween(0),
        label = "binaryChoiceScale",
    )
    val shape = RoundedCornerShape(17.dp)
    Box(modifier = modifier.heightIn(min = 178.dp).padding(bottom = 6.dp)) {
        Box(Modifier.matchParentSize().offset(x = 1.dp, y = 6.dp).clip(shape).background(Color.Black))
        Column(
            modifier = Modifier
                .matchParentSize()
                .offset(y = frontOffset)
                .scale(scale)
                .clip(shape)
                .background(if (selected) Color(0xF0191220) else Color(0xEE0D0A10))
                .background(if (pressed) Color.Black.copy(alpha = 0.16f) else Color.Transparent)
                .border(1.25.dp, if (selected) palette.selectedBorder.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.18f), shape)
                .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(icon, color = if (selected) palette.selectedBorder else palette.textSecondary, fontSize = 58.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text(value.label, color = palette.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.size(23.dp).clip(CircleShape)
                    .background(if (selected) palette.selectedBorder else Color.Transparent)
                    .border(1.dp, if (selected) palette.selectedBorder else Color.White.copy(alpha = 0.26f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = Color(0xFF16091A), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
internal fun OnboardingToggleRow(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    motionEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = onboardingPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val frontOffset by animateDpAsState(
        targetValue = if (selected || (motionEnabled && pressed)) 3.5.dp else 0.dp,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.82f, stiffness = 720f) else tween(0),
        label = "toggleRowDepth",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = modifier.fillMaxWidth().padding(bottom = 5.dp).alpha(if (enabled || selected) 1f else 0.52f)) {
        Box(Modifier.matchParentSize().offset(x = 1.dp, y = 5.dp).clip(shape).background(Color.Black))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = frontOffset)
                .clip(shape)
                .background(if (selected) Color(0xF0191220) else Color(0xEE0D0A10))
                .background(if (pressed) Color.Black.copy(alpha = 0.14f) else Color.Transparent)
                .border(1.dp, if (selected) palette.selectedBorder.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.18f), shape)
                .semantics { stateDescription = if (selected) "Selected" else if (enabled) "Not selected" else "Selection limit reached" }
                .toggleable(
                    value = selected,
                    enabled = enabled || selected,
                    role = Role.Checkbox,
                    interactionSource = interactionSource,
                    indication = null,
                    onValueChange = { onClick() },
                )
                .padding(horizontal = 19.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = palette.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Box(
                Modifier.size(24.dp).clip(RoundedCornerShape(7.dp))
                    .background(if (selected) palette.selectedBorder else Color.Transparent)
                    .border(1.dp, if (selected) palette.selectedBorder else Color.White.copy(alpha = 0.26f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = Color(0xFF16091A), modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
internal fun OnboardingMultiChoiceGrid(
    items: List<Triple<String, String, Boolean>>,
    motionEnabled: Boolean,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalConfiguration.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val useOneColumn = multiChoiceUsesSingleColumn(maxWidth.value, fontScale)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (useOneColumn) {
                items.forEachIndexed { index, item ->
                    OnboardingStaggeredItem(index, motionEnabled, Modifier.fillMaxWidth()) {
                        OnboardingMultiChoiceTile(
                            title = item.first,
                            icon = item.second,
                            selected = item.third,
                            motionEnabled = motionEnabled,
                            onClick = { onToggle(index) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                items.chunked(2).forEachIndexed { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowItems.forEachIndexed { columnIndex, item ->
                            val index = rowIndex * 2 + columnIndex
                            OnboardingStaggeredItem(index, motionEnabled, Modifier.weight(1f)) {
                                OnboardingMultiChoiceTile(
                                    title = item.first,
                                    icon = item.second,
                                    selected = item.third,
                                    motionEnabled = motionEnabled,
                                    onClick = { onToggle(index) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

internal fun multiChoiceUsesSingleColumn(
    availableWidthDp: Float,
    fontScale: Float,
): Boolean = availableWidthDp < 285f || fontScale >= 1.5f

@Composable
private fun OnboardingMultiChoiceTile(
    title: String,
    icon: String,
    selected: Boolean,
    motionEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = onboardingPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val frontOffset by animateDpAsState(
        targetValue = if (selected || (motionEnabled && pressed)) 4.dp else 0.dp,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.84f, stiffness = 720f) else tween(0),
        label = "multiChoiceDepth",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (motionEnabled && pressed) 0.985f else 1f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.84f, stiffness = 720f) else tween(0),
        label = "multiChoicePress",
    )
    val fill by animateColorAsState(
        targetValue = if (selected) palette.selectedFill else palette.unselectedFill,
        animationSpec = tween(if (motionEnabled) 170 else 0),
        label = "multiChoiceFill",
    )
    val outline by animateColorAsState(
        targetValue = if (selected) palette.selectedBorder.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.13f),
        animationSpec = tween(if (motionEnabled) 170 else 0),
        label = "multiChoiceOutline",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = modifier.heightIn(min = 98.dp).padding(bottom = 6.dp)) {
        Box(Modifier.matchParentSize().offset(x = 1.dp, y = 6.dp).clip(shape).background(Color.Black))
        Column(
            modifier = Modifier
                .matchParentSize()
                .offset(y = frontOffset)
                .scale(pressScale)
                .clip(shape)
                .background(fill)
                .background(if (pressed) Color.Black.copy(alpha = 0.15f) else Color.Transparent)
                .border(1.dp, outline, shape)
                .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
                .toggleable(
                    value = selected,
                    role = Role.Checkbox,
                    interactionSource = interactionSource,
                    indication = null,
                    onValueChange = { onClick() },
                )
                .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (selected) palette.selectedBorder.copy(alpha = 0.14f)
                        else Color.White.copy(alpha = 0.055f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = icon,
                    color = if (selected) palette.selectedBorder else palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Box(
                modifier = Modifier
                    .size(23.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) palette.selectedBorder else Color.Transparent)
                    .border(
                        1.dp,
                        if (selected) palette.selectedBorder else Color.White.copy(alpha = 0.26f),
                        RoundedCornerShape(7.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(Icons.Rounded.Check, null, tint = Color(0xFF1A0720), modifier = Modifier.size(17.dp))
                }
            }
        }
            Text(
                text = title,
                color = palette.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                lineHeight = 20.sp,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun OnboardingSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = onboardingPalette()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF28242D))
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Text(
                text = label,
                color = if (selected) Color.Black else palette.textSecondary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelected(index) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
internal fun OnboardingInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val palette = onboardingPalette()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        suffix = suffix?.let { { Text(it, fontWeight = FontWeight.Bold) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
            focusedBorderColor = palette.selectedBorder,
            unfocusedBorderColor = palette.hairline,
            focusedContainerColor = Color.Black.copy(alpha = 0.28f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.22f),
            focusedLabelColor = palette.selectedBorder,
            unfocusedLabelColor = palette.textSecondary,
            focusedPlaceholderColor = palette.textSecondary.copy(alpha = 0.65f),
            unfocusedPlaceholderColor = palette.textSecondary.copy(alpha = 0.65f),
            cursorColor = palette.selectedBorder,
        ),
    )
}

@Composable
internal fun OnboardingInfoPanel(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
    accent: Color = onboardingPalette().selectedBorder,
) {
    val palette = onboardingPalette()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.panelFill)
            .border(1.dp, palette.hairline, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title.uppercase(),
            color = accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
        Text(text = content, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
    }
}

@Composable
internal fun OnboardingReceiptRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    val palette = onboardingPalette()
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label.uppercase(), color = palette.textSecondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.1.sp)
            Text(text = value, color = palette.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        action?.invoke(this)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun OnboardingRulerPicker(
    value: Double,
    range: ClosedFloatingPointRange<Double>,
    step: Double,
    valueLabel: (Double) -> String,
    semanticLabel: String,
    motionEnabled: Boolean,
    onValueChange: (Double) -> Boolean,
    onTick: () -> Unit,
    modifier: Modifier = Modifier,
    contextLabel: String? = null,
) {
    val palette = onboardingPalette()
    val count = remember(range, step) { OnboardingRulerMath.itemCount(range, step) }
    val snapped = OnboardingRulerMath.snap(value, range, step)
    val currentIndex = OnboardingRulerMath.indexFor(snapped, range, step)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    val snapFling = rememberSnapFlingBehavior(listState)
    val reducedMotionFling = remember {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float = 0f
        }
    }
    var userInteracted by remember { mutableStateOf(false) }
    var lastTickAt by remember { mutableLongStateOf(0L) }
    val centeredIndex by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { item ->
                abs(item.offset + item.size / 2 - viewportCenter)
            }?.index ?: currentIndex
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userInteracted = true
        } else if (!motionEnabled && userInteracted) {
            listState.scrollToItem(centeredIndex)
        }
    }
    LaunchedEffect(centeredIndex, userInteracted) {
        if (!userInteracted) return@LaunchedEffect
        val next = OnboardingRulerMath.valueFor(centeredIndex, range, step)
        if (onValueChange(next)) {
            val now = SystemClock.uptimeMillis()
            if (now - lastTickAt >= 48L) {
                lastTickAt = now
                onTick()
            }
        }
    }
    LaunchedEffect(currentIndex, motionEnabled) {
        if (!listState.isScrollInProgress && centeredIndex != currentIndex) {
            if (motionEnabled) listState.animateScrollToItem(currentIndex) else listState.scrollToItem(currentIndex)
        }
    }

    val adjustBy: (Int) -> Boolean = { direction ->
        val nextIndex = (currentIndex + direction).coerceIn(0, count - 1)
        if (nextIndex == currentIndex) {
            false
        } else {
            val changed = onValueChange(OnboardingRulerMath.valueFor(nextIndex, range, step))
            if (changed) onTick()
            changed
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = semanticLabel
                stateDescription = valueLabel(snapped)
                progressBarRangeInfo = ProgressBarRangeInfo(
                    snapped.toFloat(),
                    range.start.toFloat()..range.endInclusive.toFloat(),
                )
                setProgress { requested ->
                    val next = OnboardingRulerMath.snap(requested.toDouble(), range, step)
                    val changed = onValueChange(next)
                    if (changed) onTick()
                    changed
                }
                customActions = listOf(
                    CustomAccessibilityAction("Increase $semanticLabel") { adjustBy(1) },
                    CustomAccessibilityAction("Decrease $semanticLabel") { adjustBy(-1) },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (contextLabel != null) {
            Text(
                text = contextLabel,
                color = palette.textSecondary,
                fontSize = 17.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = valueLabel(snapped),
            color = palette.textPrimary,
            fontSize = 48.sp,
            lineHeight = 53.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        val rulerShade by animateColorAsState(
            targetValue = if (listState.isScrollInProgress) Color.Black.copy(alpha = 0.20f) else Color.Transparent,
            animationSpec = tween(if (motionEnabled) 140 else 0),
            label = "rulerPressedShade",
        )
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(rulerShade),
        ) {
            val tickWidth = 18.dp
            val sidePadding = ((maxWidth - tickWidth) / 2).coerceAtLeast(0.dp)
            LazyRow(
                state = listState,
                flingBehavior = if (motionEnabled) snapFling else reducedMotionFling,
                contentPadding = PaddingValues(horizontal = sidePadding),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.matchParentSize(),
            ) {
                items((0 until count).toList(), key = { it }) { index ->
                    val major = index % 5 == 0
                    val isCenter = index == centeredIndex
                    Box(
                        modifier = Modifier.width(tickWidth).height(88.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .width(if (isCenter) 3.dp else if (major) 2.dp else 1.dp)
                                .height(if (isCenter) 82.dp else if (major) 47.dp else 27.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isCenter) palette.selectedBorder
                                    else palette.textSecondary.copy(alpha = if (major) 0.72f else 0.43f),
                                ),
                        )
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.Center)
                    .width(3.dp)
                    .height(98.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.selectedBorder.copy(alpha = 0.92f)),
            )
        }
        Text(
            text = if (motionEnabled) "Swipe and release to snap" else "Swipe or use accessibility actions to adjust",
            color = palette.textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun TrainingFrequencyChart(
    selectedDays: Int,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = onboardingPalette()
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(if (motionEnabled) 620 else 0),
        label = "trainingPrincipleChart",
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(230.dp)
                .semantics {
                    stateDescription = "Training-frequency illustration. Guideline region begins at two days per week. Your selection is $selectedDays days per week. Not a performance forecast."
                },
        ) {
            val left = 26.dp.toPx()
            val right = size.width - 18.dp.toPx()
            val top = 20.dp.toPx()
            val bottom = size.height - 34.dp.toPx()
            val plotWidth = right - left
            val xForDay: (Int) -> Float = { day -> left + ((day - 1) / 5f) * plotWidth }

            drawRoundRect(
                color = Color(0xFF74E6A8).copy(alpha = 0.12f),
                topLeft = Offset(xForDay(2), top),
                size = Size(right - xForDay(2), bottom - top),
                cornerRadius = CornerRadius(14.dp.toPx()),
            )
            drawLine(palette.hairline, Offset(left, bottom), Offset(right, bottom), 1.dp.toPx())

            val curve = Path().apply {
                moveTo(left, bottom - 20.dp.toPx())
                cubicTo(
                    xForDay(2), bottom - 72.dp.toPx(),
                    xForDay(3), bottom - 108.dp.toPx(),
                    right, bottom - 128.dp.toPx(),
                )
            }
            drawPath(
                path = curve,
                color = Color(0xFF74E6A8).copy(alpha = reveal),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
            val markerX = xForDay(selectedDays.coerceIn(1, 6))
            drawLine(
                color = Color(0xFFFF5964).copy(alpha = reveal),
                start = Offset(markerX, top + (1f - reveal) * (bottom - top)),
                end = Offset(markerX, bottom),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(Color(0xFFFF5964), 6.dp.toPx(), Offset(markerX, top + 24.dp.toPx()))
            (1..6).forEach { day ->
                drawContext.canvas.nativeCanvas.drawText(
                    day.toString(),
                    xForDay(day),
                    size.height - 8.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(190, 220, 210, 225)
                        textSize = 12.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1 day", color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
            Text("Guideline region: 2+", color = Color(0xFF74E6A8), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text("6 days", color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun ProfileCompletenessRing(
    completeness: ProfileCompleteness,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = onboardingPalette()
    val progress by animateFloatAsState(
        targetValue = completeness.percentage / 100f,
        animationSpec = tween(if (motionEnabled) 720 else 0),
        label = "profileCompleteness",
    )
    Box(modifier = modifier.size(210.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawArc(
                color = Color(0xFF3A3140),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(18.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = if (completeness.requiredReady) palette.selectedBorder else Color(0xFF8D7B96),
                startAngle = 135f,
                sweepAngle = 270f * progress,
                useCenter = false,
                style = Stroke(18.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${completeness.percentage}%",
                color = palette.textPrimary,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = if (completeness.requiredReady) "PROFILE READY" else "REQUIRED DETAILS",
                color = if (completeness.requiredReady) palette.selectedBorder else palette.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
    }
}
