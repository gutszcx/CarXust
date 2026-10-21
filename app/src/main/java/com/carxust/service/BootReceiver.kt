package com.carxust.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.carxust.core.CarXustPrefs

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val prefs = CarXustPrefs(context)
        if (prefs.config.value.autoStart) {
            VehicleService.start(context)
        }
    }
}
