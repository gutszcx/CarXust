package com.carxust.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.TireRepair
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carxust.ui.theme.CarAmber
import com.carxust.ui.theme.CarBackground
import com.carxust.ui.theme.CarBorder
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarGreen
import com.carxust.ui.theme.CarRed
import com.carxust.ui.theme.CarSurface
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextMuted
import com.carxust.vehicle.AdasState
import com.carxust.vehicle.Gear
import com.carxust.vehicle.TurnSignal
import com.carxust.vehicle.VehicleState
import kotlin.math.abs
import kotlin.math.min

@Composable
fun SpeedGauge(
    speed: String,
    unit: String,
    gear: Gear,
    accent: Color,
    modifier: Modifier = Modifier,
    maxSpeed: Float = 220f,
    numericSpeed: Float = 0f
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.035f
            val inset = stroke
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = CarBorder.copy(alpha = 0.55f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            val ratio = (numericSpeed / maxSpeed).coerceIn(0f, 1f)
            if (ratio > 0.005f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            accent.copy(alpha = 0.25f),
                            accent,
                            if (ratio > 0.8f) CarAmber else accent
                        )
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * ratio,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            for (tick in 0..10) {
                val fraction = tick / 10f
                val angle = Math.toRadians((135f + 270f * fraction).toDouble())
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - stroke * 2.5f
                val outer = Offset(
                    center.x + (radius * kotlin.math.cos(angle)).toFloat(),
                    center.y + (radius * kotlin.math.sin(angle)).toFloat()
                )
                val innerRadius = radius - (if (tick % 5 == 0) stroke * 2.2f else stroke * 1.2f)
                val inner = Offset(
                    center.x + (innerRadius * kotlin.math.cos(angle)).toFloat(),
                    center.y + (innerRadius * kotlin.math.sin(angle)).toFloat()
                )
                drawLine(
                    color = if (tick % 5 == 0) CarTextMuted.copy(alpha = 0.6f) else CarBorder,
                    start = inner,
                    end = outer,
                    strokeWidth = if (tick % 5 == 0) 2.5f else 1.5f,
                    cap = StrokeCap.Round
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speed,
                fontSize = 92.sp,
                fontWeight = FontWeight.Thin,
                color = CarText,
                lineHeight = 92.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = CarTextMuted
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = gear.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryRing(
    percent: Float,
    range: String,
    rangeUnit: String,
    accent: Color,
    modifier: Modifier = Modifier,
    charging: Boolean = false
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.055f
            val inset = stroke
            drawArc(
                color = CarBorder,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        if (percent < 20f) CarRed else accent.copy(alpha = 0.35f),
                        if (percent < 20f) CarRed else accent
                    )
                ),
                startAngle = 140f,
                sweepAngle = 260f * (percent / 100f).coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percent.toInt()}%",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = if (percent < 20f) CarRed else CarText
            )
            Text(
                text = "$range $rangeUnit",
                style = MaterialTheme.typography.labelMedium,
                color = CarTextMuted
            )
            if (charging) {
                Icon(
                    imageVector = Icons.Rounded.ElectricBolt,
                    contentDescription = null,
                    tint = CarGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CarTopView(
    vehicle: VehicleState,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val bodyWidth = w * 0.34f
        val bodyHeight = h * 0.82f
        val left = cx - bodyWidth / 2f
        val top = (h - bodyHeight) / 2f

        drawOval(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(cx - bodyWidth * 0.8f, top + bodyHeight * 0.78f),
            size = Size(bodyWidth * 1.6f, bodyHeight * 0.2f)
        )

        val wheelWidth = bodyWidth * 0.16f
        val wheelHeight = bodyHeight * 0.19f
        val wheelColor = Color(0xFF0A0D11)
        listOf(
            Offset(left - wheelWidth * 0.55f, top + bodyHeight * 0.13f),
            Offset(left + bodyWidth - wheelWidth * 0.45f, top + bodyHeight * 0.13f),
            Offset(left - wheelWidth * 0.55f, top + bodyHeight * 0.66f),
            Offset(left + bodyWidth - wheelWidth * 0.45f, top + bodyHeight * 0.66f)
        ).forEach { offset ->
            drawRoundRect(
                color = wheelColor,
                topLeft = offset,
                size = Size(wheelWidth, wheelHeight),
                cornerRadius = CornerRadius(wheelWidth / 2f, wheelWidth / 2f)
            )
        }

        val body = Path().apply {
            moveTo(cx - bodyWidth * 0.42f, top)
            cubicTo(
                cx - bodyWidth * 0.54f, top + bodyHeight * 0.08f,
                cx - bodyWidth * 0.52f, top + bodyHeight * 0.3f,
                cx - bodyWidth * 0.5f, top + bodyHeight * 0.52f
            )
            cubicTo(
                cx - bodyWidth * 0.52f, top + bodyHeight * 0.78f,
                cx - bodyWidth * 0.42f, top + bodyHeight * 0.95f,
                cx - bodyWidth * 0.26f, top + bodyHeight
            )
            lineTo(cx + bodyWidth * 0.26f, top + bodyHeight)
            cubicTo(
                cx + bodyWidth * 0.42f, top + bodyHeight * 0.95f,
                cx + bodyWidth * 0.52f, top + bodyHeight * 0.78f,
                cx + bodyWidth * 0.5f, top + bodyHeight * 0.52f
            )
            cubicTo(
                cx + bodyWidth * 0.52f, top + bodyHeight * 0.3f,
                cx + bodyWidth * 0.54f, top + bodyHeight * 0.08f,
                cx + bodyWidth * 0.42f, top
            )
            close()
        }
        drawPath(
            path = body,
            brush = Brush.verticalGradient(
                listOf(
                    accent.copy(alpha = 0.9f),
                    accent.copy(alpha = 0.55f),
                    Color(0xFF1A232E)
                ),
                startY = top,
                endY = top + bodyHeight
            )
        )
        drawPath(body, color = Color.White.copy(alpha = 0.12f), style = Stroke(width = 1.5f))

        drawRoundRect(
            color = Color(0xFF081018).copy(alpha = 0.85f),
            topLeft = Offset(cx - bodyWidth * 0.33f, top + bodyHeight * 0.25f),
            size = Size(bodyWidth * 0.66f, bodyHeight * 0.24f),
            cornerRadius = CornerRadius(bodyWidth * 0.1f, bodyWidth * 0.1f)
        )
        drawRoundRect(
            color = Color(0xFF0C141D),
            topLeft = Offset(cx - bodyWidth * 0.3f, top + bodyHeight * 0.52f),
            size = Size(bodyWidth * 0.6f, bodyHeight * 0.27f),
            cornerRadius = CornerRadius(bodyWidth * 0.12f, bodyWidth * 0.12f)
        )

        val headlight = if (vehicle.headlights) CarText else Color(0xFF3A444F)
        drawRoundRect(
            color = headlight,
            topLeft = Offset(cx - bodyWidth * 0.36f, top + 2f),
            size = Size(bodyWidth * 0.18f, bodyHeight * 0.035f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = headlight,
            topLeft = Offset(cx + bodyWidth * 0.18f, top + 2f),
            size = Size(bodyWidth * 0.18f, bodyHeight * 0.035f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        if (vehicle.highBeam) {
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = bodyWidth * 0.16f,
                center = Offset(cx - bodyWidth * 0.27f, top)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = bodyWidth * 0.16f,
                center = Offset(cx + bodyWidth * 0.27f, top)
            )
        }
        drawRoundRect(
            color = CarRed.copy(alpha = 0.9f),
            topLeft = Offset(cx - bodyWidth * 0.38f, top + bodyHeight * 0.955f),
            size = Size(bodyWidth * 0.2f, bodyHeight * 0.025f),
            cornerRadius = CornerRadius(5f, 5f)
        )
        drawRoundRect(
            color = CarRed.copy(alpha = 0.9f),
            topLeft = Offset(cx + bodyWidth * 0.18f, top + bodyHeight * 0.955f),
            size = Size(bodyWidth * 0.2f, bodyHeight * 0.025f),
            cornerRadius = CornerRadius(5f, 5f)
        )

        val signalColor = CarAmber
        when (vehicle.turnSignal) {
            TurnSignal.LEFT -> drawCircle(
                color = signalColor.copy(alpha = 0.55f),
                radius = bodyWidth * 0.1f,
                center = Offset(left, top + bodyHeight * 0.16f)
            )
            TurnSignal.RIGHT -> drawCircle(
                color = signalColor.copy(alpha = 0.55f),
                radius = bodyWidth * 0.1f,
                center = Offset(left + bodyWidth, top + bodyHeight * 0.16f)
            )
            TurnSignal.HAZARD -> {
                drawCircle(
                    color = signalColor.copy(alpha = 0.45f),
                    radius = bodyWidth * 0.09f,
                    center = Offset(left, top + bodyHeight * 0.16f)
                )
                drawCircle(
                    color = signalColor.copy(alpha = 0.45f),
                    radius = bodyWidth * 0.09f,
                    center = Offset(left + bodyWidth, top + bodyHeight * 0.16f)
                )
            }
            TurnSignal.OFF -> Unit
        }

        if (vehicle.doors.frontLeft) {
            drawLine(
                color = CarAmber,
                start = Offset(left - wheelWidth * 0.2f, top + bodyHeight * 0.34f),
                end = Offset(left - wheelWidth * 1.1f, top + bodyHeight * 0.45f),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
        }
        if (vehicle.doors.frontRight) {
            drawLine(
                color = CarAmber,
                start = Offset(left + bodyWidth + wheelWidth * 0.2f, top + bodyHeight * 0.34f),
                end = Offset(left + bodyWidth + wheelWidth * 1.1f, top + bodyHeight * 0.45f),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun AdasLaneView(
    adas: AdasState,
    speed: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.18f
        val roadTopHalf = w * 0.11f
        val roadBottomHalf = w * 0.4f

        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF0A1119), Color(0xFF0D1720))
            ),
            size = size
        )

        val road = Path().apply {
            moveTo(w / 2f - roadTopHalf, horizon)
            lineTo(w / 2f + roadTopHalf, horizon)
            lineTo(w / 2f + roadBottomHalf, h)
            lineTo(w / 2f - roadBottomHalf, h)
            close()
        }
        drawPath(road, color = Color(0xFF141D26))
        drawPath(
            road,
            color = if (adas.laneAssistActive) accent.copy(alpha = 0.75f) else CarBorder,
            style = Stroke(width = 3f)
        )

        val laneHalf = (roadTopHalf + (roadBottomHalf - roadTopHalf) * 0.8f)
        val laneCenter = w / 2f + adas.laneOffsetMeters * 34f
        val laneLeftBottom = laneCenter - laneHalf * 0.9f
        val laneRightBottom = laneCenter + laneHalf * 0.9f
        drawLine(
            color = accent.copy(alpha = 0.55f),
            start = Offset(laneCenter - roadTopHalf * 0.55f, horizon * 1.6f),
            end = Offset(laneLeftBottom, h * 0.94f),
            strokeWidth = 2f
        )
        drawLine(
            color = accent.copy(alpha = 0.55f),
            start = Offset(laneCenter + roadTopHalf * 0.55f, horizon * 1.6f),
            end = Offset(laneRightBottom, h * 0.94f),
            strokeWidth = 2f
        )

        var dashY = h * 0.35f
        while (dashY < h * 0.92f) {
            val progress = (dashY - horizon) / (h - horizon)
            val half = roadTopHalf + (roadBottomHalf - roadTopHalf) * progress
            drawLine(
                color = CarText.copy(alpha = 0.22f),
                start = Offset(w / 2f, dashY),
                end = Offset(w / 2f, min(dashY + h * 0.05f, h * 0.94f)),
                strokeWidth = 2f + progress * 4f
            )
            dashY += h * 0.11f
            @Suppress("UNUSED_VARIABLE")
            val unused = half
        }

        val egoY = h * 0.86f
        val egoWidth = w * 0.19f
        val egoHeight = h * 0.2f
        drawRoundRect(
            color = Color(0xFF273747),
            topLeft = Offset(laneCenter - egoWidth / 2f, egoY),
            size = Size(egoWidth, egoHeight),
            cornerRadius = CornerRadius(egoWidth * 0.22f, egoWidth * 0.22f)
        )
        drawRoundRect(
            color = accent.copy(alpha = 0.2f),
            topLeft = Offset(laneCenter - egoWidth * 0.38f, egoY + egoHeight * 0.2f),
            size = Size(egoWidth * 0.76f, egoHeight * 0.32f),
            cornerRadius = CornerRadius(egoWidth * 0.12f, egoWidth * 0.12f)
        )

        if (adas.leadPresent) {
            val leadY = horizon + (h * 0.62f) * (1f - (adas.leadDistanceMeters / 80f).coerceIn(0.05f, 1f))
            val leadWidth = egoWidth * 0.8f
            val leadHeight = egoHeight * 0.8f
            drawRoundRect(
                color = if (adas.collisionWarning) CarRed else Color(0xFF4A5B6C),
                topLeft = Offset(w / 2f - leadWidth / 2f, leadY),
                size = Size(leadWidth, leadHeight),
                cornerRadius = CornerRadius(leadWidth * 0.2f, leadWidth * 0.2f)
            )
        }

        if (adas.collisionWarning) {
            drawRect(
                color = CarRed.copy(alpha = 0.12f),
                topLeft = Offset(0f, h * 0.7f),
                size = Size(w, h * 0.3f)
            )
        }

        if (adas.blindSpotLeft) {
            drawCircle(
                color = CarAmber.copy(alpha = 0.8f),
                radius = w * 0.025f,
                center = Offset(w * 0.08f, h * 0.72f)
            )
        }
        if (adas.blindSpotRight) {
            drawCircle(
                color = CarAmber.copy(alpha = 0.8f),
                radius = w * 0.025f,
                center = Offset(w * 0.92f, h * 0.72f)
            )
        }

        @Suppress("UNUSED_VARIABLE")
        val unusedSpeed = speed
    }
}

private data class WarningItem(
    val label: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun WarningStrip(
    vehicle: VehicleState,
    modifier: Modifier = Modifier
) {
    val warnings = buildList {
        if (vehicle.batteryLow) add(
            WarningItem("Bateria baixa", Icons.Rounded.BatteryAlert, CarRed)
        )
        if (vehicle.rangeLow) add(
            WarningItem("Autonomia baixa", Icons.Rounded.LocalGasStation, CarAmber)
        )
        if (vehicle.hasDoorOpen) add(
            WarningItem("Porta aberta", Icons.Rounded.DoorFront, CarAmber)
        )
        if (!vehicle.seatbeltFastened) add(
            WarningItem("Cinto", Icons.Rounded.Sensors, CarAmber)
        )
        if (vehicle.parkingBrake) add(
            WarningItem("Freio de mão", Icons.Rounded.Lock, CarTextMuted)
        )
        if (vehicle.adas.collisionWarning) add(
            WarningItem("Colisão", Icons.Rounded.Warning, CarRed)
        )
        if (vehicle.highBeam) add(
            WarningItem("Farol alto", Icons.Rounded.LightMode, CarCyan)
        )
        if (!vehicle.climateOn) add(
            WarningItem("Clima off", Icons.Rounded.Thermostat, CarTextMuted)
        )
        if (vehicle.tirePressures.frontLeft < 2.0f) add(
            WarningItem("Pneu FL", Icons.Rounded.TireRepair, CarAmber)
        )
    }

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (warnings.isEmpty()) {
            CarChip("Tudo normal", color = CarGreen)
        } else {
            warnings.forEach { warning ->
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(warning.color.copy(alpha = 0.12f))
                        .border(1.dp, warning.color.copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = warning.icon,
                        contentDescription = null,
                        tint = warning.color,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = warning.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = warning.color
                    )
                }
            }
        }
    }
}

@Composable
fun PowerBar(powerKw: Float, modifier: Modifier = Modifier) {
    val ratio = (abs(powerKw) / 200f).coerceIn(0f, 1f)
    val color = if (powerKw < -1f) CarGreen else if (powerKw > 140f) CarAmber else CarCyan
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionLabel(if (powerKw < -1f) "Regeneração" else "Potência")
            Text(
                text = "${if (powerKw >= 0) "+" else ""}${powerKw.toInt()} kW",
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(CarBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.5f), color)
                        )
                    )
            )
        }
    }
}

