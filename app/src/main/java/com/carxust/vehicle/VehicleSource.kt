package com.carxust.vehicle

interface VehicleSource {
    val name: String
    fun start(onPatch: (VehiclePatch) -> Unit, onStatus: (SourceStatus) -> Unit)
    fun stop()

    /** Zera a viagem no estado interno da fonte, quando ela controla esse valor. */
    fun resetTrip() = Unit
}
