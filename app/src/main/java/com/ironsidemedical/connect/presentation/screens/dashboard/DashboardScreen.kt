package com.ironsidemedical.connect.presentation.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ironsidemedical.connect.data.bluetooth.BleState
import com.ironsidemedical.connect.domain.model.VitalSigns
import com.ironsidemedical.connect.presentation.components.ConnectionStatusBadge
import com.ironsidemedical.connect.presentation.components.VitalCard
import com.ironsidemedical.connect.presentation.theme.AlertRedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToHistory: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAuditLog: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }

    val connectedDevice = (uiState.bleState as? BleState.Ready)?.device
    val isConnected = connectedDevice != null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("IronSide Connect", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Audit Log") },
                            onClick = { menuExpanded = false; onNavigateToAuditLog() }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = { menuExpanded = false; onNavigateToSettings() }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Connection status header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ConnectionStatusBadge(isConnected = isConnected, deviceName = connectedDevice?.displayName)
                    if (!isConnected) {
                        FilledTonalButton(onClick = onNavigateToScan) {
                            Text("Connect Device")
                        }
                    }
                }
            }

            // Alert banner
            if (uiState.activeAlerts.isNotEmpty()) {
                item {
                    AlertBanner(alerts = uiState.activeAlerts)
                }
            }

            // Vital sign cards grid
            if (uiState.latestVitals != null) {
                item {
                    VitalsGrid(vitals = uiState.latestVitals!!)
                }
                item {
                    FilledTonalButton(
                        onClick = { onNavigateToHistory(uiState.latestVitals!!.sessionId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("View Session History")
                    }
                }
            } else if (!isConnected) {
                item {
                    EmptyStateCard(onConnectClick = onNavigateToScan)
                }
            } else {
                item {
                    WaitingForDataCard()
                }
            }
        }
    }
}

@Composable
private fun VitalsGrid(vitals: VitalSigns) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VitalCard(
                label = "Heart Rate",
                value = vitals.heartRateBpm.toInt().toString(),
                unit = "bpm",
                icon = Icons.Default.Favorite,
                isAlert = vitals.isHeartRateAbnormal,
                modifier = Modifier.weight(1f),
            )
            VitalCard(
                label = "SpO₂",
                value = vitals.spo2Percent.toInt().toString(),
                unit = "%",
                icon = Icons.Default.WaterDrop,
                isAlert = vitals.isSpo2Low,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VitalCard(
                label = "Systolic BP",
                value = vitals.systolicMmHg.toInt().toString(),
                unit = "mmHg",
                icon = Icons.Default.MonitorHeart,
                isAlert = vitals.isBloodPressureAbnormal,
                modifier = Modifier.weight(1f),
            )
            VitalCard(
                label = "Temperature",
                value = "%.1f".format(vitals.temperatureCelsius),
                unit = "°C",
                icon = Icons.Default.DeviceThermostat,
                isAlert = vitals.isTempAbnormal,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AlertBanner(alerts: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AlertRedColor.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Clinical Alert",
                style = MaterialTheme.typography.titleLarge,
                color = AlertRedColor,
            )
            alerts.forEach { alert ->
                Text(
                    text = "• $alert",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AlertRedColor,
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(onConnectClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.MonitorHeart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(48.dp),
            )
            Text("No device connected", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Scan for a nearby IronSide device to begin monitoring.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            FilledTonalButton(onClick = onConnectClick) {
                Text("Scan for Devices")
            }
        }
    }
}

@Composable
private fun WaitingForDataCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Connected — waiting for first measurement…", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
