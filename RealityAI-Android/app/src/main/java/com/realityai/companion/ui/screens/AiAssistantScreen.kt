package com.realityai.companion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.realityai.companion.ui.MainUiState
import com.realityai.companion.ui.components.DashboardPanel
import com.realityai.companion.ui.components.MetricStatBlock
import com.realityai.companion.ui.theme.CyberCyan
import com.realityai.companion.ui.theme.HairlineBorder
import com.realityai.companion.ui.theme.NeonGreen
import com.realityai.companion.ui.theme.ObsidianBg
import com.realityai.companion.ui.theme.TextPrimary
import com.realityai.companion.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AiAssistantScreen(
    state: MainUiState,
    onAnalyzeNow: () -> Unit,
    onApplyRecommendation: () -> Unit,
    onImportImage: () -> Unit
) {
    val report = state.latestSceneReport
    val hasFrame = state.capture.latestFrame != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "AI Gaming Assistant",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Text(
                text = "Runs strictly on-demand when you request an analysis of a captured or imported frame. Never analyzes frames continuously in the background.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        item {
            DashboardPanel {
                Text(
                    text = "On-Demand Scene Inspection",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = if (hasFrame) {
                        "Ready to inspect current frame (${state.capture.latestFrame!!.width}x${state.capture.latestFrame!!.height})."
                    } else {
                        "No frame loaded. Start MediaProjection capture or import a screenshot."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAnalyzeNow,
                        enabled = hasFrame && !state.isAnalyzingScene,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = ObsidianBg
                        )
                    ) {
                        if (state.isAnalyzingScene) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ObsidianBg,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Analyze Screenshot", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onImportImage,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Import")
                    }
                }
            }
        }

        if (report != null) {
            item {
                DashboardPanel(borderColor = CyberCyan) {
                    Text(
                        text = "Detected Environment",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = report.condition.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricStatBlock(
                            modifier = Modifier.weight(1f),
                            label = "Mean Luminance",
                            value = "${(report.meanLuminance * 100).toInt()}%"
                        )
                        MetricStatBlock(
                            modifier = Modifier.weight(1f),
                            label = "Shadows / Glare",
                            value = "${(report.shadowClipRatio * 100).toInt()}% / ${(report.highlightClipRatio * 100).toInt()}%"
                        )
                        MetricStatBlock(
                            modifier = Modifier.weight(1f),
                            label = "Edge Density",
                            value = "${(report.edgeDensity * 100).toInt()}%"
                        )
                    }

                    HorizontalDivider(color = HairlineBorder)

                    Text(
                        text = "Recommended Profile: ${report.recommendedProfile.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonGreen
                    )
                    Text(
                        text = report.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    val rec = report.recommendedAdjustments
                    Text(
                        text = String.format(
                            Locale.US,
                            "Brightness %+.2f · Contrast %.2f · Saturation %.2f · Sharpness %.2f · Gamma %.2f",
                            rec.brightness, rec.contrast, rec.saturation, rec.sharpness, rec.gamma
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    Button(
                        onClick = onApplyRecommendation,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = ObsidianBg
                        )
                    ) {
                        Text("Apply Recommended Preview Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
