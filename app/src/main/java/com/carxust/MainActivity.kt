package com.carxust

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.carxust.core.DisplayRouter
import com.carxust.media.MediaTrack
import com.carxust.media.MediaUiState
import com.carxust.service.VehicleService
import com.carxust.ui.CarScreen
import com.carxust.ui.components.NavRail
import com.carxust.ui.components.StatusTopBar
import com.carxust.ui.components.toColor
import com.carxust.ui.screens.AppsScreen
import com.carxust.ui.screens.ClimateScreen
import com.carxust.ui.screens.DashboardScreen
import com.carxust.ui.screens.DiagnosticsScreen
import com.carxust.ui.screens.MediaScreen
import com.carxust.ui.screens.NavigationScreen
import com.carxust.ui.screens.PhoneScreen
import com.carxust.ui.screens.SettingsScreen
import com.carxust.ui.theme.CarBackground
import com.carxust.ui.theme.CarXustTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var app: CarXustApp
    private lateinit var router: DisplayRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as CarXustApp
        router = DisplayRouter(this, app.prefs, app.vehicleRepository, app.mediaRepository)
        VehicleService.start(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.prefs.config.collectLatest {
                    router.refresh()
                }
            }
        }

        setContent {
            val vehicle by app.vehicleRepository.state.collectAsStateWithLifecycle()
            val media by app.mediaRepository.state.collectAsStateWithLifecycle()
            val config by app.prefs.config.collectAsStateWithLifecycle()
            val clusterStatus by router.status.collectAsStateWithLifecycle()

            CarXustTheme(accent = config.accent.toColor()) {
                Surface(color = CarBackground, modifier = Modifier.fillMaxSize()) {
                    CarXustRoot(
                        vehicleState = vehicle,
                        mediaState = media,
                        config = config,
                        clusterStatus = clusterStatus,
                        onPreviewCluster = {
                            startActivity(
                                Intent(this@MainActivity, ClusterPreviewActivity::class.java)
                            )
                        },
                        onConfigChange = { transform ->
                            app.prefs.update(transform)
                        },
                        onRestartTelemetry = { app.vehicleRepository.restart() },
                        onResetTrip = { app.vehicleRepository.resetTrip() }
                    )
                }
            }
        }

        ensurePermissions()
    }

    override fun onStart() {
        super.onStart()
        router.start()
        app.vehicleRepository.startGps()
    }

    override fun onResume() {
        super.onResume()
        router.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        router.refresh()
    }

    override fun onDestroy() {
        router.stop()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            app.vehicleRepository.startGps()
            router.refresh()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun ensurePermissions() {
        val wanted = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.CALL_PHONE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        val missing = wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST)
        }
    }

    private companion object {
        const val PERMISSION_REQUEST = 4201
    }
}

