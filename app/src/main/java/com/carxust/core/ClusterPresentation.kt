package com.carxust.core

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.carxust.media.MediaRepository
import com.carxust.ui.cluster.ClusterScreen
import com.carxust.ui.components.toColor
import com.carxust.ui.theme.CarXustTheme
import com.carxust.vehicle.VehicleRepository

/**
 * Cluster renderizado em uma segunda tela via Presentation.
 * Não exige que a ROM permita lançar Activities em displays secundários.
 */
class ClusterPresentation(
    outerContext: Context,
    display: Display,
    private val vehicleRepository: VehicleRepository,
    private val prefs: CarXustPrefs,
    private val mediaRepository: MediaRepository
) : android.app.Presentation(outerContext, display) {

    private val owner = PresentationComposeOwner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        owner.create()

        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
        }
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val vehicle by vehicleRepository.state.collectAsStateWithLifecycle()
                val media by mediaRepository.state.collectAsStateWithLifecycle()
                val config by prefs.config.collectAsStateWithLifecycle()
                CarXustTheme(accent = config.accent.toColor()) {
                    ClusterScreen(
                        vehicle = vehicle,
                        media = media,
                        config = config
                    )
                }
            }
        }
        ViewTreeLifecycleOwner.set(composeView, owner)
        ViewTreeSavedStateRegistryOwner.set(composeView, owner)
        ViewTreeViewModelStoreOwner.set(composeView, owner)

        root.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            addFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    override fun onStart() {
        super.onStart()
        owner.start()
    }

    override fun onStop() {
        owner.stop()
        super.onStop()
    }

    override fun onDestroy() {
        owner.destroy()
        super.onDestroy()
    }

    private class PresentationComposeOwner : LifecycleOwner, ViewModelStoreOwner,
        SavedStateRegistryOwner {

        private val registry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle
            get() = registry
        override val viewModelStore: ViewModelStore
            get() = store
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        fun create() {
            savedStateController.performAttach()
            savedStateController.performRestore(null)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        fun start() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun stop() {
            if (registry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
        }

        fun destroy() {
            if (registry.currentState != Lifecycle.State.DESTROYED) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
            store.clear()
        }
    }
}
