package com.ironsidemedical.connect.presentation.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ironsidemedical.connect.domain.model.VitalSigns
import com.ironsidemedical.connect.presentation.theme.AlertRedColor
import com.ironsidemedical.connect.presentation.theme.SafeGreenColor
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalHistoryScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: VitalHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportSession(sessionId) }) {
                        Icon(Icons.Default.Share, "Export")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SessionSummaryCard(
                    recordCount = uiState.vitals.size,
                    alertCount = uiState.vitals.count { it.hasAnyAlert },
                )
            }

            items(uiState.vitals.reversed(), key = { it.id }) { reading ->
                VitalHistoryRow(reading)
            }
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    .withZone(ZoneId.systemDefault())

@Composable
private fun VitalHistoryRow(v: VitalSigns) {
    val color = if (v.hasAnyAlert) AlertRedColor.copy(alpha = 0.08f)
    else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    timeFormatter.format(v.timestamp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Text(
                    "HR ${v.heartRateBpm.toInt()} · SpO₂ ${v.spo2Percent.toInt()}% · " +
                        "${v.systolicMmHg.toInt()}/${v.diastolicMmHg.toInt()} mmHg · ${v.temperatureCelsius}°C",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (v.hasAnyAlert) {
                Icon(Icons.Default.Warning, contentDescription = "Alert", tint = AlertRedColor)
            }
        }
    }
}

@Composable
private fun SessionSummaryCard(recordCount: Int, alertCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$recordCount", style = MaterialTheme.typography.headlineLarge)
                Text("Readings", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$alertCount",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (alertCount > 0) AlertRedColor else SafeGreenColor,
                )
                Text("Alerts", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}
