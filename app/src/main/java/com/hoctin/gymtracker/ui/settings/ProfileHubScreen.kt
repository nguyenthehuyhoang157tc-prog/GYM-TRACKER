package com.hoctin.gymtracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoctin.gymtracker.ui.BottomNavSpace
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import com.hoctin.gymtracker.ui.theme.auronScreenTopPadding

@Composable
fun ProfileHubScreen(
    displayName: String?,
    onOpen: (ProfileDestination) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .auronScreenBackground {
                listState.firstVisibleItemIndex * 180f + listState.firstVisibleItemScrollOffset.toFloat()
            },
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = auronScreenTopPadding(18.dp),
            bottom = BottomNavSpace + 18.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = "Profile & Settings",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = displayName?.takeIf { it.isNotBlank() }?.let { "Welcome back, $it" }
                        ?: "Your training, preferences and local data",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        profileHubSections().forEach { section ->
            item(key = section.label) {
                ProfileSectionCard(label = section.label) {
                    section.items.forEach { item ->
                        ProfileNavigationRow(
                            title = item.title,
                            supportingText = item.supportingText,
                            icon = iconFor(item.destination),
                            status = item.status,
                            onClick = { onOpen(item.destination) },
                        )
                    }
                }
            }
        }
    }
}

private fun iconFor(destination: ProfileDestination): ImageVector = when (destination) {
    ProfileDestination.CONSISTENCY -> Icons.Outlined.FitnessCenter
    ProfileDestination.INSIGHTS -> Icons.Outlined.BarChart
    ProfileDestination.PLAN -> Icons.Outlined.CalendarMonth
    ProfileDestination.TRAINING -> Icons.Outlined.Tune
    ProfileDestination.PREFERENCES -> Icons.Outlined.Palette
    ProfileDestination.NOTIFICATIONS -> Icons.Outlined.Notifications
    ProfileDestination.PERSONAL_PROFILE -> Icons.Outlined.Person
    ProfileDestination.HISTORY -> Icons.Outlined.History
    ProfileDestination.DATA -> Icons.Outlined.Backup
    ProfileDestination.RECOVERY_RESEARCH -> Icons.Outlined.Science
    ProfileDestination.RESOURCES -> Icons.AutoMirrored.Outlined.HelpOutline
    ProfileDestination.PRIVACY -> Icons.Outlined.Shield
    ProfileDestination.ABOUT -> Icons.Outlined.Info
}
