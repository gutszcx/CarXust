package com.carxust.ui.cluster

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carxust.core.Formatters
import com.carxust.core.UserConfig
import com.carxust.media.MediaUiState
import com.carxust.ui.components.AdasLaneView
import com.carxust.ui.components.BatteryRing
import com.carxust.ui.components.GearStrip
import com.carxust.ui.components.PowerBar
import com.carxust.ui.components.rememberClock
import com.carxust.ui.components.toColor
import com.carxust.ui.theme.CarAmber
import com.carxust.ui.theme.CarBackground
import com.carxust.ui.theme.CarBorder
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarGreen
import com.carxust.ui.theme.CarRed
import com.carxust.ui.theme.CarSurface
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextMuted
import com.carxust.vehicle.TurnSignal
import com.carxust.vehicle.VehicleState
import java.util.Locale

@Composable
fun ClusterScreen(
    vehicle: VehicleState,
    media: MediaUiState,
    config: UserConfig,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = config.accent.toColor()
    val clock = rememberClock()
    val blink by rememberInfiniteTransition(label = "turnBlink").animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "blink"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF0B121A), CarBackground, Color(0xFF040609))
                )
            )
            .padding(horizontal = 26.dp, vertical = 14.dp)
    ) {
        val compact = maxHeight < 600.dp
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(if (compact) 36.dp else 44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = clock,
                    fontSize = if (compact) 22.sp else 26.sp,
                    fontWeight = FontWeight.Light,
                    color = CarText
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = Formatters.shortDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = CarTextMuted
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    TopIndicator(
                        icon = Icons.Rounded.Thermostat,
                        text = Formatters.temperature(vehicle.outsideTempC, config.useMiles),
                        active = true,
                        color = CarTextMuted
                    )
                    TopIndicator(
                        icon = Icons.Rounded.LightMode,
                        text = when {
                            vehicle.highBeam -> "ALTO"
                            vehicle.headlights -> "BAIXO"
                            else -> "OFF"
                        },
                        active = vehicle.headlights,
                        color = if (vehicle.highBeam) CarCyan else CarTextMuted
                    )
                    TopIndicator(
                        icon = Icons.Rounded.Sensors,
                        text = vehicle.connectionLabel,
                        active = vehicle.sourceConnected,
                        color = if (vehicle.sourceConnected) CarGreen else CarRed
                    )
                    if (vehicle.adas.cruiseActive) {
                        TopIndicator(
                            icon = Icons.Rounded.Sensors,
                            text = "${vehicle.adas.cruiseSetKph}",
                            active = true,
                            color = CarCyan
                        )
                    }
                }
                if (onClose != null) {
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CarSurface)
                            .border(1.dp, CarBorder, CircleShape)
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Fechar",
                            tint = CarTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(0.26f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = Formatters.speed(vehicle.speedKph, config.useMiles),
                            fontSize = if (compact) 88.sp else 118.sp,
                            fontWeight = FontWeight.Thin,
                            color = CarText,
                            lineHeight = (if (compact) 88 else 118).sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.padding(bottom = if (compact) 10.dp else 16.dp)) {
                            Text(
                                text = Formatters.speedUnit(config.useMiles),
                                style = MaterialTheme.typography.labelMedium,
                                color = CarTextMuted
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accent.copy(alpha = 0.16f))
                                    .padding(horizontal = 9.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = vehicle.gear.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    GearStrip(gear = vehicle.gear, accent = accent)
                    Spacer(Modifier.height(14.dp))
                    PowerBar(
                        powerKw = vehicle.powerKw,
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        ClusterStat(
                            label = "ODÔMETRO",
                            value = Formatters.distance(vehicle.odometerKm, config.useMiles),
                            unit = Formatters.distanceUnit(config.useMiles)
                        )
                        ClusterStat(
                            label = "VIAGEM",
                            value = Formatters.distance(vehicle.tripKm, config.useMiles),
                            unit = Formatters.distanceUnit(config.useMiles)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TurnArrow(
                            icon = Icons.Rounded.ArrowBack,
                            active = vehicle.turnSignal == TurnSignal.LEFT ||
                                vehicle.turnSignal == TurnSignal.HAZARD,
                            blink = blink,
                            color = CarAmber
                        )
                        Spacer(Modifier.width(18.dp))
                        Text(
                            text = if (vehicle.adas.laneAssistActive) "LANE ASSIST" else "ADAS OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (vehicle.adas.laneAssistActive) accent else CarTextMuted
                        )
                        Spacer(Modifier.width(18.dp))
                        TurnArrow(
                            icon = Icons.Rounded.ArrowForward,
                            active = vehicle.turnSignal == TurnSignal.RIGHT ||
                                vehicle.turnSignal == TurnSignal.HAZARD,
                            blink = blink,
                            color = CarAmber
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    AdasLaneView(
                        adas = vehicle.adas,
                        speed = vehicle.speedKph,
                        accent = accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.dp, CarBorder.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ClusterStat(
                            label = "FAIXA",
                            value = String.format(Locale.US, "%+.2f", vehicle.adas.laneOffsetMeters),
                            unit = "m"
                        )
                        ClusterStat(
                            label = "À FRENTE",
                            value = if (vehicle.adas.leadPresent) {
                                String.format(Locale.US, "%.0f", vehicle.adas.leadDistanceMeters)
                            } else {
                                "—"
                            },
                            unit = "m"
                        )
                        ClusterStat(
                            label = "CONSUMO",
                            value = String.format(Locale.US, "%.1f", vehicle.avgConsumptionKwh100),
                            unit = "kWh/100"
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(0.24f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    BatteryRing(
                        percent = vehicle.batteryPercent,
                        range = Formatters.distance(vehicle.rangeKm, config.useMiles),
                        rangeUnit = Formatters.distanceUnit(config.useMiles),
                        accent = if (vehicle.batteryPercent < 20) CarRed else CarGreen,
                        charging = vehicle.charging,
                        modifier = Modifier.size(if (compact) 130.dp else 160.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (vehicle.charging) {
                            Icon(
                                imageVector = Icons.Rounded.BatteryChargingFull,
                                contentDescription = null,
                                tint = CarGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = if (vehicle.charging) {
                                "Carregando ${Formatters.power(vehicle.chargePowerKw)} kW"
                            } else {
                                "${Formatters.temperature(vehicle.driverTempC, config.useMiles)} • clima ${if (vehicle.climateOn) "on" else "off"}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = CarTextMuted
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    MediaCluster(media = media, accent = accent)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WarningLamp(
                        icon = Icons.Rounded.Lock,
                        active = vehicle.parkingBrake,
                        color = CarRed
                    )
                    WarningLamp(
                        icon = Icons.Rounded.Sensors,
                        active = !vehicle.seatbeltFastened,
                        color = CarRed
                    )
                    WarningLamp(
                        icon = Icons.Rounded.DoorFront,
                        active = vehicle.hasDoorOpen,
                        color = CarAmber
                    )
                    WarningLamp(
                        icon = Icons.Rounded.Warning,
                        active = vehicle.adas.collisionWarning,
                        color = CarRed
                    )
                    WarningLamp(
                        icon = Icons.Rounded.LightMode,
                        active = vehicle.highBeam,
                        color = CarCyan
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = buildString {
                        if (vehicle.batteryLow) append("BATERIA BAIXA  ")
                        if (vehicle.rangeLow) append("AUTONOMIA BAIXA  ")
                        if (vehicle.hasDoorOpen) append("PORTA ABERTA  ")
                        if (!vehicle.seatbeltFastened) append("CINTO  ")
                        if (vehicle.adas.collisionWarning) append("COLISÃO  ")
                    }.ifBlank { "SISTEMAS NORMAIS" },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        vehicle.adas.collisionWarning || vehicle.batteryLow -> CarRed
                        vehicle.hasDoorOpen || !vehicle.seatbeltFastened -> CarAmber
                        else -> CarTextMuted
                    }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "CarXust • cluster",
                    style = MaterialTheme.typography.labelSmall,
                    color = CarTextMuted.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun TopIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    active: Boolean,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) color else CarTextMuted.copy(alpha = 0.55f),
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) color else CarTextMuted.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun TurnArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    blink: Float,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .alpha(if (active) blink else 0.18f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) color else CarTextMuted,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun ClusterStat(label: String, value: String, unit: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CarTextMuted
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                color = CarText
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = CarTextMuted
            )
        }
    }
}

@Composable
private fun MediaCluster(media: MediaUiState, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .clip(RoundedCornerShape(14.dp))
            .background(CarSurface)
            .border(1.dp, CarBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = media.title,
                style = MaterialTheme.typography.labelLarge,
                color = CarText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (media.isPlaying) media.artist else "Pausado",
                style = MaterialTheme.typography.labelSmall,
                color = CarTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WarningLamp(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    color: Color
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (active) color else CarBorder,
        modifier = Modifier
            .size(20.dp)
            .alpha(if (active) 1f else 0.55f)
    )
}
