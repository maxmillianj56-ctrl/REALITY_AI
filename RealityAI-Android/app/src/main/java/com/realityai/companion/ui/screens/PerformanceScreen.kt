package com.realityai.companion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.realityai.companion.ui.MainUiState
import com.realityai.companion.ui.components.DashboardPanel
import com.realityai.companion.ui.components.MetricStatBlock
import com.realityai.companion.ui.theme.AmberWarning
import com.realityai.companion.ui.theme.CyberCyan
import com.realityai.companion.ui.theme.HairlineBorder
import com.realityai.companion.ui.theme.NeonGreen
import com.realityai.companion.ui.theme.TextPrimary
import com.realityai.companion.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun PerformanceScreen(
    state: MainUiState,
    onToggleThermalDownscale: (Boolean) -> Unit
) {
    val telemetry = state.telemetry
    val budget = state.effectiveBudget

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Performance & Thermal Guard",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Text(
                text = "Monitors Android BatteryManager, PowerManager thermal status, and ActivityManager memory. Caps REALITY AI preview processing at up to 30 FPS and scales down automatically when hot.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        item {
            DashboardPanel {
                Text(
                    text = "Device Hardware Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = telemetry.deviceModel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberCyan
                )
                Text(
                    text = if (telemetry.isNote20UltraTarget) {
                        "Verified Galaxy Note 20 Ultra profile (Snapdragon 865+ / Adreno 650 tuning active)"
                    } else {
                        "Hardware: ${telemetry.hardwareBoard} · ${telemetry.androidVersion} (Optimized baseline: SM-N986U1 Snapdragon 865+)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        item {
            DashboardPanel {
                Text(
                    text = "Live Memory & Thermal Telemetry",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatBlock(
                        modifier = Modifier.weight(1f),
                        label = "Available RAM",
                        value = telemetry.availableMemoryMb?.let { "$it MB" } ?: "Unavailable",
                        subtext = telemetry.totalMemoryMb?.let { "Total: $it MB" }
                    )
                    MetricStatBlock(
                        modifier = Modifier.weight(1f),
                        label = "Thermal Status",
                        value = telemetry.thermalLevel.label,
                        accentColor = if (telemetry.thermalLevel.isThrottled) AmberWarning else NeonGreen,
                        subtext = telemetry.batteryTempCelsius?.let {
                            String.format(Locale.US, "Battery: %.1f °C", it)
                        }
                    )
                }

                HorizontalDivider(color = HairlineBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatBlock(
                        modifier = Modifier.weight(1f),
                        label = "Preview FPS Target",
                        value = "${budget.targetFpsCap} FPS",
                        accentColor = NeonGreen,
                        subtext = if (budget.isDownscaled) "Downscaled from 30 FPS" else "Nominal 30 FPS cap"
                    )
                    MetricStatBlock(
                        modifier = Modifier.weight(1f),
                        label = "Effective Quality",
                        value = budget.processingQuality.label,
                        accentColor = CyberCyan,
                        subtext = "Configured: ${state.persisted.settings.processingQuality.label}"
                    )
                }

                if (budget.reason != null) {
                    Text(
                        text = budget.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning
                    )
                }
            }
        }

        item {
            DashboardPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Thermal Downscaling",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Automatically reduce preview FPS cap (to 15–20 FPS) and image-processing quality when PowerManager reports Moderate/Severe heat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = state.persisted.settings.thermalAutoDownscale,
                        onCheckedChange = onToggleThermalDownscale,
                        colors = SwitchDefaults.colors(checkedTrackColor = NeonGreen)
                    )
                }

                HorizontalDivider(color = HairlineBorder)

                Text(
                    text = "Android Platform Boundary: REALITY AI strictly limits its own capture and preview pipeline. Standard Android apps cannot force third-party games to run at 30 FPS or alter external game frame pacing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
