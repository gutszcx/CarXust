package com.carxust.vehicle

import android.content.Context
import com.carxust.core.CarXustPrefs
import com.carxust.core.VehicleSourceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class VehicleRepository(
    context: Context,
    private val prefs: CarXustPrefs
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gps = GpsSource(appContext)

    private val _state = MutableStateFlow(VehicleState())
    val state: StateFlow<VehicleState> = _state.asStateFlow()

    private val _status = MutableStateFlow(SourceStatus.mock())
    val status: StateFlow<SourceStatus> = _status.asStateFlow()

    private var source: VehicleSource? = null
    private var lastSourceConfig: SourceConfig? = null
    private val generation = AtomicInteger(0)

    init {
        startGps()
        scope.launch {
            prefs.config
                .map {
                    SourceConfig(
                        type = it.vehicleSource,
                        obdMac = it.obdMac,
                        canInterface = it.canInterface,
                        canBitrate = it.canBitrate,
                        rawCan = it.showRawCan,
                        esp32Host = it.esp32Host,
                        esp32Port = it.esp32Port
                    )
                }
                .distinctUntilChanged()
                .collectLatest { config -> restartSource(config) }
        }
    }

    private fun restartSource(config: SourceConfig) {
        if (config == lastSourceConfig && source != null) return
        val token = generation.incrementAndGet()
        lastSourceConfig = config
        source?.stop()
        source = null
        val current = _state.value
        val next = when (config.type) {
            VehicleSourceType.MOCK -> MockVehicleSource(
                initialOdometerKm = current.odometerKm,
                initialTripKm = current.tripKm,
                initialBatteryPercent = current.batteryPercent
            )
            VehicleSourceType.OBD -> ObdSource(appContext, config.obdMac)
            VehicleSourceType.CAN -> CanSource(
                interfaceName = config.canInterface,
                bitrate = config.canBitrate,
                showRawFrames = config.rawCan
            )
            VehicleSourceType.CAR_PROPERTY -> CarPropertySource(appContext)
            VehicleSourceType.ESP32 -> Esp32Source(config.esp32Host, config.esp32Port)
        }
        _status.value = SourceStatus(next.name, false, "Iniciando...")
        _state.update {
            it.copy(sourceLabel = next.name, sourceConnected = false, sourceMessage = "Iniciando...")
        }
        source = next
        next.start(
            onPatch = { patch ->
                if (generation.get() == token) {
                    _state.update { current -> current.applyPatch(patch) }
                }
            },
            onStatus = { status ->
                if (generation.get() == token) {
                    _status.value = status
                    _state.update {
                        it.copy(
                            sourceLabel = status.name,
                            sourceConnected = status.connected,
                            sourceMessage = status.message
                        )
                    }
                }
            }
        )
    }

    fun startGps() {
        runCatching {
            gps.start { patch -> _state.update { it.applyPatch(patch) } }
        }
    }

    fun restart() {
        lastSourceConfig = null
        restartSource(
            SourceConfig(
                type = prefs.config.value.vehicleSource,
                obdMac = prefs.config.value.obdMac,
                canInterface = prefs.config.value.canInterface,
                canBitrate = prefs.config.value.canBitrate,
                rawCan = prefs.config.value.showRawCan,
                esp32Host = prefs.config.value.esp32Host,
                esp32Port = prefs.config.value.esp32Port
            )
        )
    }

    fun resetTrip() {
        source?.resetTrip()
        _state.update { it.copy(tripKm = 0f) }
    }

    fun setClimate(on: Boolean) {
        _state.update { it.copy(climateOn = on, cabinTempC = if (on) it.driverTempC else it.outsideTempC) }
    }

    fun setFan(level: Int) {
        _state.update { it.copy(fanLevel = level.coerceIn(0, 7)) }
    }

    fun setDriverTemp(value: Float) {
        _state.update { it.copy(driverTempC = value.coerceIn(16f, 28f)) }
    }

    fun setPassengerTemp(value: Float) {
        _state.update { it.copy(passengerTempC = value.coerceIn(16f, 28f)) }
    }

    fun setSeatHeat(driver: Int? = null, passenger: Int? = null) {
        _state.update {
            it.copy(
                seatHeatDriver = (driver ?: it.seatHeatDriver).coerceIn(0, 3),
                seatHeatPassenger = (passenger ?: it.seatHeatPassenger).coerceIn(0, 3)
            )
        }
    }

    fun toggleHeadlights() {
        _state.update { it.copy(headlights = !it.headlights) }
    }

    fun stop() {
        generation.incrementAndGet()
        source?.stop()
        source = null
        gps.stop()
    }

    private data class SourceConfig(
        val type: VehicleSourceType,
        val obdMac: String,
        val canInterface: String,
        val canBitrate: Int,
        val rawCan: Boolean,
        val esp32Host: String,
        val esp32Port: Int
    )
}
