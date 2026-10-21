package com.carxust.vehicle

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Leitor SocketCAN. Requer root (su) ou uma ROM que conceda CAP_NET_RAW ao app.
 * Usa can-utils (candump) ou o helper /system/bin/carxust-can incluído na ROM.
 */
class CanSource(
    private val interfaceName: String,
    private val bitrate: Int,
    private val showRawFrames: Boolean
) : VehicleSource {

    override val name: String = "CAN"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var process: Process? = null
    private var adasState = AdasState()

    override fun start(onPatch: (VehiclePatch) -> Unit, onStatus: (SourceStatus) -> Unit) {
        stop()
        val iface = interfaceName.trim()
        if (!iface.matches(Regex("[A-Za-z0-9_.-]{1,16}"))) {
            onStatus(SourceStatus(name, false, "Interface CAN inválida"))
            return
        }

        job = scope.launch {
            onStatus(SourceStatus(name, false, "Configurando $iface a $bitrate kbit/s..."))
            configureInterface(iface)
            delay(250)

            val helper = File(HELPER_PATH)
            val candidates = buildList {
                if (helper.exists()) {
                    add(listOf(HELPER_PATH, iface, bitrate.toString()))
                    add(listOf("su", "-c", "$HELPER_PATH $iface $bitrate"))
                }
                add(listOf("su", "-c", "candump -L $iface"))
                add(listOf("candump", "-L", iface))
            }

            var connected = false
            for (command in candidates) {
                if (!isActive) break
                try {
                    val proc = ProcessBuilder(command)
                        .redirectErrorStream(true)
                        .start()
                    process = proc
                    onStatus(SourceStatus(name, false, "Aguardando frames de $iface..."))
                    val reader = BufferedReader(InputStreamReader(proc.inputStream))
                    var first: String?
                    do {
                        first = reader.readLine()
                        if (first == null) break
                        val frame = CanFrameParser.parse(first)
                        if (frame != null) {
                            connected = true
                            onStatus(
                                SourceStatus(name, true, "Recebendo frames de $iface")
                            )
                            val patch = CanDecoder.decode(frame)
                            if (patch != null) {
                                adasState = mergeAdas(frame.id, adasState, patch.adas)
                                onPatch(
                                    if (showRawFrames) {
                                        patch.copy(
                                            adas = adasState,
                                            rawCanFrame = CanDecoder.format(frame)
                                        )
                                    } else {
                                        patch.copy(adas = adasState)
                                    }
                                )
                            }
                            break
                        }
                    } while (first != null)

                    if (connected) {
                        readLoop(reader, onPatch, onStatus)
                    } else {
                        val error = runCatching {
                            reader.readText().take(120)
                        }.getOrDefault("")
                        onStatus(
                            SourceStatus(
                                name,
                                false,
                                error.ifBlank { "${command.last()} não disponível" }
                            )
                        )
                        proc.destroy()
                        process = null
                    }
                } catch (t: Throwable) {
                    onStatus(SourceStatus(name, false, t.message ?: "Falha no SocketCAN"))
                    process?.destroy()
                    process = null
                }
                if (connected) break
            }

            if (!connected && isActive) {
                onStatus(
                    SourceStatus(
                        name,
                        false,
                        "Sem acesso ao CAN. Instale can-utils ou use a ROM CarXust com root."
                    )
                )
            } else if (connected && isActive) {
                onStatus(SourceStatus(name, false, "Barramento CAN parou de responder"))
            }
        }
    }

    private suspend fun readLoop(
        reader: BufferedReader,
        onPatch: (VehiclePatch) -> Unit,
        onStatus: (SourceStatus) -> Unit
    ) {
        while (currentCoroutineContext().isActive) {
            val line = reader.readLine()
            if (line == null) break
            val frame = CanFrameParser.parse(line) ?: continue
            val patch = CanDecoder.decode(frame) ?: continue
            adasState = mergeAdas(frame.id, adasState, patch.adas)
            onPatch(
                if (showRawFrames) {
                    patch.copy(adas = adasState, rawCanFrame = CanDecoder.format(frame))
                } else {
                    patch.copy(adas = adasState)
                }
            )
        }
        onStatus(SourceStatus(name, false, "Conexão CAN encerrada"))
    }

    private fun mergeAdas(frameId: Int, previous: AdasState, incoming: AdasState?): AdasState {
        if (incoming == null) return previous
        return when (frameId) {
            0x200 -> previous.copy(
                laneOffsetMeters = incoming.laneOffsetMeters,
                leadDistanceMeters = incoming.leadDistanceMeters,
                leadSpeedKph = incoming.leadSpeedKph,
                cruiseSetKph = incoming.cruiseSetKph
            )
            0x201 -> previous.copy(
                laneAssistActive = incoming.laneAssistActive,
                leadPresent = incoming.leadPresent,
                collisionWarning = incoming.collisionWarning,
                cruiseActive = incoming.cruiseActive,
                blindSpotLeft = incoming.blindSpotLeft,
                blindSpotRight = incoming.blindSpotRight
            )
            else -> incoming
        }
    }

    private fun configureInterface(iface: String) {
        val commands = listOf(
            listOf("su", "-c", "ip link set $iface up type can bitrate $bitrate"),
            listOf("ip", "link", "set", iface, "up", "type", "can", "bitrate", bitrate.toString())
        )
        for (command in commands) {
            try {
                val proc = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
                val finished = proc.waitFor(2, TimeUnit.SECONDS)
                if (!finished) {
                    proc.destroy()
                } else if (proc.exitValue() == 0) {
                    return
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Falha ao configurar CAN: ${t.message}")
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        runCatching { process?.destroy() }
        process = null
    }

    private companion object {
        const val TAG = "CarXustCan"
        const val HELPER_PATH = "/system/bin/carxust-can"
    }
}
