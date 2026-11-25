package com.realityai.companion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.realityai.companion.domain.model.GraphicsProfileType
import com.realityai.companion.ui.MainUiState
import com.realityai.companion.ui.components.DashboardPanel
import com.realityai.companion.ui.theme.CyberCyan
import com.realityai.companion.ui.theme.HairlineBorder
import com.realityai.companion.ui.theme.NeonGreen
import com.realityai.companion.ui.theme.TextPrimary
import com.realityai.companion.ui.theme.TextSecondary

@Composable
fun GraphicsProfilesScreen(
    state: MainUiState,
    onSelectProfile: (GraphicsProfileType) -> Unit
) {
    val active = state.persisted.activeProfile

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Graphics Analysis Profiles",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Text(
                text = "Profiles configure MediaProjection capture resolution, preview filtering, and thermal cadence for ${state.persisted.selectedGameName ?: "your active session"}. They do not modify external game shaders or GPU pipelines.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        items(GraphicsProfileType.entries, key = { it.name }) { profile ->
            val isSelected = profile == active
            val adj = profile.defaultAdjustments

            DashboardPanel(
                modifier = Modifier.clickable { onSelectProfile(profile) },
                borderColor = if (isSelected) NeonGreen else HairlineBorder
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) NeonGreen else TextPrimary
                        )
                        Text(
                            text = profile.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberCyan
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectProfile(profile) },
                        colors = RadioButtonDefaults.colors(selectedColor = NeonGreen)
                    )
                }

                Text(
                    text = profile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                HorizontalDivider(color = HairlineBorder)

                Text(
                    text = "Preview Target: ${profile.recommendedResolution.label} · ${profile.targetPreviewFps} FPS cap · ${profile.recommendedQuality.label} processing",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Brightness ${String.format("%+.2f", adj.brightness)} · Contrast ${String.format("%.2f", adj.contrast)} · Sat ${String.format("%.2f", adj.saturation)} · Sharp ${String.format("%.2f", adj.sharpness)} · Gamma ${String.format("%.2f", adj.gamma)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
