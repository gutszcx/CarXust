package com.carxust.vehicle

import java.util.Locale

/**
 * Protocolo de telemetria entre o ESP32 (plugado no OBD2) e o painel.
 *
 * O firmware envia um quadro textual por vez, uma variável por linha:
 *
 * ```
 * HELLO
 * FW=1.0
 * CAR=Chevrolet Onix 1.4
 * SPEED=42.5
 * RPM=850
 * TPMS_FL=2.31
 * END
 * ```
 *
 * O quadro termina em `END`. Tudo que não vier no quadro significa que o
 * veículo não possui aquele sensor, e por isso ele não aparece no painel.
 *
 * Sem dependências de Android para poder ser testado na JVM.
 */
object Esp32Protocol {

    const val END = "END"
    const val HELLO = "HELLO"

    /** Interpreta uma linha `CHAVE=VALOR`. Devolve null para linhas vazias ou sem separador. */
    fun parseLine(line: String): Pair<String, String>? {
        val clean = line.trim()
        if (clean.isEmpty() || clean.startsWith("#")) return null
        val index = clean.indexOf('=')
        if (index <= 0) return null
        val key = clean.substring(0, index).trim().uppercase(Locale.US)
        val value = clean.substring(index + 1).trim()
        if (key.isEmpty()) return null
        return key to value
    }

    fun isEnd(line: String): Boolean = line.trim().uppercase(Locale.US) == END

    fun isHello(line: String): Boolean = line.trim().uppercase(Locale.US) == HELLO

    /** Converte o quadro recebido em um patch de estado do veículo. */
    fun toPatch(fields: Map<String, String>): VehiclePatch {
        fun num(key: String): Float? = fields[key]?.trim()?.toFloatOrNull()
        fun int(key: String): Int? = fields[key]?.trim()?.toFloatOrNull()?.toInt()
        fun bool(key: String): Boolean? = when (fields[key]?.trim()?.lowercase(Locale.US)) {
            "1", "true", "on", "yes", "ligado" -> true
            "0", "false", "off", "no", "desligado" -> false
            else -> null
        }

        val speed = num("SPEED") ?: num("VSS")
        val rpm = int("RPM")
        val coolant = num("COOLANT")
        val fuel = num("FUEL")
        val odometer = num("ODO")
        val batteryPercent = num("BATT")
        val outsideTemp = num("AMBIENT")
        val oilPressure = num("OILP")
        val vss = num("VSS") ?: speed
        val trip = num("TRIP")
        val consumption = num("CONSUMPTION")

        val mil = bool("MIL")
        val lambda = num("LAMBDA")
        val maf = num("MAF")
        val map = num("MAP")
        val injection = group(mil, lambda, maf, map) {
            InjectionData(
                checkEngine = mil,
                lambda = lambda,
                mafGramsPerSec = maf,
                mapKpa = map
            )
        }

        val wheelFl = num("WHEEL_FL")
        val wheelFr = num("WHEEL_FR")
        val wheelRl = num("WHEEL_RL")
        val wheelRr = num("WHEEL_RR")
        val abs = group(wheelFl, wheelFr, wheelRl, wheelRr) {
            AbsData(
                frontLeftKph = wheelFl,
                frontRightKph = wheelFr,
                rearLeftKph = wheelRl,
                rearRightKph = wheelRr
            )
        }

        val yaw = num("YAW")
        val lateral = num("LATACC")
        val esp = group(yaw, lateral) {
            EspData(yawRateDps = yaw, lateralAccel = lateral)
        }

        val tpmsFl = num("TPMS_FL")
        val tpmsFr = num("TPMS_FR")
        val tpmsRl = num("TPMS_RL")
        val tpmsRr = num("TPMS_RR")
        val tires = group(tpmsFl, tpmsFr, tpmsRl, tpmsRr) {
            TirePressures(
                frontLeft = tpmsFl ?: 0f,
                frontRight = tpmsFr ?: 0f,
                rearLeft = tpmsRl ?: 0f,
                rearRight = tpmsRr ?: 0f
            )
        }

        val padFront = num("PAD_FRONT")
        val padRear = num("PAD_REAR")
        val brakePadData = group(padFront, padRear) {
            BrakePadData(frontPercent = padFront, rearPercent = padRear)
        }

        val steerAngle = num("STEER_ANGLE")
        val steerTorque = num("STEER_TORQUE")
        val steering = group(steerAngle, steerTorque) {
            SteeringData(angleDeg = steerAngle, torqueNm = steerTorque)
        }

        val beltDriver = bool("SEATBELT_DRIVER")
        val beltPassenger = bool("SEATBELT_PASS")
        val seatbelts = group(beltDriver, beltPassenger) {
            SeatbeltsData(driver = beltDriver, passenger = beltPassenger)
        }

        val doorFl = bool("DOOR_FL")
        val doorFr = bool("DOOR_FR")
        val doorRl = bool("DOOR_RL")
        val doorRr = bool("DOOR_RR")
        val trunk = bool("TRUNK")
        val hood = bool("HOOD")
        val openings = group(doorFl, doorFr, doorRl, doorRr, trunk, hood) {
            DoorsState(
                frontLeft = doorFl ?: false,
                frontRight = doorFr ?: false,
                rearLeft = doorRl ?: false,
                rearRight = doorRr ?: false,
                trunk = trunk ?: false,
                hood = hood ?: false
            )
        }

        val volts = num("VOLT")
        val charging = bool("CHG")
        val battery = group(batteryPercent, volts, charging) {
            BatteryData(percent = batteryPercent, volts = volts, charging = charging)
        }

        val lights = bool("LIGHTS")
        val highBeam = bool("HIGHBEAM")
        val rain = bool("RAIN")
        val wiper = bool("WIPER")
        val lightRain = group(lights, highBeam, rain, wiper) {
            LightRainData(lights = lights, highBeam = highBeam, rain = rain, wiper = wiper)
        }

        val parkFl = num("PARK_FL")
        val parkFr = num("PARK_FR")
        val parkRl = num("PARK_RL")
        val parkRr = num("PARK_RR")
        val parking = group(parkFl, parkFr, parkRl, parkRr) {
            ParkingData(
                frontLeftCm = parkFl,
                frontRightCm = parkFr,
                rearLeftCm = parkRl,
                rearRightCm = parkRr
            )
        }

        val blindLeft = bool("BSL")
        val blindRight = bool("BSR")
        val laneVisible = bool("LANE")
        val laneOffset = num("LANE_OFFSET")
        val leadPresent = bool("LEAD")
        val leadDistance = num("LEAD_DIST")
        val leadSpeed = num("LEAD_SPEED")
        val assist = group(
            blindLeft,
            blindRight,
            laneVisible,
            laneOffset,
            leadPresent,
            leadDistance,
            leadSpeed
        ) {
            AssistData(
                blindSpotLeft = blindLeft,
                blindSpotRight = blindRight,
                laneVisible = laneVisible,
                laneOffsetM = laneOffset,
                leadPresent = leadPresent,
                leadDistanceM = leadDistance,
                leadSpeedKph = leadSpeed
            )
        }

        val fluidBrake = num("FLUID_BRAKE")
        val fluidCoolant = num("FLUID_COOL")
        val fluidWasher = num("FLUID_WASHER")
        val fluids = group(fluidBrake, fluidCoolant, fluidWasher) {
            FluidLevels(
                brakePercent = fluidBrake,
                coolantPercent = fluidCoolant,
                washerPercent = fluidWasher
            )
        }

        val airbag = bool("AIRBAG")
        val readings = SensorReadings(
            coolantC = coolant,
            oilPressureBar = oilPressure,
            fuelPercent = fuel,
            injection = injection,
            vssKph = vss,
            rpm = rpm,
            odometerKm = odometer,
            abs = abs,
            esp = esp,
            tires = tires,
            brakePads = brakePadData,
            steering = steering,
            airbagTriggered = airbag,
            seatbelts = seatbelts,
            openings = openings,
            battery = battery,
            lightRain = lightRain,
            parking = parking,
            assist = assist,
            fluids = fluids
        )

        val adas = assist?.let { assistData ->
            AdasState(
                laneAssistActive = assistData.laneVisible ?: true,
                laneOffsetMeters = assistData.laneOffsetM ?: 0.05f,
                leadDistanceMeters = assistData.leadDistanceM ?: 42f,
                leadSpeedKph = assistData.leadSpeedKph ?: 58f,
                leadPresent = assistData.leadPresent ?: false,
                cruiseActive = false,
                blindSpotLeft = assistData.blindSpotLeft ?: false,
                blindSpotRight = assistData.blindSpotRight ?: false
            )
        }

        return VehiclePatch(
            speedKph = speed,
            rpm = rpm,
            gear = parseGear(fields["GEAR"]),
            batteryPercent = batteryPercent,
            outsideTempC = outsideTemp,
            odometerKm = odometer,
            tripKm = trip,
            avgConsumptionKwh100 = consumption,
            tirePressures = readings.tires,
            doors = readings.openings,
            headlights = lights,
            highBeam = highBeam,
            parkingBrake = bool("PARKBRAKE"),
            seatbeltFastened = beltDriver,
            turnSignal = parseTurnSignal(fields["TURN"]),
            adas = adas,
            sensors = readings,
            rawCanFrame = describe(fields)
        )
    }

