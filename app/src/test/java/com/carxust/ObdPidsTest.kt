package com.carxust

import com.carxust.vehicle.ObdPids
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObdPidsTest {

    @Test
    fun rpm_is_decoded_from_standard_response() {
        assertEquals(1726, ObdPids.rpm("41 0C 1A F8\r\r>"))
        assertEquals(1726, ObdPids.rpm("010C\r41 0C 1A F8\r\r>"))
    }

    @Test
    fun speed_and_throttle_are_decoded() {
        assertEquals(50f, ObdPids.speedKph("41 0D 32\r\r>")!!, 0.001f)
        assertEquals(100f, ObdPids.throttlePercent("41 11 FF\r\r>")!!, 0.001f)
    }

    @Test
    fun temperatures_and_voltage_are_decoded() {
        assertEquals(83f, ObdPids.coolantCelsius("41 05 7B\r\r>")!!, 0.001f)
        assertEquals(60f, ObdPids.ambientCelsius("41 46 64\r\r>")!!, 0.001f)
        assertEquals(12.345f, ObdPids.moduleVoltage("41 42 30 39\r\r>")!!, 0.001f)
    }

    @Test
    fun fuel_and_battery_life_are_percentages() {
        assertEquals(50.2f, ObdPids.fuelPercent("41 2F 80\r\r>")!!, 0.1f)
        assertEquals(80f, ObdPids.batteryLifePercent("41 5B 50\r\r>")!!, 0.001f)
    }

    @Test
    fun missing_or_invalid_responses_return_null() {
        assertNull(ObdPids.rpm("NO DATA\r\r>"))
        assertNull(ObdPids.speedKph("SEARCHING...\r\r>"))
        assertNull(ObdPids.moduleVoltage("41 42 30"))
        assertNull(ObdPids.batteryLifePercent("UNABLE TO CONNECT\r\r>"))
    }

    @Test
    fun negative_response_returns_null() {
        assertNull(ObdPids.rpm("7F 01 12\r\r>"))
    }
}
