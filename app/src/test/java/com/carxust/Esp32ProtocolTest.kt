package com.carxust

import com.carxust.vehicle.Esp32Protocol
import com.carxust.vehicle.Gear
import com.carxust.vehicle.SensorGroup
import com.carxust.vehicle.isAvailable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Esp32ProtocolTest {

    private fun frame(vararg lines: String): Map<String, String> {
        val fields = linkedMapOf<String, String>()
        lines.forEach { line ->
            Esp32Protocol.parseLine(line)?.let { fields[it.first] = it.second }
        }
        return fields
    }

    @Test
    fun parsesKeyValueLines() {
        val parsed = Esp32Protocol.parseLine("SPEED=42.5")
        assertNotNull(parsed)
        assertEquals("SPEED", parsed?.first)
        assertEquals("42.5", parsed?.second)
    }

    @Test
    fun ignoresEmptyAndCommentLines() {
        assertNull(Esp32Protocol.parseLine(""))
        assertNull(Esp32Protocol.parseLine("   "))
        assertNull(Esp32Protocol.parseLine("# comentario"))
        assertNull(Esp32Protocol.parseLine("sem separador"))
    }

    @Test
    fun detectsFrameBoundaries() {
        assertTrue(Esp32Protocol.isEnd("END"))
        assertTrue(Esp32Protocol.isEnd(" end "))
        assertTrue(Esp32Protocol.isHello("HELLO"))
        assertFalse(Esp32Protocol.isHello("SPEED=1"))
    }

    @Test
    fun mapsCoreVehicleValues() {
        val patch = Esp32Protocol.toPatch(
            frame(
                "SPEED=60",
                "RPM=2500",
                "GEAR=D",
                "COOLANT=88.5",
                "FUEL=62",
                "ODO=12345.6",
                "BATT=13.8"
            )
        )

        assertEquals(60f, patch.speedKph!!, 0.01f)
        assertEquals(2500, patch.rpm!!)
        assertEquals(Gear.DRIVE, patch.gear)
        assertEquals(88.5f, patch.sensors!!.coolantC!!, 0.01f)
        assertEquals(12345.6f, patch.odometerKm!!, 0.01f)
        assertEquals(62f, patch.sensors!!.fuelPercent!!, 0.01f)
    }

    @Test
    fun groupsAbsentWhenSensorIsMissing() {
        val patch = Esp32Protocol.toPatch(frame("SPEED=10"))

        assertEquals(10f, patch.speedKph!!, 0.01f)
        assertNull(patch.sensors!!.coolantC)
        assertNull(patch.sensors!!.fuelPercent)
        assertNull(patch.sensors!!.tires)
        assertNull(patch.sensors!!.abs)
        assertEquals(1, patch.sensors!!.availableGroups)
    }

    @Test
    fun countsOnlyGroupsPresentInTheCar() {
        val patch = Esp32Protocol.toPatch(
            frame(
                "COOLANT=90",
                "FUEL=40",
                "TPMS_FL=2.4",
                "TPMS_FR=2.4",
                "TPMS_RL=2.3",
                "TPMS_RR=2.3"
            )
        )
        val readings = patch.sensors!!

        assertTrue(SensorGroup.COOLANT.isAvailable(readings))
        assertTrue(SensorGroup.FUEL.isAvailable(readings))
        assertTrue(SensorGroup.TIRES.isAvailable(readings))
        assertFalse(SensorGroup.ESP.isAvailable(readings))
        assertFalse(SensorGroup.AIRBAG.isAvailable(readings))
        assertEquals(3, readings.availableGroups)
    }

    @Test
    fun mapsOpeningsAndSeatbelts() {
        val patch = Esp32Protocol.toPatch(
            frame(
                "DOOR_FL=1",
                "TRUNK=0",
                "HOOD=1",
                "SEATBELT_DRIVER=1",
                "SEATBELT_PASS=0"
            )
        )
        val readings = patch.sensors!!

        assertTrue(readings.openings!!.frontLeft)
        assertTrue(readings.openings!!.hood)
        assertFalse(readings.openings!!.trunk)
        assertTrue(readings.seatbelts!!.driver!!)
        assertFalse(readings.seatbelts!!.passenger!!)
    }

    @Test
    fun mapsAssistAndEsp() {
        val patch = Esp32Protocol.toPatch(
            frame(
                "YAW=1.5",
                "LATACC=0.3",
                "BSL=1",
                "LANE=1",
                "LANE_OFFSET=0.08",
                "LEAD=1",
                "LEAD_DIST=42",
                "LEAD_SPEED=58"
            )
        )
        val readings = patch.sensors!!

        assertEquals(1.5f, readings.esp!!.yawRateDps!!, 0.01f)
        assertTrue(readings.assist!!.blindSpotLeft!!)
        assertTrue(readings.assist!!.laneVisible!!)
        assertEquals(42f, readings.assist!!.leadDistanceM!!, 0.01f)
        assertTrue(patch.adas!!.leadPresent)
    }

    @Test
    fun mapsInjectionGroup() {
        val patch = Esp32Protocol.toPatch(
            frame("MIL=1", "LAMBDA=0.98", "MAF=12.5", "MAP=32")
        )
        val injection = patch.sensors!!.injection!!

        assertTrue(injection.checkEngine!!)
        assertEquals(0.98f, injection.lambda!!, 0.001f)
        assertEquals(12.5f, injection.mafGramsPerSec!!, 0.01f)
        assertEquals(32f, injection.mapKpa!!, 0.01f)
    }

    @Test
    fun mapsFluidsAndParking() {
        val patch = Esp32Protocol.toPatch(
            frame(
                "FLUID_BRAKE=80",
                "FLUID_COOL=95",
                "FLUID_WASHER=40",
                "PARK_FL=45",
                "PARK_RR=120"
            )
        )
        val readings = patch.sensors!!

        assertEquals(80f, readings.fluids!!.brakePercent!!, 0.01f)
        assertEquals(45f, readings.parking!!.frontLeftCm!!, 0.01f)
        assertNull(readings.parking!!.frontRightCm)
    }
}
