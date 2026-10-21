package com.carxust.vehicle

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat

class GpsSource(private val context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var listener: LocationListener? = null
    private var onPatch: ((VehiclePatch) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun start(onPatch: (VehiclePatch) -> Unit) {
        stop()
        this.onPatch = onPatch
        if (!hasPermission()) return

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                this@GpsSource.onPatch?.invoke(
                    VehiclePatch(
                        gps = GpsState(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitudeMeters = location.altitude,
                            heading = location.bearing.takeIf { it >= 0f } ?: 0f,
                            accuracyMeters = location.accuracy,
                            hasFix = location.hasAccuracy()
                        )
                    )
                )
            }

            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        this.listener = listener

        runCatching {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { locationManager.isProviderEnabled(it) }
            providers.forEach { provider ->
                locationManager.requestLocationUpdates(
                    provider,
                    1000L,
                    1f,
                    listener,
                    Looper.getMainLooper()
                )
            }
        }
    }

    fun stop() {
        listener?.let { runCatching { locationManager.removeUpdates(it) } }
        listener = null
        onPatch = null
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
