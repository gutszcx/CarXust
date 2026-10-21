package com.carxust.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.TireRepair
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carxust.core.Formatters
import com.carxust.core.UserConfig
import com.carxust.ui.components.CarCard
import com.carxust.ui.components.CarChip
import com.carxust.ui.components.SectionLabel
import com.carxust.ui.components.toColor
import com.carxust.ui.theme.CarBackground
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarGreen
import com.carxust.ui.theme.CarRed
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextMuted
import com.carxust.vehicle.SensorGroup
import com.carxust.vehicle.SensorReadings
import com.carxust.vehicle.VehicleState
import com.carxust.vehicle.isAvailable
import java.util.Locale
import kotlin.math.abs

private data class SensorRow(
    val label: String,
    val value: String,
    val color: Color = CarText
)

/**
 * Tela "Sensores do painel do carro".
 *
 * Mostra apenas os grupos de sensores que o veículo realmente entrega.
 * Tudo que o ESP32 não enviar fica oculto, porque o carro não tem o sensor.
 */
@Composable
fun SensorsScreen(
    vehicle: VehicleState,
    config: UserConfig,
    modifier: Modifier = Modifier
) {
    val accent = config.accent.toColor()
    val readings = vehicle.sensors
    val groups = SensorGroup.entries.filter { it.isAvailable(readings) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarBackground)
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SENSORES DO PAINEL DO CARRO",
                    style = MaterialTheme.typography.headlineSmall,
                    color = CarText,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Leitura direta do veículo via ${vehicle.sourceLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CarTextMuted
                )
            }
            CarChip(
                text = "${groups.size} de ${SensorGroup.entries.size} grupos",
                icon = Icons.Rounded.Sensors,
                color = if (groups.isEmpty()) CarTextMuted else accent
            )
        }
        Spacer(Modifier.height(14.dp))

        if (groups.isEmpty()) {
            CarCard(modifier = Modifier.fillMaxWidth(), accent = accent) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Nenhum sensor disponível",
                            style = MaterialTheme.typography.titleMedium,
                            color = CarText
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Conecte o ESP32 no conector OBD2 e escolha a fonte " +
                            "\"ESP32\" em Ajustes → Fonte de telemetria. O painel mostra " +
                            "somente os sensores que o modelo do carro oferece.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CarTextMuted
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            CarCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionLabel("Como funciona")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "1. ESP32 ligado no OBD2, com ELM327 ou CAN direto.\n" +
                            "2. ESP32 na mesma rede Wi-Fi do painel (ou no ponto de acesso dele).\n" +
                            "3. Em Ajustes, informe o IP e a porta do ESP32 (padrão 192.168.4.1:3333).\n" +
                            "4. Os grupos aparecem aqui assim que o veículo responder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CarTextMuted
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(330.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups) { group ->
                    SensorGroupCard(
                        group = group,
                        readings = readings,
                        useMiles = config.useMiles,
                        accent = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorGroupCard(
    group: SensorGroup,
    readings: SensorReadings,
    useMiles: Boolean,
    accent: Color
) {
    val rows = groupRows(group, readings, useMiles)
    CarCard(modifier = Modifier.fillMaxWidth(), accent = accent) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = groupIcon(group),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = CarText,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CarTextMuted
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = row.value,
                        style = MaterialTheme.typography.titleSmall,
                        color = row.color
                    )
                }
            }
        }
    }
}

private fun groupIcon(group: SensorGroup): ImageVector = when (group) {
    SensorGroup.COOLANT -> Icons.Rounded.Thermostat
    SensorGroup.OIL_PRESSURE -> Icons.Rounded.Bolt
    SensorGroup.FUEL -> Icons.Rounded.LocalGasStation
    SensorGroup.INJECTION -> Icons.Rounded.Warning
    SensorGroup.VSS -> Icons.Rounded.Speed
    SensorGroup.RPM -> Icons.Rounded.Speed
    SensorGroup.ODOMETER -> Icons.Rounded.Speed
    SensorGroup.ABS -> Icons.Rounded.TireRepair
    SensorGroup.ESP -> Icons.Rounded.Radio
    SensorGroup.TIRES -> Icons.Rounded.TireRepair
    SensorGroup.BRAKE_PADS -> Icons.Rounded.Build
    SensorGroup.STEERING -> Icons.Rounded.Speed
    SensorGroup.AIRBAG -> Icons.Rounded.Warning
    SensorGroup.SEATBELTS -> Icons.Rounded.EventSeat
    SensorGroup.OPENINGS -> Icons.Rounded.DoorFront
    SensorGroup.BATTERY -> Icons.Rounded.Bolt
    SensorGroup.LIGHT_RAIN -> Icons.Rounded.Sensors
    SensorGroup.PARKING -> Icons.Rounded.Radio
    SensorGroup.BLIND_SPOT -> Icons.Rounded.Sensors
    SensorGroup.FLUIDS -> Icons.Rounded.Build
}

