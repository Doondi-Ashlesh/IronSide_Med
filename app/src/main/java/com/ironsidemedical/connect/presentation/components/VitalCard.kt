package com.ironsidemedical.connect.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ironsidemedical.connect.presentation.theme.AlertRedColor
import com.ironsidemedical.connect.presentation.theme.SafeGreenColor

/**
 * Displays a single vital-sign reading with alert-state colour animation.
 *
 * Uses a smooth color transition (300 ms) to draw attention to out-of-range
 * values without the jarring flash of an instant color change - a deliberate
 * UX choice to reduce alarm fatigue while still surfacing clinical alerts.
 */
@Composable
fun VitalCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    isAlert: Boolean,
    modifier: Modifier = Modifier,
) {
    val cardColor by animateColorAsState(
        targetValue = if (isAlert) AlertRedColor.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "cardColor",
    )
    val valueColor by animateColorAsState(
        targetValue = if (isAlert) AlertRedColor else SafeGreenColor,
        animationSpec = tween(durationMillis = 300),
        label = "valueColor",
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                if (isAlert) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = AlertRedColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium.copy(color = valueColor, fontWeight = FontWeight.Bold),
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
fun ConnectionStatusBadge(isConnected: Boolean, deviceName: String?) {
    val color = if (isConnected) SafeGreenColor else MaterialTheme.colorScheme.error
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Text(
            text = if (isConnected && deviceName != null) deviceName else "No Device",
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}