    /** Resumo curto do quadro, exibido no diagnóstico bruto. */
    fun describe(fields: Map<String, String>): String {
        val parts = listOf(
            "SPEED" to "km/h",
            "RPM" to "rpm",
            "COOLANT" to "°C",
            "FUEL" to "%",
            "ODO" to "km"
        ).mapNotNull { (key, unit) ->
            fields[key]?.takeIf { it.isNotBlank() }?.let { "$key=$it$unit" }
        }
        val extra = fields.size - parts.size
        return if (extra > 0) parts.joinToString(" ") + " (+$extra)" else parts.joinToString(" ")
    }

    private fun parseGear(raw: String?): Gear? = when (raw?.trim()?.uppercase(Locale.US)) {
        "P", "PARK" -> Gear.PARK
        "R", "REVERSE" -> Gear.REVERSE
        "N", "NEUTRAL" -> Gear.NEUTRAL
        "D", "DRIVE" -> Gear.DRIVE
        "S", "SPORT" -> Gear.SPORT
        else -> null
    }

    private fun parseTurnSignal(raw: String?): TurnSignal? = when (raw?.trim()?.uppercase(Locale.US)) {
        "OFF", "0" -> TurnSignal.OFF
        "LEFT", "1" -> TurnSignal.LEFT
        "RIGHT", "2" -> TurnSignal.RIGHT
        "HAZARD", "3" -> TurnSignal.HAZARD
        else -> null
    }

    /**
     * Monta um grupo de sensores apenas quando ao menos um dos campos existe.
     * Grupo ausente significa que o veículo não tem aquele sensor.
     */
    private inline fun <T> group(vararg values: Any?, factory: () -> T): T? {
        if (values.all { it == null }) return null
        return factory()
    }
}