@Composable
fun TireGrid(vehicle: VehicleState, useMiles: Boolean, modifier: Modifier = Modifier) {
    val p = vehicle.tirePressures
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionLabel("Pressão dos pneus")
            Icon(
                imageVector = Icons.Rounded.TireRepair,
                contentDescription = null,
                tint = CarTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "FL" to p.frontLeft,
                "FR" to p.frontRight,
                "RL" to p.rearLeft,
                "RR" to p.rearRight
            ).forEach { (label, value) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarBackground)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = CarTextMuted
                    )
                    Text(
                        text = com.carxust.core.Formatters.pressure(value, useMiles),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (value in 2.0f..3.2f) CarText else CarAmber
                    )
                }
            }
        }
    }
}

@Composable
fun GearStrip(gear: Gear, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Gear.entries.forEach { item ->
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (item == gear) accent else Color.Transparent)
                    .border(
                        1.dp,
                        if (item == gear) accent else CarBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item == gear) Color.White else CarTextMuted
                )
            }
        }
    }
}

@Composable
fun MiniCarStatus(vehicle: VehicleState, accent: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.aspectRatio(0.6f), contentAlignment = Alignment.Center) {
        CarTopView(vehicle = vehicle, accent = accent, modifier = Modifier.fillMaxSize())
    }
}