@Composable
private fun CarXustRoot(
    vehicleState: com.carxust.vehicle.VehicleState,
    mediaState: MediaUiState,
    config: com.carxust.core.UserConfig,
    clusterStatus: String,
    onPreviewCluster: () -> Unit,
    onConfigChange: ((com.carxust.core.UserConfig) -> com.carxust.core.UserConfig) -> Unit,
    onRestartTelemetry: () -> Unit,
    onResetTrip: () -> Unit
) {
    val context = LocalContext.current
    val app = remember(context) { CarXustApp.from(context) }
    var screen by rememberSaveable { mutableStateOf(CarScreen.HOME) }
    var localTracks by remember { mutableStateOf<List<MediaTrack>>(emptyList()) }
    var loadingLocal by remember { mutableStateOf(false) }
    var scanToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(screen, scanToken) {
        if (screen == CarScreen.MEDIA) {
            loadingLocal = true
            localTracks = app.mediaRepository.loadLocalMusic()
            loadingLocal = false
        }
    }

    BackHandler(enabled = screen != CarScreen.HOME) {
        screen = CarScreen.HOME
    }

    Row(Modifier.fillMaxSize().background(CarBackground)) {
        NavRail(
            current = screen,
            onSelect = { screen = it },
            accent = config.accent.toColor()
        )
        Column(Modifier.fillMaxSize()) {
            StatusTopBar(
                vehicle = vehicleState,
                useMiles = config.useMiles,
                driverName = config.driverName,
                clusterStatus = clusterStatus,
                onSettings = { screen = CarScreen.SETTINGS },
                onDiagnostics = { screen = CarScreen.DIAGNOSTICS },
                modifier = Modifier.fillMaxWidth()
            )
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(top = 2.dp)

            when (screen) {
                CarScreen.HOME -> DashboardScreen(
                    vehicle = vehicleState,
                    media = mediaState,
                    config = config,
                    onScreen = { screen = it },
                    onToggleMedia = app.mediaRepository::toggle,
                    onNextMedia = app.mediaRepository::next,
                    onPreviousMedia = app.mediaRepository::previous,
                    onClimateToggle = {
                        app.vehicleRepository.setClimate(!vehicleState.climateOn)
                    },
                    onFanChange = app.vehicleRepository::setFan,
                    onTempUp = {
                        app.vehicleRepository.setDriverTemp(vehicleState.driverTempC + 0.5f)
                    },
                    onTempDown = {
                        app.vehicleRepository.setDriverTemp(vehicleState.driverTempC - 0.5f)
                    },
                    onClusterPreview = onPreviewCluster,
                    onVolumeChange = app.mediaRepository::setVolume,
                    modifier = contentModifier
                )

                CarScreen.NAVIGATION -> NavigationScreen(
                    vehicle = vehicleState,
                    config = config,
                    modifier = contentModifier
                )

                CarScreen.MEDIA -> MediaScreen(
                    media = mediaState,
                    config = config,
                    localTracks = localTracks,
                    loadingLocal = loadingLocal,
                    onRefreshLocal = { scanToken++ },
                    onPlayTrack = { tracks, index ->
                        app.mediaRepository.playQueue(tracks, index)
                    },
                    onPlayRadio = app.mediaRepository::playRadio,
                    onToggle = app.mediaRepository::toggle,
                    onNext = app.mediaRepository::next,
                    onPrevious = app.mediaRepository::previous,
                    onSeek = app.mediaRepository::seekTo,
                    onVolume = app.mediaRepository::setVolume,
                    onShuffle = app.mediaRepository::toggleShuffle,
                    onRepeat = app.mediaRepository::toggleRepeat,
                    modifier = contentModifier
                )

                CarScreen.CLIMATE -> ClimateScreen(
                    vehicle = vehicleState,
                    config = config,
                    onClimateToggle = {
                        app.vehicleRepository.setClimate(!vehicleState.climateOn)
                    },
                    onFanChange = app.vehicleRepository::setFan,
                    onDriverTemp = app.vehicleRepository::setDriverTemp,
                    onPassengerTemp = app.vehicleRepository::setPassengerTemp,
                    onSeatHeatDriver = {
                        app.vehicleRepository.setSeatHeat(
                            driver = (vehicleState.seatHeatDriver + 1) % 4
                        )
                    },
                    onSeatHeatPassenger = {
                        app.vehicleRepository.setSeatHeat(
                            passenger = (vehicleState.seatHeatPassenger + 1) % 4
                        )
                    },
                    modifier = contentModifier
                )

                CarScreen.PHONE -> PhoneScreen(
                    config = config,
                    modifier = contentModifier
                )

                CarScreen.APPS -> AppsScreen(
                    config = config,
                    onScreen = { screen = it },
                    modifier = contentModifier
                )

                CarScreen.DIAGNOSTICS -> DiagnosticsScreen(
                    vehicle = vehicleState,
                    config = config,
                    onRestart = onRestartTelemetry,
                    onResetTrip = onResetTrip,
                    modifier = contentModifier
                )

                CarScreen.SETTINGS -> SettingsScreen(
                    config = config,
                    onConfig = onConfigChange,
                    onRestartTelemetry = onRestartTelemetry,
                    onResetTrip = onResetTrip,
                    onPreviewCluster = onPreviewCluster,
                    onScreen = { screen = it },
                    modifier = contentModifier
                )
            }
        }
    }
}
