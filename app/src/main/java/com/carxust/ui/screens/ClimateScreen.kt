package com.carxust.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Thermostat
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
import com.carxust.ui.components.CarCard
import com.carxust.ui.components.CarTopView
import com.carxust.ui.components.ClimateDial
import com.carxust.ui.components.FanDots
import com.carxust.ui.components.SeatHeatButton
import com.carxust.ui.components.SectionLabel
import com.carxust.ui.components.TempStepper
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
fun ClimateScreen(
    vehicle: VehicleState,
    config: UserConfig,
    onClimateToggle: () -> Unit,
    onFanChange: (Int) -> Unit,
    onDriverTemp: (Float) -> Unit,
    onPassengerTemp: (Float) -> Unit,
    onSeatHeatDriver: () -> Unit,
    onSeatHeatPassenger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = config.accent.toColor()
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(CarBackground)
            .padding(22.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CarCard(modifier = Modifier.weight(0.32f).fillMaxHeight(), accent = accent) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Cabine")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (vehicle.climateOn) CarGreen else CarTextMuted)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (vehicle.climateOn) "Clima ativo" else "Clima off",
                            style = MaterialTheme.typography.labelMedium,
                            color = CarTextMuted
                        )
                    }
                }
                CarTopView(
                    vehicle = vehicle,
                    accent = accent,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Formatters.temperature(vehicle.cabinTempC, config.useMiles),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Thin,
                        color = CarText
                    )
                    Text(
                        text = "Temperatura da cabine",
                        style = MaterialTheme.typography.labelMedium,
                        color = CarTextMuted
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            icon = Icons.Rounded.AcUnit,
                            label = "A/C",
                            active = vehicle.climateOn,
                            accent = accent
                        )
                        StatusPill(
                            icon = Icons.Rounded.Air,
                            label = "Auto",
                            active = true,
                            accent = accent
                        )
                        StatusPill(
                            icon = Icons.Rounded.Sync,
                            label = "Sync",
                            active = false,
                            accent = accent
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(0.42f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CarCard(modifier = Modifier.fillMaxWidth().weight(1f), accent = accent) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "Climatização",
                        style = MaterialTheme.typography.headlineMedium,
                        color = CarText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ClimateDial(
                            label = "MOTORISTA",
                            tempC = vehicle.driverTempC,
                            useMiles = config.useMiles,
                            accent = accent,
                            onUp = { onDriverTemp(vehicle.driverTempC + 0.5f) },
                            onDown = { onDriverTemp(vehicle.driverTempC - 0.5f) }
                        )
                        ClimateDial(
                            label = "PASSAGEIRO",
                            tempC = vehicle.passengerTempC,
                            useMiles = config.useMiles,
                            accent = accent,
                            onUp = { onPassengerTemp(vehicle.passengerTempC + 0.5f) },
                            onDown = { onPassengerTemp(vehicle.passengerTempC - 0.5f) }
                        )
                    }
                }
            }

            CarCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        SectionLabel("Ventilação")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when (vehicle.fanLevel) {
                                0 -> "Desligada"
                                1 -> "Nível 1 • silencioso"
                                2 -> "Nível 2"
                                3 -> "Nível 3 • automático"
                                4 -> "Nível 4"
                                else -> "Nível ${vehicle.fanLevel}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = CarText
                        )
                    }
                    FanDots(level = vehicle.fanLevel, onFanChange = onFanChange)
                }
            }
        }

        Column(
            modifier = Modifier.weight(0.26f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CarCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("Assentos")
                        Icon(
                            imageVector = Icons.Rounded.EventSeat,
                            contentDescription = null,
                            tint = CarTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SeatHeatButton(
                            label = "Motorista",
                            level = vehicle.seatHeatDriver,
                            accent = accent,
                            onClick = onSeatHeatDriver,
                            modifier = Modifier.weight(1f)
                        )
                        SeatHeatButton(
                            label = "Passageiro",
                            level = vehicle.seatHeatPassenger,
                            accent = accent,
                            onClick = onSeatHeatPassenger,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            CarCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionLabel("Temperaturas")
                    Spacer(Modifier.height(10.dp))
                    TemperatureRow("Externa", vehicle.outsideTempC, config.useMiles)
                    Spacer(Modifier.height(8.dp))
                    TemperatureRow("Bateria", vehicle.outsideTempC + 6f, config.useMiles)
                    Spacer(Modifier.height(8.dp))
                    TemperatureRow("Motor", vehicle.outsideTempC + 18f, config.useMiles)
                }
            }

            CarCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClimateToggle)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        SectionLabel(if (vehicle.climateOn) "Desligar clima" else "Ligar clima")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (vehicle.climateOn) "Manter cabine em ${Formatters.temperature(vehicle.driverTempC, config.useMiles)}" else "Clima desligado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CarTextMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (vehicle.climateOn) accent.copy(alpha = 0.18f) else CarSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Thermostat,
                            contentDescription = null,
                            tint = if (vehicle.climateOn) accent else CarTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemperatureRow(label: String, tempC: Float, useMiles: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = CarTextMuted
        )
        Text(
            text = Formatters.temperature(tempC, useMiles),
            style = MaterialTheme.typography.titleMedium,
            color = CarText
        )
    }
}

@Composable
private fun StatusPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    accent: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) accent.copy(alpha = 0.14f) else CarSurface)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) accent else CarTextMuted,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) accent else CarTextMuted
        )
    }
}
