package com.carxust

import com.carxust.core.Formatters
import com.carxust.vehicle.CanDecoder
import com.carxust.vehicle.CanFrameParser
import com.carxust.vehicle.Gear
import com.carxust.vehicle.TurnSignal
import com.carxust.vehicle.VehiclePatch
import com.carxust.vehicle.VehicleState
import com.carxust.vehicle.applyPatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleCoreTest {

    @Test
    fun speed_conversion_is_correct() {
        assertEquals("100", Formatters.speed(100f, false))
        assertEquals("62", Formatters.speed(100f, true))
        assertEquals("km/h", Formatters.speedUnit(false))
        assertEquals("mph", Formatters.speedUnit(true))
    }

    @Test
    fun duration_formats_minutes_and_seconds() {
        assertEquals("0:00", Formatters.duration(0))
        assertEquals("1:05", Formatters.duration(65_000))
        assertEquals("10:00", Formatters.duration(600_000))
    }

    @Test
    fun patch_merges_only_present_fields() {
        val initial = VehicleState(speedKph = 10f, rpm = 1000, gear = Gear.DRIVE)
        val merged = initial.applyPatch(VehiclePatch(speedKph = 55f, turnSignal = TurnSignal.LEFT))
        assertEquals(55f, merged.speedKph)
        assertEquals(1000, merged.rpm)
        assertEquals(Gear.DRIVE, merged.gear)
        assertEquals(TurnSignal.LEFT, merged.turnSignal)
        assertEquals(initial.batteryPercent, merged.batteryPercent)
    }

    @Test
    fun can_hash_frame_is_parsed() {
        val frame = CanFrameParser.parse("(1700000000.123456) can0 100#0BB8000003000000")
        assertNotNull(frame)
        assertEquals(0x100, frame!!.id)
        assertEquals("can0", frame.interfaceName)
        assertEquals(8, frame.data.size)
        assertEquals(0x0B, frame.data[0])
        assertEquals(0xB8, frame.data[1])
    }

    @Test
    fun can_bracket_frame_is_parsed_without_timestamp() {
        val frame = CanFrameParser.parse("  can0  101   [8]  01 F4 00 64 00 0A 01 00")
        assertNotNull(frame)
        assertEquals(0x101, frame!!.id)
        assertEquals(8, frame.data.size)
    }

    @Test
    fun powertrain_frame_decodes_speed_and_gear() {
        val frame = CanFrameParser.parse("can0 100#27100FA003000000")
        assertNotNull(frame)
        val patch = CanDecoder.decode(frame!!)
        assertNotNull(patch)
        val decoded = patch!!
        assertEquals(100f, decoded.speedKph!!)
        assertEquals(4000, decoded.rpm)
        assertEquals(Gear.DRIVE, decoded.gear)
    }

    @Test
    fun tire_frame_decodes_pressures() {
        val frame = CanFrameParser.parse("can0 104#1E1D1C1B")
        assertNotNull(frame)
        val patch = CanDecoder.decode(frame!!)
        assertNotNull(patch)
        val decoded = patch!!
        assertEquals(5.0f, decoded.tirePressures!!.frontLeft)
        assertEquals(4.7f, decoded.tirePressures.rearRight)
    }

    @Test
    fun body_frame_decodes_turn_signal_and_doors() {
        val frame = CanFrameParser.parse("can0 103#0102020100")
        assertNotNull(frame)
        val patch = CanDecoder.decode(frame!!)
        assertNotNull(patch)
        val decoded = patch!!
        assertTrue(decoded.doors!!.frontLeft)
        assertEquals(TurnSignal.RIGHT, decoded.turnSignal)
        assertEquals(true, decoded.seatbeltFastened)
    }

    @Test
    fun energy_frame_decodes_soc_range_and_charging() {
        val frame = CanFrameParser.parse("can0 101#01F4006EFF9C0100")
        assertNotNull(frame)
        val decoded = CanDecoder.decode(frame!!)!!
        assertEquals(50f, decoded.batteryPercent!!)
        assertEquals(110f, decoded.rangeKm!!)
        assertEquals(-10f, decoded.powerKw!!)
        assertEquals(true, decoded.charging)
    }

    @Test
    fun climate_frame_decodes_temperatures_and_fan() {
        val frame = CanFrameParser.parse("can0 102#3C32000501")
        assertNotNull(frame)
        val decoded = CanDecoder.decode(frame!!)!!
        assertEquals(20f, decoded.outsideTempC!!)
        assertEquals(10f, decoded.cabinTempC!!)
        assertEquals(5, decoded.fanLevel)
        assertEquals(true, decoded.climateOn)
    }

    @Test
    fun adas_flags_are_decoded() {
        val frame = CanFrameParser.parse("can0 201#2D")
        assertNotNull(frame)
        val decoded = CanDecoder.decode(frame!!)!!
        val adas = decoded.adas!!
        assertTrue(adas.laneAssistActive)
        assertTrue(adas.collisionWarning)
        assertTrue(adas.cruiseActive)
        assertTrue(adas.blindSpotLeft)
        assertTrue(adas.blindSpotRight)
    }

    @Test
    fun unknown_frame_returns_null() {
        val frame = CanFrameParser.parse("can0 7FF#0102")
        assertNotNull(frame)
        assertEquals(null, CanDecoder.decode(frame!!))
    }

    @Test
    fun invalid_line_returns_null() {
        assertEquals(null, CanFrameParser.parse("not a can frame"))
        assertEquals(null, CanFrameParser.parse("can0 100#"))
    }

    @Test
    fun formatted_frame_can_be_parsed_again() {
        val frame = CanFrameParser.parse("can0 104#1E1D1C1B")!!
        val formatted = CanDecoder.format(frame)
        assertEquals("can0 104#1E1D1C1B", formatted)
        val reparsed = CanFrameParser.parse(formatted)!!
        assertEquals(frame.id, reparsed.id)
        assertEquals(frame.data.toList(), reparsed.data.toList())
    }
}