private fun groupRows(
    group: SensorGroup,
    readings: SensorReadings,
    useMiles: Boolean
): List<SensorRow> {
    val rows = mutableListOf<SensorRow>()

    when (group) {
        SensorGroup.COOLANT -> readings.coolantC?.let {
            rows += SensorRow(
                "Temperatura",
                "${Formatters.temperature(it, useMiles)} ${Formatters.temperatureUnit(useMiles)}",
                if (it > 110f) CarRed else CarText
            )
        }

        SensorGroup.OIL_PRESSURE -> readings.oilPressureBar?.let {
            rows += SensorRow(
                "Pressão",
                String.format(Locale.US, "%.1f bar", it),
                if (it < 1.0f) CarRed else CarText
            )
        }

        SensorGroup.FUEL -> readings.fuelPercent?.let {
            rows += SensorRow(
                "Tanque",
                Formatters.percent(it),
                if (it < 15f) CarRed else CarText
            )
        }

        SensorGroup.INJECTION -> readings.injection?.let { injection ->
            injection.checkEngine?.let {
                rows += SensorRow(
                    "Check engine",
                    if (it) "Aceso" else "Normal",
                    if (it) CarRed else CarGreen
                )
            }
            injection.lambda?.let { rows += SensorRow("Sonda lambda", String.format(Locale.US, "%.2f", it)) }
            injection.mafGramsPerSec?.let { rows += SensorRow("MAF", String.format(Locale.US, "%.1f g/s", it)) }
            injection.mapKpa?.let { rows += SensorRow("MAP", String.format(Locale.US, "%.0f kPa", it)) }
        }

        SensorGroup.VSS -> readings.vssKph?.let {
            rows += SensorRow("Velocimetro", "${Formatters.speed(it, useMiles)} ${Formatters.speedUnit(useMiles)}")
        }

        SensorGroup.RPM -> readings.rpm?.let {
            rows += SensorRow("Rotação", "$it rpm")
        }

        SensorGroup.ODOMETER -> readings.odometerKm?.let {
            rows += SensorRow(
                "Quilometragem",
                "${Formatters.distance(it, useMiles)} ${Formatters.distanceUnit(useMiles)}"
            )
        }

        SensorGroup.ABS -> readings.abs?.let { abs ->
            abs.frontLeftKph?.let { rows += SensorRow("Rodas dianteiras E", "${Formatters.speed(it, useMiles)} km/h") }
            abs.frontRightKph?.let { rows += SensorRow("Rodas dianteiras D", "${Formatters.speed(it, useMiles)} km/h") }
            abs.rearLeftKph?.let { rows += SensorRow("Rodas traseiras E", "${Formatters.speed(it, useMiles)} km/h") }
            abs.rearRightKph?.let { rows += SensorRow("Rodas traseiras D", "${Formatters.speed(it, useMiles)} km/h") }
        }

        SensorGroup.ESP -> readings.esp?.let { esp ->
            esp.yawRateDps?.let { rows += SensorRow("Guinada", String.format(Locale.US, "%.1f °/s", it)) }
            esp.lateralAccel?.let { rows += SensorRow("Aceleração lateral", String.format(Locale.US, "%.2f m/s²", it)) }
        }

        SensorGroup.TIRES -> readings.tires?.let { tires ->
            rows += SensorRow("Dianteira E", Formatters.pressure(tires.frontLeft, useMiles), tireColor(tires.frontLeft))
            rows += SensorRow("Dianteira D", Formatters.pressure(tires.frontRight, useMiles), tireColor(tires.frontRight))
            rows += SensorRow("Traseira E", Formatters.pressure(tires.rearLeft, useMiles), tireColor(tires.rearLeft))
            rows += SensorRow("Traseira D", Formatters.pressure(tires.rearRight, useMiles), tireColor(tires.rearRight))
        }

        SensorGroup.BRAKE_PADS -> readings.brakePads?.let { pads ->
            pads.frontPercent?.let {
                rows += SensorRow("Pastilha dianteira", Formatters.percent(it), if (it < 20f) CarRed else CarText)
            }
            pads.rearPercent?.let {
                rows += SensorRow("Pastilha traseira", Formatters.percent(it), if (it < 20f) CarRed else CarText)
            }
        }

        SensorGroup.STEERING -> readings.steering?.let { steering ->
            steering.angleDeg?.let { rows += SensorRow("Ângulo do volante", String.format(Locale.US, "%.1f°", it)) }
            steering.torqueNm?.let { rows += SensorRow("Torque", String.format(Locale.US, "%.1f Nm", it)) }
        }

        SensorGroup.AIRBAG -> readings.airbagTriggered?.let {
            rows += SensorRow(
                "Sensor de impacto",
                if (it) "Airbag disparado" else "Sem impacto",
                if (it) CarRed else CarGreen
            )
        }

        SensorGroup.SEATBELTS -> readings.seatbelts?.let { belts ->
            belts.driver?.let {
                rows += SensorRow("Motorista", if (it) "Aferido" else "Solto", if (it) CarGreen else CarRed)
            }
            belts.passenger?.let {
                rows += SensorRow("Passageiro", if (it) "Aferido" else "Solto", if (it) CarGreen else CarRed)
            }
        }

        SensorGroup.OPENINGS -> readings.openings?.let { doors ->
            doors.frontLeft.takeIf { it }?.let { rows += SensorRow("Porta dianteira E", "Aberta", CarRed) }
            doors.frontRight.takeIf { it }?.let { rows += SensorRow("Porta dianteira D", "Aberta", CarRed) }
            doors.rearLeft.takeIf { it }?.let { rows += SensorRow("Porta traseira E", "Aberta", CarRed) }
            doors.rearRight.takeIf { it }?.let { rows += SensorRow("Porta traseira D", "Aberta", CarRed) }
            doors.trunk.takeIf { it }?.let { rows += SensorRow("Mala", "Aberta", CarRed) }
            doors.hood.takeIf { it }?.let { rows += SensorRow("Capô", "Aberto", CarRed) }
            if (rows.isEmpty()) rows += SensorRow("Portas e capô", "Todos fechados", CarGreen)
        }

        SensorGroup.BATTERY -> readings.battery?.let { battery ->
            battery.percent?.let {
                rows += SensorRow("Carga", Formatters.percent(it), if (it < 20f) CarRed else CarText)
            }
            battery.volts?.let {
                rows += SensorRow("Tensão", String.format(Locale.US, "%.1f V", it), if (it < 12f) CarRed else CarText)
            }
            battery.charging?.let {
                rows += SensorRow("Alternador", if (it) "Carregando" else "Não carrega", if (it) CarGreen else CarTextMuted)
            }
        }

        SensorGroup.LIGHT_RAIN -> readings.lightRain?.let { light ->
            light.lights?.let {
                rows += SensorRow("Faróis", if (it) "Ligados" else "Apagados", if (it) CarGreen else CarTextMuted)
            }
            light.highBeam?.let {
                rows += SensorRow("Luz alta", if (it) "Ligada" else "Apagada", if (it) CarGreen else CarTextMuted)
            }
            light.rain?.let {
                rows += SensorRow("Chuva", if (it) "Detectada" else "Sem chuva", if (it) CarCyan else CarTextMuted)
            }
            light.wiper?.let {
                rows += SensorRow("Limpador", if (it) "Ligado" else "Desligado", if (it) CarGreen else CarTextMuted)
            }
        }

        SensorGroup.PARKING -> readings.parking?.let { parking ->
            parking.frontLeftCm?.let { rows += SensorRow("Sensor dianteiro E", distanceCm(it), parkingColor(it)) }
            parking.frontRightCm?.let { rows += SensorRow("Sensor dianteiro D", distanceCm(it), parkingColor(it)) }
            parking.rearLeftCm?.let { rows += SensorRow("Sensor traseiro E", distanceCm(it), parkingColor(it)) }
            parking.rearRightCm?.let { rows += SensorRow("Sensor traseiro D", distanceCm(it), parkingColor(it)) }
        }

        SensorGroup.BLIND_SPOT -> readings.assist?.let { assist ->
            assist.blindSpotLeft?.let {
                rows += SensorRow("Ponto cego E", if (it) "Veículo ao lado" else "Livre", if (it) CarRed else CarGreen)
            }
            assist.blindSpotRight?.let {
                rows += SensorRow("Ponto cego D", if (it) "Veículo ao lado" else "Livre", if (it) CarRed else CarGreen)
            }
            assist.laneVisible?.let {
                rows += SensorRow("Faixa", if (it) "Detectada" else "Não detectada", if (it) CarGreen else CarTextMuted)
            }
            assist.laneOffsetM?.let {
                rows += SensorRow("Desvio da faixa", String.format(Locale.US, "%.2f m", abs(it)))
            }
            assist.leadPresent?.let {
                rows += SensorRow("Carro à frente", if (it) "Presente" else "Livre", if (it) CarText else CarGreen)
            }
            assist.leadDistanceM?.let {
                rows += SensorRow("Distância", String.format(Locale.US, "%.0f m", it))
            }
        }

        SensorGroup.FLUIDS -> readings.fluids?.let { fluids ->
            fluids.brakePercent?.let {
                rows += SensorRow("Fluido de freio", Formatters.percent(it), if (it < 20f) CarRed else CarText)
            }
            fluids.coolantPercent?.let {
                rows += SensorRow("Arrefecimento", Formatters.percent(it), if (it < 20f) CarRed else CarText)
            }
            fluids.washerPercent?.let {
                rows += SensorRow("Lavador", Formatters.percent(it), if (it < 15f) CarRed else CarText)
            }
        }
    }

    return rows
}

private fun tireColor(bar: Float): Color = when {
    bar <= 0f -> CarTextMuted
    bar < 1.9f || bar > 2.9f -> CarRed
    else -> CarText
}

private fun parkingColor(cm: Float): Color = when {
    cm <= 0f -> CarTextMuted
    cm < 30f -> CarRed
    else -> CarText
}

private fun distanceCm(cm: Float): String = "${cm.toInt()} cm"
