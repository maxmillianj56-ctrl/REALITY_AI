package com.realityai.companion.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.realityai.companion.ui.theme.CharcoalSurface
import com.realityai.companion.ui.theme.CyberCyan
import com.realityai.companion.ui.theme.HairlineBorder
import com.realityai.companion.ui.theme.NeonGreen
import com.realityai.companion.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RealityLogoEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.minDimension * 0.44f

        val hexPath = Path().apply {
            for (i in 0 until 6) {
                val angleRad = Math.toRadians((60.0 * i) - 90.0)
                val px = cx + (r * cos(angleRad)).toFloat()
                val py = cy + (r * sin(angleRad)).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }

        drawPath(
            path = hexPath,
            brush = Brush.linearGradient(
                colors = listOf(NeonGreen, CyberCyan),
                start = Offset(0f, 0f),
                end = Offset(this.size.width, this.size.height)
            ),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val innerR = r * 0.52f
        drawLine(
            color = NeonGreen,
            start = Offset(cx - innerR * 0.6f, cy - innerR * 0.5f),
            end = Offset(cx + innerR * 0.7f, cy),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = CyberCyan,
            start = Offset(cx + innerR * 0.7f, cy),
            end = Offset(cx - innerR * 0.6f, cy + innerR * 0.5f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun DashboardPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = HairlineBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun MetricStatBlock(
    label: String,
    value: String,
    accentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtext: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = accentColor
        )
        if (subtext != null) {
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun rememberDrawableImageBitmap(drawable: Drawable?): ImageBitmap? {
    return remember(drawable) {
        drawable?.let { d ->
            runCatching {
                if (d is BitmapDrawable && d.bitmap != null) {
                    d.bitmap.asImageBitmap()
                } else {
                    val w = d.intrinsicWidth.coerceIn(48, 128)
                    val h = d.intrinsicHeight.coerceIn(48, 128)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    d.setBounds(0, 0, canvas.width, canvas.height)
                    d.draw(canvas)
                    bmp.asImageBitmap()
                }
            }.getOrNull()
        }
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onActionClick != null) {
            androidx.compose.material3.TextButton(onClick = onActionClick) {
                Text(text = actionLabel, color = CyberCyan)
            }
        }
    }
}
