package com.carxust.vehicle

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Formato CAN proprietário "CarXust spec v1" (500 kbit/s, 11-bit IDs).
 * Mapeie a sua ECU/MCU para estes IDs ou edite este decoder.
 *
 * 0x100  byte0-1 velocidade (uint16, 0.01 km/h) | byte2-3 RPM (uint16, 1 rpm)
 *        byte4 marcha (0=P,1=R,2=N,3=D,4=S) | byte5 freio de estacionamento
 * 0x101  byte0-1 SOC (uint16, 0.1 %) | byte2-3 autonomia (uint16, 1 km)
 *        byte4-5 potência (int16, 0.1 kW, positivo=consumo) | byte6 carregando
 * 0x102  byte0 temp externa (int8, offset -40) | byte1 temp cabine | byte2 temp bateria
 *        byte3 nível do ventilador (0-7) | byte4 clima ligado
 * 0x103  byte0 portas (bit0 FL, bit1 FR, bit2 RL, bit3 RR, bit4 porta-malas, bit5 capô)
 *        byte1 luzes (bit0 farol, bit1 alto) | byte2 seta (0=off,1=esq,2=dir,3=pisca)
 *        byte3 cinto (1=afivelado) | byte4 pressão pneus válida
 * 0x104  byte0-3 pressões (FL,FR,RL,RR em 0.1 bar acima de 2.0)
 * 0x200  byte0-1 offset de faixa (int16, 0.001 m) | byte2-3 distância do carro à frente (0.1 m)
 *        byte4-5 velocidade do carro à frente (0.1 km/h) | byte6-7 cruise set (0.1 km/h)
 * 0x201  byte0 flags ADAS: bit0 lane assist, bit1 carro à frente, bit2 colisão,
 *        bit3 cruise ativo, bit4 blind spot esquerdo, bit5 blind spot direito
 */
object CanDecoder {

    fun decode(frame: CanFrame): VehiclePatch? {
        val data = frame.data
        return when (frame.id) {
            0x100 -> decodePowertrain(data)
            0x101 -> decodeEnergy(data)
            0x102 -> decodeClimate(data)
            0x103 -> decodeBody(data)
            0x104 -> decodeTires(data)
            0x200 -> decodeAdas(data)
            0x201 -> decodeAdasFlags(data)
            else -> null
        }
    }

    private fun decodePowertrain(data: IntArray): VehiclePatch? {
        if (data.size < 6) return null
        val speed = u16(data, 0) / 100f
        val rpm = u16(data, 2)
        val gear = when (data[4] and 0x0F) {
            0 -> Gear.PARK
            1 -> Gear.REVERSE
            2 -> Gear.NEUTRAL
            3 -> Gear.DRIVE
            4 -> Gear.SPORT
            else -> Gear.DRIVE
        }
        return VehiclePatch(
            speedKph = speed,
            rpm = rpm,
            gear = gear,
            parkingBrake = data[5] != 0
        )
    }

    private fun decodeEnergy(data: IntArray): VehiclePatch? {
        if (data.size < 7) return null
        val soc = u16(data, 0) / 10f
        val range = u16(data, 2).toFloat()
        val power = s16(data, 4) / 10f
        return VehiclePatch(
            batteryPercent = soc,
            rangeKm = range,
            powerKw = power,
            charging = data[6] != 0
        )
    }

    private fun decodeClimate(data: IntArray): VehiclePatch? {
        if (data.size < 5) return null
        return VehiclePatch(
            outsideTempC = (data[0] - 40).toFloat(),
            cabinTempC = (data[1] - 40).toFloat(),
            fanLevel = data[3].coerceIn(0, 7),
            climateOn = data[4] != 0
        )
    }

    private fun decodeBody(data: IntArray): VehiclePatch? {
        if (data.size < 5) return null
        val doorBits = data[0]
        val lightBits = data[1]
        val turn = when (data[2] and 0x03) {
            1 -> TurnSignal.LEFT
            2 -> TurnSignal.RIGHT
            3 -> TurnSignal.HAZARD
            else -> TurnSignal.OFF
        }
        return VehiclePatch(
            doors = DoorsState(
                frontLeft = doorBits and 0x01 != 0,
                frontRight = doorBits and 0x02 != 0,
                rearLeft = doorBits and 0x04 != 0,
                rearRight = doorBits and 0x08 != 0,
                trunk = doorBits and 0x10 != 0,
                hood = doorBits and 0x20 != 0
            ),
            headlights = lightBits and 0x01 != 0,
            highBeam = lightBits and 0x02 != 0,
            turnSignal = turn,
            seatbeltFastened = data[3] != 0
        )
    }

