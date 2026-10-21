package com.carxust.vehicle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Telemetria vinda do ESP32 plugado no conector OBD2.
 *
 * O ESP32 lê o barramento (ELM327 e/ou CAN) e publica quadros de texto
 * `CHAVE=VALOR` por TCP. O painel conecta como cliente, o que funciona
 * tanto no ponto de acesso do próprio ESP32 quanto numa rede Wi-Fi do carro.
 */
class Esp32Source(
    private val host: String,
    private val port: Int
) : VehicleSource {

    override val name: String = "ESP32"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var socket: Socket? = null

    override fun start(onPatch: (VehiclePatch) -> Unit, onStatus: (SourceStatus) -> Unit) {
        stop()
        if (host.isBlank()) {
            onStatus(SourceStatus(name, false, "Informe o endereço IP do ESP32 em Ajustes"))
            return
        }

        job = scope.launch {
            var backoff = 1_000L
            while (isActive) {
                try {
                    onStatus(SourceStatus(name, false, "Conectando a $host:$port..."))
                    val connection = Socket()
                    connection.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                    connection.soTimeout = READ_TIMEOUT_MS
                    socket = connection
                    backoff = 1_000L

                    val output = connection.getOutputStream()
                    output.write("HELLO CarXust\n".toByteArray(Charsets.US_ASCII))
                    output.flush()

                    onStatus(SourceStatus(name, true, "Ligado • $host:$port"))
                    readFrames(connection, onPatch, onStatus)
                } catch (t: Throwable) {
                    if (isActive) {
                        onStatus(
                            SourceStatus(
                                name,
                                false,
                                t.message?.take(60) ?: "Sem conexão com o ESP32"
                            )
                        )
                    }
                } finally {
                    runCatching { socket?.close() }
                    socket = null
                }
                if (isActive) {
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF_MS)
                }
            }
        }
    }

    private suspend fun readFrames(
        connection: Socket,
        onPatch: (VehiclePatch) -> Unit,
        onStatus: (SourceStatus) -> Unit
    ) {
        val reader = BufferedReader(InputStreamReader(connection.getInputStream(), Charsets.UTF_8))
        val fields = linkedMapOf<String, String>()
        var device = ""

        while (isActive) {
            val line = reader.readLine() ?: break
            when {
                Esp32Protocol.isHello(line) -> {
                    fields.clear()
                    device = ""
                }

                Esp32Protocol.isEnd(line) -> {
                    if (fields.isNotEmpty()) {
                        onPatch(Esp32Protocol.toPatch(fields))
                    }
                    fields.clear()
                }

                else -> {
                    val parsed = Esp32Protocol.parseLine(line) ?: continue
                    if (parsed.first == "CAR") device = parsed.second
                    fields[parsed.first] = parsed.second
                }
            }
        }

        onStatus(
            SourceStatus(
                name,
                false,
                if (device.isBlank()) "Conexão encerrada" else "Conexão encerrada • $device"
            )
        )
    }

    override fun stop() {
        job?.cancel()
        job = null
        runCatching { socket?.close() }
        socket = null
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 4_000
        const val READ_TIMEOUT_MS = 8_000
        const val MAX_BACKOFF_MS = 15_000L
    }
}
