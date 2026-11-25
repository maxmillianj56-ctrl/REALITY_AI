package com.realityai.companion.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.realityai.companion.domain.model.ImageAdjustmentParams
import com.realityai.companion.ui.MainUiState
import com.realityai.companion.ui.components.DashboardPanel
import com.realityai.companion.ui.theme.CyberCyan
import com.realityai.companion.ui.theme.ElevatedCharcoal
import com.realityai.companion.ui.theme.HairlineBorder
import com.realityai.companion.ui.theme.NeonGreen
import com.realityai.companion.ui.theme.ObsidianBg
import com.realityai.companion.ui.theme.TextPrimary
import com.realityai.companion.ui.theme.TextSecondary
import java.util.Locale

private enum class CompareMode { SPLIT, ENHANCED, ORIGINAL }

@Composable
fun VisualAnalysisScreen(
    state: MainUiState,
    onUpdateAdjustments: (ImageAdjustmentParams) -> Unit,
    onResetToProfile: () -> Unit,
    onRequestStartCapture: () -> Unit,
    onImportImage: () -> Unit
) {
    val frame = state.capture.latestFrame
    val enhanced = state.enhancedPreviewBitmap
    val adj = state.liveAdjustments
    var compareMode by remember { mutableStateOf(CompareMode.SPLIT) }
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Visual Analysis Preview",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Text(
                text = "Preview filters transform captured frames inside REALITY AI only. They do not alter the external game's GPU rendering pipeline.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        item {
            DashboardPanel {
                if (frame == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "No captured frame available yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Authorize MediaProjection screen capture or import a screenshot from storage to compare Original vs Enhanced output.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onRequestStartCapture,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = ObsidianBg
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Start Capture")
                            }
                            OutlinedButton(
                                onClick = onImportImage,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Import Screenshot")
                            }
                        }
                    }
                } else {
                    val originalBmp = remember(frame.capturedAtMs, frame.bitmap) {
                        frame.bitmap.asImageBitmap()
                    }
                    val enhancedBmp = remember(enhanced) {
                        enhanced?.asImageBitmap() ?: originalBmp
                    }
                    val ratio = (frame.width.toFloat() / frame.height.coerceAtLeast(1)).coerceIn(0.5f, 2.2f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompareMode.entries.forEach { mode ->
                            val selected = compareMode == mode
                            OutlinedButton(
                                onClick = { compareMode = mode },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) ElevatedCharcoal else ObsidianBg,
                                    contentColor = if (selected) NeonGreen else TextSecondary
                                )
                            ) {
                                Text(mode.name, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratio)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                            .pointerInput(compareMode) {
                                if (compareMode == CompareMode.SPLIT) {
                                    detectHorizontalDragGestures { change, _ ->
                                        change.consume()
                                        splitFraction = (change.position.x / size.width.toFloat())
                                            .coerceIn(0.05f, 0.95f)
                                    }
                                }
                            }
                    ) {
                        val totalWidth = maxWidth
                        when (compareMode) {
                            CompareMode.ORIGINAL -> {
                                Image(
                                    bitmap = originalBmp,
                                    contentDescription = "Original captured frame",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            CompareMode.ENHANCED -> {
                                Image(
                                    bitmap = enhancedBmp,
                                    contentDescription = "Enhanced preview frame",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            CompareMode.SPLIT -> {
                                Image(
                                    bitmap = enhancedBmp,
                                    contentDescription = "Enhanced preview half",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .width(totalWidth * splitFraction)
                                        .fillMaxHeight()
                                        .clipToBounds()
                                ) {
                                    Image(
                                        bitmap = originalBmp,
                                        contentDescription = "Original frame half",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.width(totalWidth).fillMaxHeight()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(start = totalWidth * splitFraction)
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .background(CyberCyan)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${frame.width}x${frame.height} · ${if (frame.isFromScreenCapture) "Live Capture" else "Imported"} · Quality: ${state.effectiveBudget.processingQuality.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        OutlinedButton(onClick = onImportImage, shape = RoundedCornerShape(8.dp)) {
                            Text("Load Image")
                        }
                    }
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
                    Text(
                        text = "Image Processing Controls",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    OutlinedButton(onClick = onResetToProfile, shape = RoundedCornerShape(8.dp)) {
                        Text("Reset")
                    }
                }

                AdjustmentSlider(
                    label = "Brightness",
                    value = adj.brightness,
                    valueRange = -0.5f..0.5f,
                    valueText = String.format(Locale.US, "%+.2f", adj.brightness),
                    onValueChange = { onUpdateAdjustments(adj.copy(brightness = it)) }
                )
                AdjustmentSlider(
                    label = "Contrast",
                    value = adj.contrast,
                    valueRange = 0.5f..2.0f,
                    valueText = String.format(Locale.US, "%.2fx", adj.contrast),
                    onValueChange = { onUpdateAdjustments(adj.copy(contrast = it)) }
                )
                AdjustmentSlider(
                    label = "Saturation",
                    value = adj.saturation,
                    valueRange = 0.0f..2.0f,
                    valueText = String.format(Locale.US, "%.2fx", adj.saturation),
                    onValueChange = { onUpdateAdjustments(adj.copy(saturation = it)) }
                )
                AdjustmentSlider(
                    label = "Sharpness (High Quality Pass)",
                    value = adj.sharpness,
                    valueRange = 0.0f..1.5f,
                    valueText = String.format(Locale.US, "%.2f", adj.sharpness),
                    onValueChange = { onUpdateAdjustments(adj.copy(sharpness = it)) }
                )
                AdjustmentSlider(
                    label = "Gamma",
                    value = adj.gamma,
                    valueRange = 0.5f..2.2f,
                    valueText = String.format(Locale.US, "%.2f", adj.gamma),
                    onValueChange = { onUpdateAdjustments(adj.copy(gamma = it)) }
                )
            }
        }
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(text = valueText, style = MaterialTheme.typography.labelMedium, color = CyberCyan)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = NeonGreen,
                activeTrackColor = NeonGreen
            )
        )
    }
}
