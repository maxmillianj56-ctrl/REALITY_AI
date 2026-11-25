package com.realityai.companion.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.realityai.companion.domain.model.ThermalLevel
import com.realityai.companion.ui.MainUiState
import com.realityai.companion.ui.components.DashboardPanel
import com.realityai.companion.ui.components.MetricStatBlock
import com.realityai.companion.ui.components.RealityLogoEmblem
import com.realityai.companion.ui.theme.AmberWarning
import com.realityai.companion.ui.theme.CrimsonAlert
import com.realityai.companion.ui.theme.CyberCyan
import com.realityai.companion.ui.theme.HairlineBorder
import com.realityai.companion.ui.theme.NeonGreen
import com.realityai.companion.ui.theme.ObsidianBg
import com.realityai.companion.ui.theme.TextPrimary
import com.realityai.companion.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    state: MainUiState,
    onRequestStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onNavigateToGames: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToVisualAnalysis: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onAcceptDetectedGame: () -> Unit,
    onDismissDetectedGame: () -> Unit
) {
    val telemetry = state.telemetry
    val capture = state.capture

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RealityLogoEmblem(size = 40.dp)
                    Column {
                        Text(
                            text = "REALITY AI",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = if (capture.isCapturing) {
                                "MediaProjection Active · ${capture.activeWidth}x${capture.activeHeight}"
                            } else {
                                "Offline Companion Ready · ${telemetry.deviceModel}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (capture.isCapturing) NeonGreen else TextSecondary
                        )
                    }
                }
            }
        }

        if (state.detectedForegroundGame != null) {
            item {
                val detected = state.detectedForegroundGame
                DashboardPanel(borderColor = CyberCyan) {
                    Text(
                        text = "Foreground Game Detected: ${detected.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan
                    )
                    Text(
                        text = "Apply your saved ${detected.savedProfile.displayName} profile? Screen capture remains off unless you start it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onAcceptDetectedGame,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Apply Profile")
                        }
                        OutlinedButton(
                            onClick = onDismissDetectedGame,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        item {
            DashboardPanel(borderColor = if (capture.isCapturing) NeonGreen else HairlineBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Selected Game",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = state.persisted.selectedGameName ?: "No game selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateToGames,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (state.persisted.selectedGamePackage == null) "Select Game" else "Change")
                    }
                }

                HorizontalDivider(color = HairlineBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = state.persisted.activeProfile.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen
                        )
                        Text(
                            text = state.persisted.activeProfile.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateToProfiles,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Profiles")
                    }
                }
            }
        }

        item {
            DashboardPanel {
                Text(
                    text = "Device & Preview Telemetry",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatBlock(
                        modifier = Modifier.weight(1f),
                        label = "Battery Level",
                        value = telemetry.batteryPercent?.let { "$it%" } ?: "Unavailable",
                        accentColor = NeonGreen,
                        subtext = if (telemetry.isCharging) "Charging" else "Discharging"
                    )
                    MetricStatBlock(
                        modifier = Modifier.weight(1f),
                        label = "Battery Temp",
                        value = telemetry.batteryTempCelsius?.let {
                            String.format(Locale.US, "%.1f °C", it)
                        } ?: "Unavailable",
                        accentColor = when {
                            (telemetry.batteryTempCelsius ?: 0f) >= 42f -> CrimsonAlert
                            (telemetry.batteryTempCelsius ?: 0f) >= 39f -> AmberWarning
                            else -> CyberCyan
                        },
                        subtext = telemetry.thermalLevel.label
                    )
                }

                HorizontalDivider(color = HairlineBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val fpsValue = if (capture.isCapturing && telemetry.measuredPreviewFps != null) {
                        String.format(Locale.US, "%.1f FPS", telemetry.measuredPreviewFps)
                    } else if (capture.isCapturing) {
                        "Measuring..."
                    } else {
                        "Unavailable"
                    }
                    MetricStatBlock(
                        modifier = Modifier.weight(1f),
                        label = "Preview Capture FPS",
                        value = fpsValue,
                        accentColor = if (capture.isCapturing) NeonGreen else TextSecondary,
                        subtext = "Target cap: ${state.effectiveBudget.targetFpsCap} FPS"
                    )
                    val memLabel = if (telemetry.availableMemoryMb != null && telemetry.totalMemoryMb != null) {
                        "${telemetry.availableMemoryMb} MB free"
                    } else {
                        "Unavailable"
                    }
                    MetricStatBlock(
                        modifier = Modifier.weight(1f),
                        label = "Available RAM",
                        value = memLabel,
                        accentColor = TextPrimary,
                        subtext = telemetry.totalMemoryMb?.let { "of ${it} MB total" }
                    )
                }

                if (telemetry.thermalDownscaleReason != null) {
                    Text(
                        text = telemetry.thermalDownscaleReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning
                    )
                }

                Text(
                    text = "FPS reflects MediaProjection capture frames only when active. Android apps cannot read or override another game's internal render FPS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        item {
            DashboardPanel {
                Text(
                    text = "Screen Capture Control",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = if (capture.isCapturing) {
                        "Capture is running with a persistent notification. You can inspect enhanced frames in Visual Analysis or stop immediately."
                    } else {
                        "Start user-authorized MediaProjection capture (${state.persisted.settings.captureResolution.label}) to feed Visual Analysis and the on-demand AI Assistant."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                AnimatedVisibility(visible = capture.lastError != null) {
                    Text(
                        text = capture.lastError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonAlert
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!capture.isCapturing) {
                        Button(
                            onClick = onRequestStartCapture,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = ObsidianBg
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Start Capture", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onStopCapture,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonAlert,
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Stop Capture", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToVisualAnalysis,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Visual Preview")
                    }
                    OutlinedButton(
                        onClick = onNavigateToAssistant,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("AI Assistant")
                    }
                }
            }
        }
    }
}
