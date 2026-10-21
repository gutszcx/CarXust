package com.carxust.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.carxust.ui.theme.CarBackground
import com.carxust.ui.theme.CarBlue
import com.carxust.ui.theme.CarBorder
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarSurface
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextMuted
import com.carxust.vehicle.VehicleState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MapCanvas(
    vehicle: VehicleState,
    accent: Color,
    modifier: Modifier = Modifier,
    showRoute: Boolean = true,
    interactiveChrome: Boolean = true
) {
    val pulse by rememberInfiniteTransition(label = "mapPulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val heading = vehicle.gps.heading
    val shiftX = ((vehicle.gps.longitude * 1000) % 1.0).toFloat()
    val shiftY = ((vehicle.gps.latitude * 1000) % 1.0).toFloat()

    Box(modifier = modifier.clip(RoundedCornerShape(24.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0B1118), Color(0xFF0E1A22), Color(0xFF0A1218)),
                    start = Offset.Zero,
                    end = Offset(w, h)
                )
            )

            val blockSize = w * 0.13f
            val rows = (h / blockSize).toInt() + 2
            val cols = (w / blockSize).toInt() + 2
            for (row in 0..rows) {
                for (col in 0..cols) {
                    val left = col * blockSize - shiftX * blockSize * 0.3f
                    val top = row * blockSize - shiftY * blockSize * 0.3f
                    val inset = blockSize * 0.14f
                    val tone = ((row * 7 + col * 13) % 5)
                    val color = when (tone) {
                        0 -> Color(0xFF101C25)
                        1 -> Color(0xFF0E1922)
                        2 -> Color(0xFF121E26)
                        3 -> Color(0xFF0C161E)
                        else -> Color(0xFF101A21)
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left + inset, top + inset),
                        size = Size(blockSize - inset * 2, blockSize - inset * 2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                    )
                }
            }

            drawLine(
                color = Color(0xFF102A3A),
                start = Offset(w * 0.0f, h * 0.72f),
                end = Offset(w, h * 0.2f),
                strokeWidth = w * 0.035f
            )
            drawLine(
                color = Color(0xFF15384C),
                start = Offset(w * 0.0f, h * 0.72f),
                end = Offset(w, h * 0.2f),
                strokeWidth = w * 0.012f
            )

            val avenue = Offset(w * 0.5f, h * 0.5f)
            drawLine(
                color = Color(0xFF1A222B),
                start = Offset(0f, avenue.y - h * 0.05f),
                end = Offset(w, avenue.y + h * 0.03f),
                strokeWidth = w * 0.06f
            )
            drawLine(
                color = Color(0xFF29323C),
                start = Offset(0f, avenue.y - h * 0.05f),
                end = Offset(w, avenue.y + h * 0.03f),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))
            )
            drawLine(
                color = Color(0xFF1A222B),
                start = Offset(w * 0.28f, 0f),
                end = Offset(w * 0.33f, h),
                strokeWidth = w * 0.045f
            )
            drawLine(
                color = Color(0xFF1A222B),
                start = Offset(w * 0.76f, 0f),
                end = Offset(w * 0.7f, h),
                strokeWidth = w * 0.035f
            )

            if (showRoute) {
                val route = Path().apply {
                    moveTo(w * 0.18f, h * 0.9f)
                    cubicTo(w * 0.3f, h * 0.72f, w * 0.43f, h * 0.62f, w * 0.52f, h * 0.52f)
                    cubicTo(w * 0.62f, h * 0.42f, w * 0.7f, h * 0.33f, w * 0.86f, h * 0.16f)
                }
                drawPath(
                    path = route,
                    color = accent.copy(alpha = 0.22f),
                    style = Stroke(width = w * 0.035f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = route,
                    color = accent,
                    style = Stroke(width = w * 0.011f, cap = StrokeCap.Round)
                )
                val center = Offset(w * 0.5f, h * 0.5f)
                rotate(heading, pivot = center) {
                    val arrow = Path().apply {
                        moveTo(center.x, center.y - h * 0.05f)
                        lineTo(center.x + w * 0.026f, center.y + h * 0.035f)
                        lineTo(center.x, center.y + h * 0.016f)
                        lineTo(center.x - w * 0.026f, center.y + h * 0.035f)
                        close()
                    }
                    drawPath(arrow, color = CarCyan)
                }
            }

            val carCenter = Offset(w * 0.5f, h * 0.58f)
            if (!showRoute) {
                rotate(heading, pivot = carCenter) {
                    drawCircle(
                        color = CarCyan.copy(alpha = 0.12f),
                        radius = w * 0.05f * pulse,
                        center = carCenter
                    )
                    val arrow = Path().apply {
                        moveTo(carCenter.x, carCenter.y - h * 0.035f)
                        lineTo(carCenter.x + w * 0.018f, carCenter.y + h * 0.024f)
                        lineTo(carCenter.x, carCenter.y + h * 0.011f)
                        lineTo(carCenter.x - w * 0.018f, carCenter.y + h * 0.024f)
                        close()
                    }
                    drawPath(arrow, color = CarCyan)
                }
            }

            drawLine(
                color = Color(0xFF0A1117).copy(alpha = 0.75f),
                start = Offset(0f, h * 0.85f),
                end = Offset(w, h * 0.85f),
                strokeWidth = 1f
            )
        }

        if (interactiveChrome) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CarSurface.copy(alpha = 0.92f))
                        .border(1.dp, CarBorder, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Av. Paulista, 1578",
                            style = MaterialTheme.typography.labelLarge,
                            color = CarText
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CarSurface.copy(alpha = 0.92f))
                        .border(1.dp, CarBorder, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = if (vehicle.gps.hasFix) "GPS 3D" else "GPS —",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (vehicle.gps.hasFix) CarCyan else CarTextMuted
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CarSurface.copy(alpha = 0.94f))
                        .border(1.dp, CarBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Zoom +",
                        tint = CarText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CarSurface.copy(alpha = 0.94f))
                        .border(1.dp, CarBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Zoom -",
                        tint = CarText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CarBackground.copy(alpha = 0.85f))
                        .border(1.dp, CarBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Explore,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Navegação ativa",
                        style = MaterialTheme.typography.labelMedium,
                        color = CarText
                    )
                    Text(
                        text = "Destino: Centro • 8,4 km • 14 min",
                        style = MaterialTheme.typography.labelSmall,
                        color = CarTextMuted
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(CarText.copy(alpha = 0.92f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "60",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                CompassBadge(heading = heading)
            }
        }
    }
}

@Composable
private fun CompassBadge(heading: Float) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(CarSurface.copy(alpha = 0.92f))
            .border(1.dp, CarBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "N",
                style = MaterialTheme.typography.labelSmall,
                color = CarBlue
            )
            Text(
                text = "${heading.toInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = CarTextMuted
            )
        }
    }
}
