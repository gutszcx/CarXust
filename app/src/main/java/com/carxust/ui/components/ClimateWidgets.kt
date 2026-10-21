package com.carxust.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carxust.core.Formatters
import com.carxust.ui.theme.CarBorder
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarRed
import com.carxust.ui.theme.CarSurface
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextMuted
import com.carxust.vehicle.VehicleState

@Composable
fun ClimateMini(
    vehicle: VehicleState,
    useMiles: Boolean,
    onToggle: () -> Unit,
    onFanChange: (Int) -> Unit,
    onTempUp: () -> Unit,
    onTempDown: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = CarRed
) {
    CarCard(modifier = modifier) {
        Column {
            Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Thermostat,
                        contentDescription = null,
                        tint = if (vehicle.climateOn) accent else CarTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Column {
                        Text(
                            text = "Climatização",
                            style = MaterialTheme.typography.titleMedium,
                            color = CarText
                        )
                        Text(
                            text = if (vehicle.climateOn) "Auto • ${vehicle.fanLevel}" else "Desligado",
                            style = MaterialTheme.typography.labelSmall,
                            color = CarTextMuted
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (vehicle.climateOn) accent.copy(alpha = 0.18f) else CarSurface
                        )
                        .border(
                            1.dp,
                            if (vehicle.climateOn) accent.copy(alpha = 0.5f) else CarBorder,
                            CircleShape
                        )
                        .clickable(onClick = onToggle)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (vehicle.climateOn) "ON" else "OFF",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (vehicle.climateOn) accent else CarTextMuted
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TempStepper(
                        value = Formatters.temperature(vehicle.driverTempC, useMiles),
                        onUp = onTempUp,
                        onDown = onTempDown,
                        accent = accent
                    )
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text(
                            text = "Motorista",
                            style = MaterialTheme.typography.labelSmall,
                            color = CarTextMuted
                        )
                        Text(
                            text = "${Formatters.temperature(vehicle.cabinTempC, useMiles)} cabine",
                            style = MaterialTheme.typography.labelMedium,
                            color = CarText
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Air,
                        contentDescription = null,
                        tint = CarCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    FanDots(level = vehicle.fanLevel, onFanChange = onFanChange)
                }
            }
        }
    }
}

@Composable
fun TempStepper(
    value: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    accent: Color = CarRed,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(CarSurface)
                .border(1.dp, CarBorder, CircleShape)
                .clickable(onClick = onDown),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = "Diminuir",
                tint = CarTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = value,
            fontSize = 30.sp,
            fontWeight = FontWeight.Light,
            color = CarText
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
                .clickable(onClick = onUp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Aumentar",
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun FanDots(level: Int, onFanChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (i <= level) CarCyan else CarBorder)
                    .clickable { onFanChange(if (i == level) 0 else i) }
            )
        }
    }
}

@Composable
fun ClimateDial(
    label: String,
    tempC: Float,
    useMiles: Boolean,
    accent: Color,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = ((tempC - 16f) / 12f).coerceIn(0f, 1f)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(180.dp)) {
                val stroke = 10f
                drawArc(
                    color = CarBorder,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(stroke, stroke),
                    size = Size(size.width - stroke * 2, size.height - stroke * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.3f), accent)),
                    startAngle = 135f,
                    sweepAngle = 270f * fraction,
                    useCenter = false,
                    topLeft = Offset(stroke, stroke),
                    size = Size(size.width - stroke * 2, size.height - stroke * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.Thermostat,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = Formatters.temperature(tempC, useMiles),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Light,
                    color = CarText
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = CarTextMuted
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        TempStepper(value = "Ajustar", onUp = onUp, onDown = onDown, accent = accent)
    }
}

@Composable
fun SeatHeatButton(
    label: String,
    level: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (level > 0) accent.copy(alpha = 0.14f) else CarSurface)
            .border(
                1.dp,
                if (level > 0) accent.copy(alpha = 0.45f) else CarBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.AcUnit,
            contentDescription = null,
            tint = if (level > 0) accent else CarTextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CarTextMuted
        )
        Text(
            text = if (level == 0) "OFF" else "Nível $level",
            style = MaterialTheme.typography.labelMedium,
            color = if (level > 0) accent else CarTextMuted
        )
    }
}
