package com.carxust

import android.app.Application
import com.carxust.core.CarXustPrefs
import com.carxust.media.MediaRepository
import com.carxust.vehicle.VehicleRepository

class CarXustApp : Application() {

    lateinit var prefs: CarXustPrefs
        private set
    lateinit var vehicleRepository: VehicleRepository
        private set
    lateinit var mediaRepository: MediaRepository
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = CarXustPrefs(this)
        vehicleRepository = VehicleRepository(this, prefs)
        mediaRepository = MediaRepository(this)
    }

    companion object {
        fun from(context: android.content.Context): CarXustApp =
            context.applicationContext as CarXustApp
    }
}
