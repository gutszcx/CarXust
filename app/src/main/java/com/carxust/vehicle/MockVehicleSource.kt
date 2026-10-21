package com.carxust.vehicle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

class MockVehicleSource(
    initialOdometerKm: Float = 18_432f,
    initialTripKm: Float = 128.4f,
    initialBatteryPercent: Float = 78f
) : VehicleSource {

    override val name: String = "Simulador"

    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var odometer = initialOdometerKm.toDouble()
    private var trip = initialTripKm.toDouble()
    private var battery = initialBatteryPercent.toDouble()
    private var speed = 0.0
    private var accel = 0.0
    private var startedAt = 0L

    override fun start(onPatch: (VehiclePatch) -> Unit, onStatus: (SourceStatus) -> Unit) {
        stop()
        startedAt = System.currentTimeMillis()
        onStatus(SourceStatus.mock(true))
        job = scope.launch {
            var last = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                val dt = ((now - last).coerceIn(20L, 1000L)) / 1000.0
                last = now
                val t = (now - startedAt) / 1000.0

                val targetSpeed = simulatedTargetSpeed(t)
                val desiredAccel = ((targetSpeed - speed) * 0.55).coerceIn(-4.5, 3.2)
                accel += (desiredAccel - accel) * 0.08
                speed = (speed + accel * dt).coerceIn(0.0, 145.0)
                if (speed < 0.4 && targetSpeed <= 0.0) speed = 0.0

                odometer += speed * dt / 3600.0
                trip += speed * dt / 3600.0
                battery = (battery - (abs(accel) * 0.00012 + speed * 0.0000045) * dt)
                    .coerceIn(8.0, 100.0)

                val gear = when {
                    speed < 0.5 && t % 95.0 > 85.0 -> Gear.PARK
                    speed < 0.5 -> Gear.NEUTRAL
                    else -> Gear.DRIVE
                }

                val turn = when {
                    t % 120.0 in 48.0..53.0 -> TurnSignal.LEFT
                    t % 120.0 in 96.0..101.0 -> TurnSignal.RIGHT
                    t % 300.0 in 250.0..253.0 -> TurnSignal.HAZARD
                    else -> TurnSignal.OFF
                }

                val power = when {
                    gear == Gear.PARK -> 0.4
                    accel < -0.4 -> (accel * 42.0).coerceIn(-75.0, -2.0)
                    else -> (5.0 + speed * 0.16 + accel * 9.0).coerceIn(1.0, 180.0)
                }

                val laneOffset = (sin(t / 2.3) * 0.28 + sin(t / 7.1) * 0.08).toFloat()
                val leadDistance = (34.0 + sin(t / 11.0) * 20.0).toFloat()
                val cruise = t % 180.0 in 20.0..140.0

                onPatch(
                    VehiclePatch(
                        speedKph = speed.toFloat(),
                        rpm = (780 + speed * 34 + accel * 120).toInt().coerceAtLeast(0),
                        gear = gear,
                        batteryPercent = battery.toFloat(),
                        rangeKm = (battery * 4.42).toFloat(),
                        powerKw = power.toFloat(),
                        charging = false,
                        outsideTempC = (23.5 + sin(t / 40.0) * 2.0).toFloat(),
                        odometerKm = odometer.toFloat(),
                        tripKm = trip.toFloat(),
                        avgConsumptionKwh100 = (14.4 + sin(t / 50.0) * 1.2).toFloat(),
                        tirePressures = TirePressures(
                            frontLeft = (2.42 + sin(t / 33.0) * 0.02).toFloat(),
                            frontRight = (2.40 + sin(t / 37.0) * 0.02).toFloat(),
                            rearLeft = (2.35 + sin(t / 41.0) * 0.02).toFloat(),
                            rearRight = (2.36 + sin(t / 43.0) * 0.02).toFloat()
                        ),
                        doors = DoorsState(),
                        headlights = t % 240.0 > 180.0,
                        parkingBrake = gear == Gear.PARK,
                        seatbeltFastened = true,
                        turnSignal = turn,
                        gps = GpsState(
                            latitude = -23.5505 + (odometer % 10.0) * 0.00042,
                            longitude = -46.6333 + (odometer % 10.0) * 0.00031,
                            altitudeMeters = 760.0 + sin(t / 18.0) * 8.0,
                            heading = (t * 3.0 % 360.0).toFloat(),
                            accuracyMeters = 3.5f,
                            hasFix = true
                        ),
                        adas = AdasState(
                            laneAssistActive = true,
                            laneOffsetMeters = laneOffset,
                            laneWidthMeters = 3.5f,
                            leadDistanceMeters = leadDistance,
                            leadSpeedKph = (speed * 0.82 + 8.0).toFloat(),
                            leadPresent = leadDistance < 48f,
                            collisionWarning = leadDistance < 14f && speed > 55,
                            cruiseActive = cruise,
                            cruiseSetKph = 110,
                            blindSpotLeft = t % 90.0 in 40.0..46.0,
                            blindSpotRight = t % 90.0 in 70.0..74.0
                        )
                    )
                )
                delay(100)
            }
        }
    }

    private fun simulatedTargetSpeed(t: Double): Double {
        val phase = t % 170.0
        return when {
            phase < 8.0 -> 0.0
            phase < 22.0 -> 32.0
            phase < 55.0 -> 68.0
            phase < 95.0 -> 104.0
            phase < 125.0 -> 58.0
            phase < 150.0 -> 24.0
            else -> 0.0
        } + sin(t / 5.0) * 2.5
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    override fun resetTrip() {
        trip = 0.0
    }
}
