package com.carxust.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carxust.core.Formatters
import com.carxust.core.UserConfig
import com.carxust.ui.components.CarCard
import com.carxust.ui.components.CarChip
import com.carxust.ui.components.IconTextButton
import com.carxust.ui.components.KeyValue
import com.carxust.ui.components.SectionLabel
import com.carxust.ui.components.StatusDot
import com.carxust.ui.components.toColor
import com.carxust.ui.theme.CarBackground
import com.carxust.ui.theme.CarBorder
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarGreen
import com.carxust.ui.theme.CarRed
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextMuted
import com.carxust.vehicle.VehicleState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    vehicle: VehicleState,
    config: UserConfig,
    onRestart: () -> Unit,
    onResetTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = config.accent.toColor()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CarBackground)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Diagnóstico",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        color = CarText
                    )
                    Text(
                        text = "Telemetria, sensores e integração veicular",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = CarTextMuted
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconTextButton(
                        icon = Icons.Rounded.RestartAlt,
                        label = "Reiniciar fonte",
                        onClick = onRestart,
                        accent = accent
                    )
                    IconTextButton(
                        icon = Icons.Rounded.Refresh,
                        label = "Zerar viagem",
                        onClick = onResetTrip,
                        accent = accent
                    )
                }
            }
        }

        item {
            CarCard(modifier = Modifier.fillMaxWidth(), accent = accent) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(if (vehicle.sourceConnected) CarGreen else CarRed)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = vehicle.sourceLabel,
                                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                                color = CarText
                            )
                        }
                        CarChip(
                            text = vehicle.connectionLabel,
                            color = if (vehicle.sourceConnected) CarGreen else CarRed,
                            icon = Icons.Rounded.Sensors
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = vehicle.sourceMessage,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = CarTextMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Última atualização: " + SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date(vehicle.lastUpdateMs.takeIf { it > 0 } ?: System.currentTimeMillis())),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = CarTextMuted
                    )
                }
            }
        }

        item {
            CarCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionLabel("Powertrain")
                    Spacer(Modifier.height(10.dp))
                    KeyValue("Velocidade", "${Formatters.speed(vehicle.speedKph, config.useMiles)} ${Formatters.speedUnit(config.useMiles)}")
                    KeyValue("RPM", vehicle.rpm.toString())
                    KeyValue("Marcha", vehicle.gear.label)
                    KeyValue("Potência", "${Formatters.power(vehicle.powerKw)} kW")
                    KeyValue("Odômetro", "${Formatters.distance(vehicle.odometerKm, config.useMiles)} ${Formatters.distanceUnit(config.useMiles)}")
                    KeyValue("Viagem", "${Formatters.distance(vehicle.tripKm, config.useMiles)} ${Formatters.distanceUnit(config.useMiles)}")
                    KeyValue("Consumo médio", String.format(Locale.US, "%.1f kWh/100 km", vehicle.avgConsumptionKwh100))
                }
            }
        }

        item {
            CarCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionLabel("Energia e clima")
                    Spacer(Modifier.height(10.dp))
                    KeyValue("SOC", "${vehicle.batteryPercent.toInt()}%")
                    KeyValue("Autonomia", "${Formatters.distance(vehicle.rangeKm, config.useMiles)} ${Formatters.distanceUnit(config.useMiles)}")
                    KeyValue("Carregando", if (vehicle.charging) "Sim • ${Formatters.power(vehicle.chargePowerKw)} kW" else "Não")
                    KeyValue("Temp. externa", Formatters.temperature(vehicle.outsideTempC, config.useMiles))
                    KeyValue("Temp. cabine", Formatters.temperature(vehicle.cabinTempC, config.useMiles))
                    KeyValue("Clima", if (vehicle.climateOn) "ON • fan ${vehicle.fanLevel}" else "OFF")
                }
            }
        }

        item {
            CarCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionLabel("Chassi e ADAS")
                    Spacer(Modifier.height(10.dp))
                    KeyValue("Portas", if (vehicle.doors.anyOpen) "Aberta(s)" else "Fechadas", valueColor = if (vehicle.doors.anyOpen) CarRed else CarText)
                    KeyValue("Cinto", if (vehicle.seatbeltFastened) "Afivelado" else "Solto", valueColor = if (vehicle.seatbeltFastened) CarGreen else CarRed)
                    KeyValue("Freio de mão", if (vehicle.parkingBrake) "Ativo" else "Liberado")
                    KeyValue("Faróis", if (vehicle.highBeam) "Alto" else if (vehicle.headlights) "Baixo" else "Off")
                    KeyValue("Seta", vehicle.turnSignal.name)
                    KeyValue("Lane assist", if (vehicle.adas.laneAssistActive) "Ativo" else "Inativo")
                    KeyValue("Offset de faixa", String.format(Locale.US, "%.2f m", vehicle.adas.laneOffsetMeters))
                    KeyValue("Carro à frente", if (vehicle.adas.leadPresent) String.format(Locale.US, "%.1f m", vehicle.adas.leadDistanceMeters) else "Livre", valueColor = if (vehicle.adas.collisionWarning) CarRed else CarText)
                    KeyValue("Cruise", if (vehicle.adas.cruiseActive) "${vehicle.adas.cruiseSetKph} km/h" else "Off")
                    KeyValue("Blind spot", listOfNotNull(
                        if (vehicle.adas.blindSpotLeft) "E" else null,
                        if (vehicle.adas.blindSpotRight) "D" else null
                    ).joinToString(", ").ifBlank { "Livre" })
                }
            }
        }

        item {
            CarCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionLabel("Pneus")
                    Spacer(Modifier.height(10.dp))
                    val p = vehicle.tirePressures
                    KeyValue("Dianteiro esquerdo", Formatters.pressure(p.frontLeft, config.useMiles))
                    KeyValue("Dianteiro direito", Formatters.pressure(p.frontRight, config.useMiles))
                    KeyValue("Traseiro esquerdo", Formatters.pressure(p.rearLeft, config.useMiles))
                    KeyValue("Traseiro direito", Formatters.pressure(p.rearRight, config.useMiles))
                }
            }
        }

        item {
            CarCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("GPS")
                        CarChip(
                            text = if (vehicle.gps.hasFix) "FIX 3D" else "SEM FIX",
                            color = if (vehicle.gps.hasFix) CarCyan else CarTextMuted,
                            icon = Icons.Rounded.Speed
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    KeyValue("Latitude", String.format(Locale.US, "%.5f", vehicle.gps.latitude))
                    KeyValue("Longitude", String.format(Locale.US, "%.5f", vehicle.gps.longitude))
                    KeyValue("Altitude", String.format(Locale.US, "%.0f m", vehicle.gps.altitudeMeters))
                    KeyValue("Heading", String.format(Locale.US, "%.0f°", vehicle.gps.heading))
                    KeyValue("Precisão", String.format(Locale.US, "%.1f m", vehicle.gps.accuracyMeters))
                }
            }
        }

        item {
            CarCard(modifier = Modifier.fillMaxWidth(), accent = if (vehicle.rawCanFrame.isNotBlank()) accent else CarBorder) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Frame bruto / VHAL")
                        Spacer(Modifier.width(8.dp))
                        CarChip(
                            text = if (vehicle.rawCanFrame.isBlank()) "aguardando" else "recebendo",
                            color = if (vehicle.rawCanFrame.isBlank()) CarTextMuted else CarGreen,
                            icon = Icons.Rounded.Warning
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = vehicle.rawCanFrame.ifBlank { "Nenhum frame disponível. Ative 'Mostrar frames brutos' ou use OBD/CAN." },
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = CarTextMuted
                    )
                }
            }
        }

        item {
            CarCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionLabel("Hardware suportado")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Orange Pi Zero 3 (Allwinner H618, Android 12/ARM64) • Raspberry Pi 4B " +
                            "(AOSP 17 car/ARM64) • OBD-II ELM327 Bluetooth • SocketCAN (can0) • " +
                            "Android Automotive VHAL • Dual display HDMI/DSI.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = CarTextMuted
                    )
                }
            }
        }
    }
}
