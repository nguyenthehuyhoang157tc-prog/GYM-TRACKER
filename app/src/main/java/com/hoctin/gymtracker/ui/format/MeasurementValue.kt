package com.hoctin.gymtracker.ui.format

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color

/**
 * DB-6C unified inline value+unit renderer — the ONE place units are styled. The numeric value renders
 * in [valueStyle]; the [Measurement.unit] renders **bold** with slightly wider letter spacing right
 * beside it (never a detached column). Used identically by the Log set rows, History, and any future
 * metric (km/h, m/s …), so the inline design is consistent everywhere and a new unit needs no new UI.
 */
@Composable
fun MeasurementValue(
    measurement: Measurement,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalContentColor.current,
    textAlign: TextAlign = TextAlign.End,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (textAlign == TextAlign.End) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text = measurement.value, style = valueStyle, color = color)
        if (measurement.unit.isNotEmpty()) {
            Text(
                text = measurement.unit,
                modifier = Modifier.padding(start = 3.dp),
                style = valueStyle.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
                color = color,
            )
        }
    }
}