    private fun decodeTires(data: IntArray): VehiclePatch? {
        if (data.size < 4) return null
        return VehiclePatch(
            tirePressures = TirePressures(
                frontLeft = 2.0f + data[0] / 10f,
                frontRight = 2.0f + data[1] / 10f,
                rearLeft = 2.0f + data[2] / 10f,
                rearRight = 2.0f + data[3] / 10f
            )
        )
    }

    private fun decodeAdas(data: IntArray): VehiclePatch? {
        if (data.size < 8) return null
        return VehiclePatch(
            adas = AdasState(
                laneOffsetMeters = s16(data, 0) / 1000f,
                leadDistanceMeters = u16(data, 2) / 10f,
                leadSpeedKph = u16(data, 4) / 10f,
                cruiseSetKph = (u16(data, 6) / 10f).roundToInt(),
                leadPresent = true
            )
        )
    }

    private fun decodeAdasFlags(data: IntArray): VehiclePatch? {
        if (data.isEmpty()) return null
        val flags = data[0]
        return VehiclePatch(
            adas = AdasState(
                laneAssistActive = flags and 0x01 != 0,
                leadPresent = flags and 0x02 != 0,
                collisionWarning = flags and 0x04 != 0,
                cruiseActive = flags and 0x08 != 0,
                blindSpotLeft = flags and 0x10 != 0,
                blindSpotRight = flags and 0x20 != 0
            )
        )
    }

    private fun u16(data: IntArray, offset: Int): Int =
        ((data[offset] and 0xFF) shl 8) or (data[offset + 1] and 0xFF)

    private fun s16(data: IntArray, offset: Int): Int {
        val raw = u16(data, offset)
        return if (raw and 0x8000 != 0) raw - 0x10000 else raw
    }

    fun format(frame: CanFrame): String =
        String.format(
            Locale.US,
            "%s %03X#%s",
            frame.interfaceName,
            frame.id,
            frame.data.joinToString("") { String.format(Locale.US, "%02X", it and 0xFF) }
        )
}

data class CanFrame(
    val interfaceName: String,
    val id: Int,
    val data: IntArray,
    val timestamp: Double = 0.0
)

object CanFrameParser {

    private val hashPattern = Regex(
        """(?:\(([\d.]+)\)\s+)?(\S+)\s+([0-9A-Fa-f]{3,8})#([0-9A-Fa-f]*)"""
    )
    private val bracketPattern = Regex(
        """(?:\(([\d.]+)\)\s+)?(\S+)\s+([0-9A-Fa-f]{3,8})\s+\[\d+\]\s+([0-9A-Fa-f ]+)"""
    )

    fun parse(line: String): CanFrame? {
        val hash = hashPattern.find(line)
        if (hash != null) {
            val dataHex = hash.groupValues[4]
            if (dataHex.isEmpty()) return null
            return CanFrame(
                interfaceName = hash.groupValues[2],
                id = hash.groupValues[3].toIntOrNull(16) ?: return null,
                data = hexToBytes(dataHex),
                timestamp = hash.groupValues[1].toDoubleOrNull() ?: 0.0
            )
        }
        val bracket = bracketPattern.find(line)
        if (bracket != null) {
            return CanFrame(
                interfaceName = bracket.groupValues[2],
                id = bracket.groupValues[3].toIntOrNull(16) ?: return null,
                data = hexToBytes(bracket.groupValues[4].replace(" ", "")),
                timestamp = bracket.groupValues[1].toDoubleOrNull() ?: 0.0
            )
        }
        return null
    }

    private fun hexToBytes(hex: String): IntArray {
        val clean = hex.filter { it.isLetterOrDigit() }
        val usable = clean.take(clean.length - (clean.length % 2))
        return usable.chunked(2).mapNotNull { it.toIntOrNull(16) }.toIntArray()
    }
}
