package com.carxust.vehicle

import java.util.Locale

/**
 * Decodificação pura das respostas do ELM327/STN1110.
 *
 * Mantido sem dependências Android para poder ser testado na JVM.
 * Todas as funções aceitam a resposta bruta (com eco de comando, cabeçalhos
 * CAN e terminador '>') e devolvem null quando o PID não está presente.
 */
object ObdPids {

    fun payload(response: String, pid: String): IntArray? {
        val normalized = response.uppercase(Locale.US)
            .replace("SEARCHING...", "")
            .replace("NO DATA", "")
            .replace("UNABLE TO CONNECT", "")
            .replace("BUS INIT", "")
            .replace(Regex("[^0-9A-F]"), "")
        val marker = "41$pid"
        val index = normalized.indexOf(marker)
        if (index < 0) return null
        val payload = normalized.substring(index + marker.length)
        if (payload.length < 2) return null
        val usable = payload.take(payload.length - (payload.length % 2))
        return usable.chunked(2).mapNotNull { it.toIntOrNull(16) }.toIntArray()
    }

    fun rpm(response: String): Int? {
        val bytes = payload(response, "0C") ?: return null
        if (bytes.size < 2) return null
        return ((bytes[0] and 0xFF) * 256 + (bytes[1] and 0xFF)) / 4
    }

    fun speedKph(response: String): Float? =
        payload(response, "0D")?.getOrNull(0)?.toFloat()

    fun coolantCelsius(response: String): Float? =
        payload(response, "05")?.getOrNull(0)?.minus(40)?.toFloat()

    fun throttlePercent(response: String): Float? =
        payload(response, "11")?.getOrNull(0)?.let { it / 255f * 100f }

    fun moduleVoltage(response: String): Float? {
        val bytes = payload(response, "42") ?: return null
        if (bytes.size < 2) return null
        return ((bytes[0] and 0xFF) * 256 + (bytes[1] and 0xFF)) / 1000f
    }

    fun ambientCelsius(response: String): Float? =
        payload(response, "46")?.getOrNull(0)?.minus(40)?.toFloat()

    fun fuelPercent(response: String): Float? =
        payload(response, "2F")?.getOrNull(0)?.let { it / 255f * 100f }

    fun batteryLifePercent(response: String): Float? =
        payload(response, "5B")?.getOrNull(0)?.toFloat()?.coerceIn(0f, 100f)
}
