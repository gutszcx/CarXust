package com.carxust.vehicle

import java.util.Locale

enum class Gear(val label: String) {
    PARK("P"),
    REVERSE("R"),
    NEUTRAL("N"),
    DRIVE("D"),
    SPORT("S")
}

enum class TurnSignal {
    OFF,
    LEFT,
    RIGHT,
    HAZARD
}

data class TirePressures(
    val frontLeft: Float = 2.4f,
    val frontRight: Float = 2.4f,
    val rearLeft: Float = 2.3f,
    val rearRight: Float = 2.3f
)

data class DoorsState(
    val frontLeft: Boolean = false,
    val frontRight: Boolean = false,
    val rearLeft: Boolean = false,
    val rearRight: Boolean = false,
    val trunk: Boolean = false,
    val hood: Boolean = false
) {
    val anyOpen: Boolean
        get() = frontLeft || frontRight || rearLeft || rearRight || trunk || hood
}

data class GpsState(
    val latitude: Double = -23.5505,
    val longitude: Double = -46.6333,
    val altitudeMeters: Double = 760.0,
    val heading: Float = 0f,
    val accuracyMeters: Float = 0f,
    val hasFix: Boolean = false
)

data class AdasState(
    val laneAssistActive: Boolean = true,
    val laneOffsetMeters: Float = 0.05f,
    val laneWidthMeters: Float = 3.5f,
    val leadDistanceMeters: Float = 42f,
    val leadSpeedKph: Float = 58f,
    val leadPresent: Boolean = true,
    val collisionWarning: Boolean = false,
    val cruiseActive: Boolean = false,
    val cruiseSetKph: Int = 110,
    val blindSpotLeft: Boolean = false,
    val blindSpotRight: Boolean = false
)

data class VehicleState(
    val speedKph: Float = 0f,
    val rpm: Int = 0,
    val gear: Gear = Gear.PARK,
    val batteryPercent: Float = 78f,
    val rangeKm: Float = 342f,
    val powerKw: Float = 0f,
    val charging: Boolean = false,
    val chargePowerKw: Float = 0f,
    val outsideTempC: Float = 24f,
    val cabinTempC: Float = 22f,
    val climateOn: Boolean = true,
    val fanLevel: Int = 3,
    val driverTempC: Float = 22f,
    val passengerTempC: Float = 22f,
    val seatHeatDriver: Int = 0,
    val seatHeatPassenger: Int = 0,
    val odometerKm: Float = 18_432f,
    val tripKm: Float = 128.4f,
    val avgConsumptionKwh100: Float = 14.8f,
    val tirePressures: TirePressures = TirePressures(),
    val doors: DoorsState = DoorsState(),
    val headlights: Boolean = false,
    val highBeam: Boolean = false,
    val parkingBrake: Boolean = false,
    val seatbeltFastened: Boolean = true,
    val turnSignal: TurnSignal = TurnSignal.OFF,
    val gps: GpsState = GpsState(),
    val adas: AdasState = AdasState(),
    val sourceLabel: String = "Simulador",
    val sourceConnected: Boolean = true,
    val sourceMessage: String = "Dados simulados",
    val rawCanFrame: String = "",
    val lastUpdateMs: Long = 0L
) {
    val displaySpeed: Int
        get() = speedKph.toInt().coerceAtLeast(0)

    val isMoving: Boolean
        get() = speedKph > 1.5f

    val batteryLow: Boolean
        get() = batteryPercent <= 15f

    val rangeLow: Boolean
        get() = rangeKm <= 40f

    val hasDoorOpen: Boolean
        get() = doors.anyOpen

    val connectionLabel: String
        get() = if (sourceConnected) "ONLINE" else "SEM SINAL"
}

data class VehiclePatch(
    val speedKph: Float? = null,
    val rpm: Int? = null,
    val gear: Gear? = null,
    val batteryPercent: Float? = null,
    val rangeKm: Float? = null,
    val powerKw: Float? = null,
    val charging: Boolean? = null,
    val chargePowerKw: Float? = null,
    val outsideTempC: Float? = null,
    val cabinTempC: Float? = null,
    val climateOn: Boolean? = null,
    val fanLevel: Int? = null,
    val driverTempC: Float? = null,
    val passengerTempC: Float? = null,
    val seatHeatDriver: Int? = null,
    val seatHeatPassenger: Int? = null,
    val odometerKm: Float? = null,
    val tripKm: Float? = null,
    val avgConsumptionKwh100: Float? = null,
    val tirePressures: TirePressures? = null,
    val doors: DoorsState? = null,
    val headlights: Boolean? = null,
    val highBeam: Boolean? = null,
    val parkingBrake: Boolean? = null,
    val seatbeltFastened: Boolean? = null,
    val turnSignal: TurnSignal? = null,
    val gps: GpsState? = null,
    val adas: AdasState? = null,
    val rawCanFrame: String? = null
)

fun VehicleState.applyPatch(patch: VehiclePatch): VehicleState = copy(
    speedKph = patch.speedKph ?: speedKph,
    rpm = patch.rpm ?: rpm,
    gear = patch.gear ?: gear,
    batteryPercent = patch.batteryPercent ?: batteryPercent,
    rangeKm = patch.rangeKm ?: rangeKm,
    powerKw = patch.powerKw ?: powerKw,
    charging = patch.charging ?: charging,
    chargePowerKw = patch.chargePowerKw ?: chargePowerKw,
    outsideTempC = patch.outsideTempC ?: outsideTempC,
    cabinTempC = patch.cabinTempC ?: cabinTempC,
    climateOn = patch.climateOn ?: climateOn,
    fanLevel = patch.fanLevel ?: fanLevel,
    driverTempC = patch.driverTempC ?: driverTempC,
    passengerTempC = patch.passengerTempC ?: passengerTempC,
    seatHeatDriver = patch.seatHeatDriver ?: seatHeatDriver,
    seatHeatPassenger = patch.seatHeatPassenger ?: seatHeatPassenger,
    odometerKm = patch.odometerKm ?: odometerKm,
    tripKm = patch.tripKm ?: tripKm,
    avgConsumptionKwh100 = patch.avgConsumptionKwh100 ?: avgConsumptionKwh100,
    tirePressures = patch.tirePressures ?: tirePressures,
    doors = patch.doors ?: doors,
    headlights = patch.headlights ?: headlights,
    highBeam = patch.highBeam ?: highBeam,
    parkingBrake = patch.parkingBrake ?: parkingBrake,
    seatbeltFastened = patch.seatbeltFastened ?: seatbeltFastened,
    turnSignal = patch.turnSignal ?: turnSignal,
    gps = patch.gps ?: gps,
    adas = patch.adas ?: adas,
    rawCanFrame = patch.rawCanFrame ?: rawCanFrame,
    lastUpdateMs = System.currentTimeMillis()
)

data class SourceStatus(
    val name: String,
    val connected: Boolean,
    val message: String = ""
) {
    fun summary(): String = if (connected) {
        "$name conectado"
    } else {
        "$name: ${message.ifBlank { "desconectado" }}"
    }

    companion object {
        fun mock(connected: Boolean = true) = SourceStatus(
            "Simulador",
            connected,
            if (connected) "Dados simulados em tempo real" else "Simulador parado"
        )
    }
}

fun Float.clamp(min: Float, max: Float): Float = coerceIn(min, max)

fun Double.formatCoordinate(): String = String.format(Locale.US, "%.5f", this)
