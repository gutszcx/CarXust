package com.carxust.core

import android.app.Activity
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import com.carxust.media.MediaRepository
import com.carxust.vehicle.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DisplayRouter(
    private val activity: Activity,
    private val prefs: CarXustPrefs,
    private val vehicleRepository: VehicleRepository,
    private val mediaRepository: MediaRepository
) {
    private val displayManager =
        activity.getSystemService(Activity.DISPLAY_SERVICE) as DisplayManager
    private val handler = Handler(Looper.getMainLooper())

    private val _status = MutableStateFlow("Cluster: procurando segunda tela")
    val status: StateFlow<String> = _status.asStateFlow()

    private var presentation: ClusterPresentation? = null
    private var currentDisplayId: Int = -1
    private var started = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = refresh()
        override fun onDisplayRemoved(displayId: Int) = refresh()
        override fun onDisplayChanged(displayId: Int) = refresh()
    }

    fun start() {
        if (started) {
            refresh()
            return
        }
        started = true
        displayManager.registerDisplayListener(displayListener, handler)
        refresh()
    }

    fun stop() {
        started = false
        runCatching { displayManager.unregisterDisplayListener(displayListener) }
        dismiss()
        _status.value = "Cluster: parado"
    }

    fun refresh() {
        if (!started) return
        val config = prefs.config.value
        if (!config.clusterEnabled) {
            dismiss()
            _status.value = "Cluster desativado"
            return
        }

        val candidates = secondaryDisplays()
        val target = candidates.firstOrNull { it.displayId == config.clusterDisplayId }
            ?: candidates.firstOrNull()

        if (target == null) {
            dismiss()
            _status.value = "Cluster: aguardando segunda tela"
            return
        }

        if (presentation?.isShowing == true && currentDisplayId == target.displayId) {
            _status.value = "Cluster ativo • ${target.name}"
            return
        }

        dismiss()
        val next = ClusterPresentation(
            activity,
            target,
            vehicleRepository,
            prefs,
            mediaRepository
        )
        runCatching {
            next.show()
        }.onSuccess {
            presentation = next
            currentDisplayId = target.displayId
            _status.value = "Cluster ativo • ${target.name}"
        }.onFailure { error ->
            runCatching { next.dismiss() }
            presentation = null
            currentDisplayId = -1
            _status.value = "Cluster indisponível: ${error.message?.take(32) ?: "erro"}"
        }
    }

    private fun secondaryDisplays(): List<Display> {
        return displayManager.displays
            .filter { display ->
                display.displayId != Display.DEFAULT_DISPLAY &&
                    display.state != Display.STATE_OFF
            }
            .sortedBy { it.displayId }
    }

    private fun dismiss() {
        runCatching { presentation?.dismiss() }
        presentation = null
        currentDisplayId = -1
    }
}
