package com.carxust.vehicle

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Integração com Android Automotive (AAOS) via CarPropertyManager.
 *
 * Usa reflexão para que o mesmo APK rode em tablets/telefones sem android.car.
 * Em builds AAOS (ex.: alvo car do Raspberry Pi 4) os dados reais do VHAL
 * são lidos quando as permissões android.car.permission.* estão concedidas.
 */
class CarPropertySource(private val context: Context) : VehicleSource {

    override val name: String = "Android Automotive"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun start(onPatch: (VehiclePatch) -> Unit, onStatus: (SourceStatus) -> Unit) {
        stop()
        job = scope.launch {
            var car: Any? = null
            try {
                val carClass = Class.forName("android.car.Car")
                val (carInstance, propertyManager) = connect(carClass)
                car = carInstance

                val vehicleProperty = Class.forName("android.car.VehicleProperty")
                val vehicleGear = runCatching { Class.forName("android.car.VehicleGear") }.getOrNull()
                val ids = listOf(
                    "PERF_VEHICLE_SPEED",
                    "ENGINE_RPM",
                    "GEAR",
                    "EV_BATTERY_LEVEL",
                    "RANGE_REMAINING",
                    "EV_BATTERY_INSTANTANEOUS_CHARGE_RATE",
                    "ENV_OUTSIDE_TEMPERATURE",
                    "PARKING_BRAKE_ON"
                ).associateWith { name ->
                    runCatching { vehicleProperty.getField(name).getInt(null) }.getOrNull()
                }

                onStatus(SourceStatus(name, true, "VHAL conectado"))
                while (isActive) {
                    val patch = readPatch(propertyManager, ids)
                    if (patch != null) onPatch(patch)
                    delay(200)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "CarProperty indisponível", t)
                onStatus(
                    SourceStatus(
                        name,
                        false,
                        when (t) {
                            is ClassNotFoundException -> "android.car não disponível (não é AAOS)"
                            is SecurityException -> "Permissões android.car não concedidas"
                            else -> t.message?.take(80) ?: "VHAL indisponível"
                        }
                    )
                )
            } finally {
                runCatching {
                    car?.let { instance ->
                        instance.javaClass.getMethod("disconnect").invoke(instance)
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun connect(carClass: Class<*>): Pair<Any, Any> {
        val contextClass = Context::class.java
        val car = runCatching {
            carClass.getMethod("createCar", contextClass).invoke(null, context)
        }.getOrElse {
            val method = carClass.methods.firstOrNull {
                it.name == "createCar" &&
                    it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] == contextClass
            } ?: throw IllegalStateException("createCar não encontrado")
            method.invoke(null, context, null)
        } ?: throw IllegalStateException("Car indisponível")

        carClass.getMethod("connect").invoke(car)
        val propertyManager = carClass.getMethod("getCarManager", String::class.java)
            .invoke(car, "property")
            ?: throw IllegalStateException("CarPropertyManager indisponível")
        return car to propertyManager
    }

    private fun readPatch(manager: Any, ids: Map<String, Int?>): VehiclePatch? {
        val speedValue = readValue(manager, ids["PERF_VEHICLE_SPEED"])
        val speed = (speedValue as? Float) ?: (speedValue as? Int)?.toFloat()
        val rpmValue = readValue(manager, ids["ENGINE_RPM"])
        val rpm = (rpmValue as? Float)?.toInt() ?: (rpmValue as? Int)
        val gearRaw = readValue(manager, ids["GEAR"])
        val gearValue = (gearRaw as? Int) ?: (gearRaw as? IntArray)?.firstOrNull()
        val batteryValue = readValue(manager, ids["EV_BATTERY_LEVEL"])
        val battery = (batteryValue as? Float) ?: (batteryValue as? Int)?.toFloat()
        val rangeValue = readValue(manager, ids["RANGE_REMAINING"])
        val rangeMeters = (rangeValue as? Float) ?: (rangeValue as? Int)?.toFloat()
        val chargeValue = readValue(manager, ids["EV_BATTERY_INSTANTANEOUS_CHARGE_RATE"])
        val chargeWatts = (chargeValue as? Float) ?: (chargeValue as? Int)?.toFloat()
        val outsideValue = readValue(manager, ids["ENV_OUTSIDE_TEMPERATURE"])
        val outside = (outsideValue as? Float) ?: (outsideValue as? Int)?.toFloat()
        val parkingBrake = readValue(manager, ids["PARKING_BRAKE_ON"]) as? Boolean

        if (speed == null && rpm == null && battery == null && rangeMeters == null &&
            gearValue == null && outside == null && parkingBrake == null
        ) {
            return null
        }

        val charging = chargeWatts != null && chargeWatts > 50f
        return VehiclePatch(
            speedKph = speed?.times(3.6f),
            rpm = rpm,
            gear = gearValue?.let { mapGear(it) },
            batteryPercent = battery,
            rangeKm = rangeMeters?.div(1000f),
            chargePowerKw = chargeWatts?.div(1000f),
            charging = charging,
            outsideTempC = outside,
            parkingBrake = parkingBrake,
            rawCanFrame = "VHAL speed=${speed ?: "-"} gear=${gearValue ?: "-"} soc=${battery ?: "-"}"
        )
    }

    private fun readValue(manager: Any, propertyId: Int?): Any? {
        if (propertyId == null) return null
        return runCatching {
            val property = manager.javaClass
                .getMethod("getProperty", Int::class.javaPrimitiveType!!)
                .invoke(manager, propertyId)
            if (property == null) return null
            val status = runCatching {
                property.javaClass.getMethod("getStatus").invoke(property) as? Int
            }.getOrNull()
            if (status != null && status != 0) return null
            property.javaClass.getMethod("getValue").invoke(property)
        }.getOrNull()
    }

    private fun mapGear(value: Int): Gear {
        val gearClass = runCatching { Class.forName("android.car.VehicleGear") }.getOrNull()
            ?: return Gear.DRIVE
        fun constant(name: String): Int? = runCatching {
            gearClass.getField(name).getInt(null)
        }.getOrNull()
        val park = constant("GEAR_PARK")
        val reverse = constant("GEAR_REVERSE")
        val neutral = constant("GEAR_NEUTRAL")
        val drive = constant("GEAR_DRIVE")
        return when (value) {
            0 -> Gear.NEUTRAL
            park -> Gear.PARK
            reverse -> Gear.REVERSE
            neutral -> Gear.NEUTRAL
            drive -> Gear.DRIVE
            else -> Gear.DRIVE
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val TAG = "CarXustVhal"
    }
}
