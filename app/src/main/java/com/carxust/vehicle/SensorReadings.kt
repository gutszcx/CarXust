package com.carxust.vehicle

/**
 * Leituras de sensores do veículo-organograma.
 *
 * Cada grupo é nulo quando o modelo do carro não expõe aquele dado.
 * O painel usa essa informação para mostrar apenas os sensores que
 * realmente existem no veículo conectado ao ESP32.
 */
data class SensorReadings(
    // 1. Temperatura da água
    val coolantC: Float? = null,
    // 2. Pressão do óleo
    val oilPressureBar: Float? = null,
    // 3. Nível de combustível (boia do tanque)
    val fuelPercent: Float? = null,
    // 4. Check engine / injeção (sonda lambda, MAF, MAP)
    val injection: InjectionData? = null,
    // 5. Velocimetro / VSS
    val vssKph: Float? = null,
    // 6. Tacometro / rotação
    val rpm: Int? = null,
    // 7. Odômetro
    val odometerKm: Float? = null,
    // 8. ABS (rotação de cada roda)
    val abs: AbsData? = null,
    // 9. ESP / estabilidade
    val esp: EspData? = null,
    // 10. Pressão dos pneus (TPMS)
    val tires: TirePressures? = null,
    // 11. Pastilha de freio
    val brakePads: BrakePadData? = null,
    // 12. Direção elétrica
    val steering: SteeringData? = null,
    // 13. Airbag (sensor de impacto)
    val airbagTriggered: Boolean? = null,
    // 14. Cinto de segurança
    val seatbelts: SeatbeltsData? = null,
    // 15. Porta / capô / mala
    val openings: DoorsState? = null,
    // 16. Bateria / alternador
    val battery: BatteryData? = null,
    // 17. Sensor de luz e chuva
    val lightRain: LightRainData? = null,
    // 18. Sensor de estacionamento
    val parking: ParkingData? = null,
    // 19. Ponto cego / faixa
    val assist: AssistData? = null,
    // 20. Fluidos (freio / arrefecimento / lavador)
    val fluids: FluidLevels? = null
) {
    val availableGroups: Int
        get() = SensorGroup.entries.count { it.isAvailable(this) }

    val isEmpty: Boolean
        get() = availableGroups == 0
}

data class InjectionData(
    val checkEngine: Boolean? = null,
    val lambda: Float? = null,
    val mafGramsPerSec: Float? = null,
    val mapKpa: Float? = null
)

data class AbsData(
    val frontLeftKph: Float? = null,
    val frontRightKph: Float? = null,
    val rearLeftKph: Float? = null,
    val rearRightKph: Float? = null
)

data class EspData(
    val yawRateDps: Float? = null,
    val lateralAccel: Float? = null
)

data class BrakePadData(
    val frontPercent: Float? = null,
    val rearPercent: Float? = null
)

data class SteeringData(
    val angleDeg: Float? = null,
    val torqueNm: Float? = null
)

data class SeatbeltsData(
    val driver: Boolean? = null,
    val passenger: Boolean? = null
)

data class BatteryData(
    val percent: Float? = null,
    val volts: Float? = null,
    val charging: Boolean? = null
)

data class LightRainData(
    val lights: Boolean? = null,
    val highBeam: Boolean? = null,
    val rain: Boolean? = null,
    val wiper: Boolean? = null
)

data class ParkingData(
    val frontLeftCm: Float? = null,
    val frontRightCm: Float? = null,
    val rearLeftCm: Float? = null,
    val rearRightCm: Float? = null
)

data class AssistData(
    val blindSpotLeft: Boolean? = null,
    val blindSpotRight: Boolean? = null,
    val laneVisible: Boolean? = null,
    val laneOffsetM: Float? = null,
    val leadPresent: Boolean? = null,
    val leadDistanceM: Float? = null,
    val leadSpeedKph: Float? = null
)

data class FluidLevels(
    val brakePercent: Float? = null,
    val coolantPercent: Float? = null,
    val washerPercent: Float? = null
)

/**
 * Os 20 grupos exibidos na tela "Sensores do painel do carro".
 * Grupos sem leitura ficam ocultos na interface.
 */
enum class SensorGroup(val title: String) {
    COOLANT("Temperatura da água"),
    OIL_PRESSURE("Pressão do óleo"),
    FUEL("Nível de combustível"),
    INJECTION("Check engine / injeção"),
    VSS("Velocimetro / VSS"),
    RPM("Tacometro / rotação"),
    ODOMETER("Odômetro"),
    ABS("ABS"),
    ESP("ESP / estabilidade"),
    TIRES("Pressão dos pneus (TPMS)"),
    BRAKE_PADS("Pastilha de freio"),
    STEERING("Direção elétrica"),
    AIRBAG("Airbag"),
    SEATBELTS("Cinto de segurança"),
    OPENINGS("Porta / capô / mala"),
    BATTERY("Bateria / alternador"),
    LIGHT_RAIN("Luz e chuva"),
    PARKING("Sensor de estacionamento"),
    BLIND_SPOT("Ponto cego / faixa"),
    FLUIDS("Níveis de fluidos")
}

fun SensorGroup.isAvailable(readings: SensorReadings): Boolean = when (this) {
    SensorGroup.COOLANT -> readings.coolantC != null
    SensorGroup.OIL_PRESSURE -> readings.oilPressureBar != null
    SensorGroup.FUEL -> readings.fuelPercent != null
    SensorGroup.INJECTION -> readings.injection != null
    SensorGroup.VSS -> readings.vssKph != null
    SensorGroup.RPM -> readings.rpm != null
    SensorGroup.ODOMETER -> readings.odometerKm != null
    SensorGroup.ABS -> readings.abs != null
    SensorGroup.ESP -> readings.esp != null
    SensorGroup.TIRES -> readings.tires != null
    SensorGroup.BRAKE_PADS -> readings.brakePads != null
    SensorGroup.STEERING -> readings.steering != null
    SensorGroup.AIRBAG -> readings.airbagTriggered != null
    SensorGroup.SEATBELTS -> readings.seatbelts != null
    SensorGroup.OPENINGS -> readings.openings != null
    SensorGroup.BATTERY -> readings.battery != null
    SensorGroup.LIGHT_RAIN -> readings.lightRain != null
    SensorGroup.PARKING -> readings.parking != null
    SensorGroup.BLIND_SPOT -> readings.assist != null
    SensorGroup.FLUIDS -> readings.fluids != null
}
