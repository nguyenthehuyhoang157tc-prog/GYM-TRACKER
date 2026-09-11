package com.hoctin.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoctin.gymtracker.ui.theme.GymTrackerTheme

/**
 * Local privacy rationale opened from Health Connect's permission-usage surface.
 *
 * WEARABLE-2A never uploads health records and never uses them in readiness. This Activity is kept
 * deliberately independent from the app navigation graph so Health Connect can open it safely even
 * when AURON has no active task stack.
 */
class HealthConnectRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Health Connect shadow context",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "AURON can optionally read sleep sessions, HRV RMSSD and resting " +
                                "heart-rate records. Imported records stay in AURON's private local " +
                                "database and are not sent over the network.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "This information is shadow research context only. It does not " +
                                "change readiness, Muscle Heat, Progressive Overload, exercise " +
                                "recommendations or workout targets.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "You can stop synchronization, manage Health Connect access or " +
                                "delete AURON's imported health context from Profile > Recovery Research.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = ::finish) { Text("Close") }
                    }
                }
            }
        }
    }
}
