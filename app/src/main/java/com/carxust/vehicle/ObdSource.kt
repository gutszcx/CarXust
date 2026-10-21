package com.carxust.vehicle

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * ELM327 / STN1110 via Bluetooth SPP.
 *
 * Supported PIDs: 0C (RPM), 0D (speed), 05 (coolant), 11 (throttle),
 * 2F (fuel level), 42 (module voltage), 46 (ambient temp), 5B (battery).
 */
class ObdSource(
    private val context: Context,
    private val macAddress: String
) : VehicleSource {

    override val name: String = "OBD-II"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    override fun start(onPatch: (VehiclePatch) -> Unit, onStatus: (SourceStatus) -> Unit) {
        stop()
        job = scope.launch {
            if (macAddress.isBlank()) {
                onStatus(SourceStatus(name, false, "Selecione um adaptador Bluetooth nas configurações"))
                return@launch
            }
            if (!hasBluetoothPermission()) {
                onStatus(SourceStatus(name, false, "Permissão de Bluetooth não concedida"))
                return@launch
            }

            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null || !adapter.isEnabled) {
                onStatus(SourceStatus(name, false, "Bluetooth desligado"))
                return@launch
            }

            while (isActive) {
                try {
                    connect(adapter)
                    onStatus(SourceStatus(name, true, "ELM327 conectado em $macAddress"))
                    initialize()
                    pollLoop(onPatch, onStatus)
                } catch (t: Throwable) {
                    onStatus(
                        SourceStatus(
                            name,
                            false,
                            t.message?.take(60) ?: "Falha de conexão"
                        )
                    )
                } finally {
                    closeQuietly()
                }
                if (isActive) delay(5_000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(adapter: BluetoothAdapter) {
        val device = adapter.getRemoteDevice(macAddress)
        val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        runCatching { adapter.cancelDiscovery() }
        socket.connect()
        this.socket = socket
        input = socket.inputStream
        output = socket.outputStream
    }

    private suspend fun initialize() {
        listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0").forEach { command ->
            sendCommand(command, timeoutMs = 1_200)
            delay(120)
        }
    }

    private suspend fun pollLoop(
        onPatch: (VehiclePatch) -> Unit,
        onStatus: (SourceStatus) -> Unit
    ) {
        var lastThrottle = 0f
        var failures = 0
        while (currentCoroutineContext().isActive) {
            try {
                val rpmResponse = sendCommand("010C")
                val speedResponse = sendCommand("010D")
                val coolant = sendCommand("0105")
                val throttle = sendCommand("0111")
                val voltage = sendCommand("0142")
                val ambient = sendCommand("0146")
                val fuel = sendCommand("012F")
                val batteryLife = sendCommand("015B")

                val rpm = ObdPids.rpm(rpmResponse)
                val speed = ObdPids.speedKph(speedResponse)
                val throttleValue = ObdPids.throttlePercent(throttle)
                if (throttleValue != null) lastThrottle = throttleValue

                val coolantTemp = ObdPids.coolantCelsius(coolant)
                val ambientTemp = ObdPids.ambientCelsius(ambient)
                val volts = ObdPids.moduleVoltage(voltage)
                val fuelLevel = ObdPids.fuelPercent(fuel)
                val batteryPercent = ObdPids.batteryLifePercent(batteryLife)

                if (rpm == null && speed == null) {
                    failures++
                    if (failures >= 8) {
                        throw IllegalStateException("Sem resposta do ELM327")
                    }
                } else {
                    failures = 0
                }

                val speedKph = speed ?: 0f
                val gear = when {
                    speedKph > 2f -> Gear.DRIVE
                    rpm != null && rpm > 300 -> Gear.NEUTRAL
                    else -> Gear.PARK
                }
                val load = (lastThrottle / 100f).coerceIn(0f, 1f)
                val estimatedPower = if (speedKph > 2f) {
                    (3f + speedKph * 0.11f + load * 22f).coerceIn(2f, 160f)
                } else {
                    0.3f + load * 2f
                }

                val patch = VehiclePatch(
                    speedKph = speedKph,
                    rpm = rpm,
                    gear = gear,
                    powerKw = estimatedPower,
                    outsideTempC = ambientTemp,
                    batteryPercent = batteryPercent,
                    headlights = null,
                    tirePressures = null,
                    rawCanFrame = buildString {
                        append("OBD 010C=$rpm rpm 010D=$speed km/h 0142=$volts V")
                        if (coolantTemp != null) append(" 0105=$coolantTemp °C")
                        if (fuelLevel != null) append(" 012F=${fuelLevel.toInt()} %")
                        if (batteryPercent != null) append(" 015B=${batteryPercent.toInt()} %")
                    }
                )
                onPatch(patch)
                onStatus(SourceStatus(name, true, "ELM327 ativo • ${volts ?: 0f} V"))
                delay(220)
            } catch (t: Throwable) {
                onStatus(SourceStatus(name, false, t.message ?: "Leitura interrompida"))
                throw t
            }
        }
    }

    private suspend fun sendCommand(command: String, timeoutMs: Long = 2_000): String {
        val out = output ?: throw IllegalStateException("OBD desconectado")
        val inp = input ?: throw IllegalStateException("OBD desconectado")
        out.write("$command\r".toByteArray(Charsets.US_ASCII))
        out.flush()

        val builder = StringBuilder()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val available = inp.available()
            if (available > 0) {
                val buffer = ByteArray(available)
                val read = inp.read(buffer)
                if (read > 0) {
                    builder.append(String(buffer, 0, read, Charsets.US_ASCII))
                    if (builder.contains('>')) break
                }
            } else {
                delay(15)
            }
        }
        return builder.toString()
    }

    private fun hasBluetoothPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            Manifest.permission.BLUETOOTH
        }
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun closeQuietly() {
        runCatching { input?.close() }
        runCatching { output?.close() }
        runCatching { socket?.close() }
        input = null
        output = null
        socket = null
    }

    override fun stop() {
        job?.cancel()
        job = null
        closeQuietly()
    }

    private companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
