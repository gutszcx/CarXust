package com.carxust.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class VehicleSourceType {
    MOCK,
    OBD,
    CAN,
    CAR_PROPERTY
}

enum class AccentChoice {
    RED,
    BLUE,
    CYAN,
    GREEN
}

data class UserConfig(
    val useMiles: Boolean = false,
    val clusterEnabled: Boolean = true,
    val clusterDisplayId: Int = -1,
    val vehicleSource: VehicleSourceType = VehicleSourceType.MOCK,
    val obdMac: String = "",
    val canInterface: String = "can0",
    val canBitrate: Int = 500_000,
    val accent: AccentChoice = AccentChoice.RED,
    val driverName: String = "Motorista",
    val autoStart: Boolean = true,
    val showRawCan: Boolean = false,
    val speedLimitKph: Int = 120
)

class CarXustPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("carxust_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(load())
    val config: StateFlow<UserConfig> = _config.asStateFlow()

    private fun load(): UserConfig {
        val source = runCatching {
            VehicleSourceType.valueOf(
                prefs.getString(KEY_SOURCE, VehicleSourceType.MOCK.name) ?: VehicleSourceType.MOCK.name
            )
        }.getOrDefault(VehicleSourceType.MOCK)
        val accent = runCatching {
            AccentChoice.valueOf(
                prefs.getString(KEY_ACCENT, AccentChoice.RED.name) ?: AccentChoice.RED.name
            )
        }.getOrDefault(AccentChoice.RED)

        return UserConfig(
            useMiles = prefs.getBoolean(KEY_MILES, false),
            clusterEnabled = prefs.getBoolean(KEY_CLUSTER, true),
            clusterDisplayId = prefs.getInt(KEY_CLUSTER_DISPLAY, -1),
            vehicleSource = source,
            obdMac = prefs.getString(KEY_OBD_MAC, "") ?: "",
            canInterface = prefs.getString(KEY_CAN_IFACE, "can0") ?: "can0",
            canBitrate = prefs.getInt(KEY_CAN_BITRATE, 500_000),
            accent = accent,
            driverName = prefs.getString(KEY_DRIVER, "Motorista") ?: "Motorista",
            autoStart = prefs.getBoolean(KEY_AUTOSTART, true),
            showRawCan = prefs.getBoolean(KEY_RAW_CAN, false),
            speedLimitKph = prefs.getInt(KEY_SPEED_LIMIT, 120)
        )
    }

    fun update(transform: (UserConfig) -> UserConfig) {
        val next = transform(_config.value)
        val editor = prefs.edit()
            .putBoolean(KEY_MILES, next.useMiles)
            .putBoolean(KEY_CLUSTER, next.clusterEnabled)
            .putInt(KEY_CLUSTER_DISPLAY, next.clusterDisplayId)
            .putString(KEY_SOURCE, next.vehicleSource.name)
            .putString(KEY_OBD_MAC, next.obdMac)
            .putString(KEY_CAN_IFACE, next.canInterface)
            .putInt(KEY_CAN_BITRATE, next.canBitrate)
            .putString(KEY_ACCENT, next.accent.name)
            .putString(KEY_DRIVER, next.driverName)
            .putBoolean(KEY_AUTOSTART, next.autoStart)
            .putBoolean(KEY_RAW_CAN, next.showRawCan)
            .putInt(KEY_SPEED_LIMIT, next.speedLimitKph)
        editor.apply()
        _config.value = next
    }

    private companion object {
        const val KEY_MILES = "units_miles"
        const val KEY_CLUSTER = "cluster_enabled"
        const val KEY_CLUSTER_DISPLAY = "cluster_display_id"
        const val KEY_SOURCE = "vehicle_source"
        const val KEY_OBD_MAC = "obd_mac"
        const val KEY_CAN_IFACE = "can_iface"
        const val KEY_CAN_BITRATE = "can_bitrate"
        const val KEY_ACCENT = "accent"
        const val KEY_DRIVER = "driver_name"
        const val KEY_AUTOSTART = "auto_start"
        const val KEY_RAW_CAN = "raw_can"
        const val KEY_SPEED_LIMIT = "speed_limit"
    }
}
