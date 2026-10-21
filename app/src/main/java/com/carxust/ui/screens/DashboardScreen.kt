package com.carxust.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carxust.core.Formatters
import com.carxust.core.UserConfig
import com.carxust.media.MediaUiState
import com.carxust.ui.CarScreen
import com.carxust.ui.components.CarCard
import com.carxust.ui.components.CarChip
import com.carxust.ui.components.CarTopView
import com.carxust.ui.components.ClimateMini
import com.carxust.ui.components.GearStrip
import com.carxust.ui.components.MapCanvas
import com.carxust.ui.components.MediaMini
import com.carxust.ui.components.Metric
import com.carxust.ui.components.PowerBar
import com.carxust.ui.components.SectionLabel
import com.carxust.ui.components.SpeedGauge
import com.carxust.ui.components.TireGrid
import com.carxust.ui.components.WarningStrip
import com.carxust.ui.components.toColor
import com.carxust.ui.theme.CarBackground
import com.carxust.ui.theme.CarBorder
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarGreen
import com.carxust.ui.theme.CarSurface
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextMuted
import com.carxust.vehicle.VehicleState

@Composable
fun DashboardScreen(
    vehicle: VehicleState,
    media: MediaUiState,
    config: UserConfig,
    onScreen: (CarScreen) -> Unit,
    onToggleMedia: () -> Unit,
    onNextMedia: () -> Unit,
    onPreviousMedia: () -> Unit,
    onClimateToggle: () -> Unit,
    onFanChange: (Int) -> Unit,
    onTempUp: () -> Unit,
    onTempDown: () -> Unit,
    onClusterPreview: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = config.accent.toColor()
    BoxWithConstraints(modifier.fillMaxSize().background(CarBackground)) {
        val compact = maxWidth < 1050.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WarningStrip(vehicle = vehicle, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                CarChip(
                    text = if (vehicle.sourceConnected) "VEÍCULO ONLINE" else "SEM TELEMETRIA",
                    color = if (vehicle.sourceConnected) CarGreen else Color(0xFFFF5252),
                    icon = Icons.Rounded.Speed
                )
            }
            SensorSummaryRow(
                vehicle = vehicle,
                accent = accent,
                onOpen = { onScreen(CarScreen.SENSORS) },
                modifier = Modifier.padding(top = 10.dp)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(if (compact) 0.44f else 0.4f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CarCard(modifier = Modifier.fillMaxWidth().weight(1f), accent = accent) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpeedGauge(
                                speed = Formatters.speed(vehicle.speedKph, config.useMiles),
                                unit = Formatters.speedUnit(config.useMiles),
                                gear = vehicle.gear,
                                accent = accent,
                                numericSpeed = vehicle.speedKph,
                                modifier = Modifier.weight(1.15f).fillMaxHeight()
                            )
                            Column(
                                modifier = Modifier
                                    .weight(0.85f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    SectionLabel("Veículo")
                                    Spacer(Modifier.height(4.dp))
                                    CarTopView(
                                        vehicle = vehicle,
                                        accent = accent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f, fill = false)
                                    )
                                }
                                GearStrip(gear = vehicle.gear, accent = accent)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CarCard(modifier = Modifier.weight(1.1f)) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SectionLabel("Bateria")
                                    Text(
                                        text = "${vehicle.batteryPercent.toInt()}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (vehicle.batteryPercent < 20) Color(0xFFFF5252) else CarGreen
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${Formatters.distance(vehicle.rangeKm, config.useMiles)} ${Formatters.distanceUnit(config.useMiles)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = CarText
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                PowerBar(powerKw = vehicle.powerKw, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        CarCard(modifier = Modifier.weight(0.9f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Metric(
                                    label = "Odômetro",
                                    value = Formatters.distance(vehicle.odometerKm, config.useMiles),
                                    unit = Formatters.distanceUnit(config.useMiles),
                                    accent = CarText,
                                    modifier = Modifier.weight(1f)
                                )
                                Metric(
                                    label = "Viagem",
                                    value = Formatters.distance(vehicle.tripKm, config.useMiles),
                                    unit = Formatters.distanceUnit(config.useMiles),
                                    accent = CarCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                Metric(
                                    label = "Consumo",
                                    value = String.format(java.util.Locale.US, "%.1f", vehicle.avgConsumptionKwh100),
                                    unit = "kWh/100",
                                    accent = CarTextMuted,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    CarCard(modifier = Modifier.fillMaxWidth()) {
                        TireGrid(vehicle = vehicle, useMiles = config.useMiles)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(if (compact) 0.56f else 0.6f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MapCanvas(
                        vehicle = vehicle,
                        accent = accent,
                        showRoute = true,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MediaMini(
                            media = media,
                            onToggle = onToggleMedia,
                            onNext = onNextMedia,
                            onPrevious = onPreviousMedia,
                            modifier = Modifier.weight(1f),
                            accent = accent
                        )
                        ClimateMini(
                            vehicle = vehicle,
                            useMiles = config.useMiles,
                            onToggle = onClimateToggle,
                            onFanChange = onFanChange,
                            onTempUp = onTempUp,
                            onTempDown = onTempDown,
                            modifier = Modifier.weight(1f),
                            accent = accent
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            BottomDock(
                media = media,
                onScreen = onScreen,
                onClusterPreview = onClusterPreview,
                onVolumeChange = onVolumeChange,
                accent = accent
            )
        }
    }
}

@Composable
private fun BottomDock(
    media: MediaUiState,
    onScreen: (CarScreen) -> Unit,
    onClusterPreview: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CarSurface)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DockButton(Icons.Rounded.Navigation, "Mapa", accent) { onScreen(CarScreen.NAVIGATION) }
        DockButton(Icons.Rounded.PlayArrow, "Mídia", accent) { onScreen(CarScreen.MEDIA) }
        DockButton(Icons.Rounded.AcUnit, "Clima", accent) { onScreen(CarScreen.CLIMATE) }
        DockButton(Icons.Rounded.Phone, "Telefone", accent) { onScreen(CarScreen.PHONE) }
        DockButton(Icons.Rounded.Apps, "Apps", accent) { onScreen(CarScreen.APPS) }
        DockButton(Icons.Rounded.Sensors, "Sensores", accent) { onScreen(CarScreen.SENSORS) }
        DockButton(Icons.Rounded.Settings, "Ajustes", accent) { onScreen(CarScreen.SETTINGS) }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .height(38.dp)
                .clip(CircleShape)
                .background(CarBackground)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = CarTextMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (media.isPlaying) "Tocando agora" else "Pronto",
                    style = MaterialTheme.typography.labelMedium,
                    color = CarTextMuted
                )
            }
        }
        Spacer(Modifier.weight(1f))
        CarChip(
            text = "Painel do motorista",
            icon = Icons.Rounded.Speed,
            color = CarCyan,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClusterPreview)
        )
        Row(
            modifier = Modifier
                .width(150.dp)
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.VolumeUp,
                contentDescription = null,
                tint = CarTextMuted,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(CarBorder)
                    .clickable {
                        onVolumeChange(if (media.volume > 0f) 0f else 0.75f)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(media.volume)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
    }
}

@Composable
private fun SensorSummaryRow(
    vehicle: VehicleState,
    accent: Color,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sensors = vehicle.sensors
    val chips = buildList {
        sensors.coolantC?.let { add("Água ${Formatters.temperature(it, false)}") }
        sensors.oilPressureBar?.let {
            add(String.format(java.util.Locale.US, "Óleo %.1f bar", it))
        }
        sensors.fuelPercent?.let { add("Comb ${Formatters.percent(it)}") }
        sensors.injection?.lambda?.let {
            add(String.format(java.util.Locale.US, "Lambda %.2f", it))
        }
        sensors.tires?.let {
            add(
                String.format(
                    java.util.Locale.US,
                    "TPMS %.1f bar",
                    minOf(it.frontLeft, it.frontRight, it.rearLeft, it.rearRight)
                )
            )
        }
        sensors.brakePads?.frontPercent?.let { add("Pastilha ${Formatters.percent(it)}") }
        sensors.steering?.angleDeg?.let {
            add(String.format(java.util.Locale.US, "Volante %.0f°", it))
        }
    }
    if (chips.isEmpty()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chips.take(4).forEach { text ->
            CarChip(
                text = text,
                icon = Icons.Rounded.Sensors,
                color = accent,
                modifier = Modifier.clickable { onOpen() }
            )
        }
        CarChip(
            text = "Sensores do carro",
            icon = Icons.Rounded.Sensors,
            color = CarCyan,
            modifier = Modifier.clickable { onOpen() }
        )
    }
}

@Composable
private fun DockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {    Row(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CarBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = CarText,
            fontWeight = FontWeight.Medium
        )
    }
}
