package com.realityai.companion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.realityai.companion.domain.model.CaptureResolution
import com.realityai.companion.domain.model.ProcessingQuality
import com.realityai.companion.ui.MainUiState
import com.realityai.companion.ui.components.DashboardPanel
import com.realityai.companion.ui.theme.CrimsonAlert
import com.realityai.companion.ui.theme.CyberCyan
import com.realityai.companion.ui.theme.ElevatedCharcoal
import com.realityai.companion.ui.theme.HairlineBorder
import com.realityai.companion.ui.theme.NeonGreen
import com.realityai.companion.ui.theme.ObsidianBg
import com.realityai.companion.ui.theme.TextPrimary
import com.realityai.companion.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    state: MainUiState,
    onSelectResolution: (CaptureResolution) -> Unit,
    onSelectQuality: (ProcessingQuality) -> Unit,
    onToggleBatterySaver: (Boolean) -> Unit,
    onToggleAutoDetect: (Boolean) -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onRemoveSavedProfile: (String) -> Unit
) {
    val settings = state.persisted.settings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Settings & Privacy",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Text(
                text = "100% offline-first storage via DataStore. No network access, no hidden capture, no root APIs.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        item {
            DashboardPanel {
                Text(
                    text = "Capture Resolution",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CaptureResolution.entries.forEach { res ->
                        val selected = settings.captureResolution == res
                        OutlinedButton(
                            onClick = { onSelectResolution(res) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) ElevatedCharcoal else ObsidianBg,
                                contentColor = if (selected) NeonGreen else TextSecondary
                            )
                        ) {
                            Text(res.label.substringBefore(" "), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Text(
                    text = "Applies on next MediaProjection capture start.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                HorizontalDivider(color = HairlineBorder)

                Text(
                    text = "Frame Processing Quality",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProcessingQuality.entries.forEach { q ->
                        val selected = settings.processingQuality == q
                        OutlinedButton(
                            onClick = { onSelectQuality(q) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) ElevatedCharcoal else ObsidianBg,
                                contentColor = if (selected) CyberCyan else TextSecondary
                            )
                        ) {
                            Text(q.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Text(
                    text = settings.processingQuality.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
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
                            text = "Battery-Saving Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Locks preview processing to Low quality and 15 FPS cap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = settings.batterySavingMode,
                        onCheckedChange = onToggleBatterySaver,
                        colors = SwitchDefaults.colors(checkedTrackColor = NeonGreen)
                    )
                }

                HorizontalDivider(color = HairlineBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Game Detection",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Offers your saved profile when a known game moves to foreground. Never starts screen capture automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = settings.autoDetectGames,
                        onCheckedChange = onToggleAutoDetect,
                        colors = SwitchDefaults.colors(checkedTrackColor = CyberCyan)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.hasUsageAccessPermission) {
                            "Usage Access: Granted"
                        } else {
                            "Usage Access: Not Granted (Required for auto-detect)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.hasUsageAccessPermission) NeonGreen else TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onOpenUsageAccessSettings,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Configure")
                    }
                }
            }
        }

        item {
            DashboardPanel {
                Text(
                    text = "Saved Game Profiles (${state.persisted.gameProfiles.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (state.persisted.gameProfiles.isEmpty()) {
                    Text(
                        text = "No game profiles saved yet. Assign profiles from the Games tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    state.persisted.gameProfiles.entries.forEach { (pkg, profile) ->
                        val gameName = state.installedGames
                            .firstOrNull { it.packageName == pkg }?.displayName ?: pkg
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(gameName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text(profile.displayName, style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                            }
                            IconButton(onClick = { onRemoveSavedProfile(pkg) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove profile",
                                    tint = CrimsonAlert
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            DashboardPanel {
                Text(
                    text = "Privacy & Permissions Transparency",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "• Screen Capture: Uses Android MediaProjection with explicit system consent every session and a persistent notification with an instant Stop button.\n• Network: No INTERNET permission declared; all screenshot analysis runs locally on-device.\n• System Integrity: Zero root, zero hidden APIs, and zero claims of modifying third-party game rendering.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
