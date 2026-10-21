package com.carxust.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.carxust.BuildConfig
import com.carxust.core.AccentChoice
import com.carxust.core.UserConfig
import com.carxust.core.VehicleSourceType
import com.carxust.ui.CarScreen
import com.carxust.ui.components.CarCard
import com.carxust.ui.components.CarChip
import com.carxust.ui.components.IconTextButton
import com.carxust.ui.components.SectionLabel
import com.carxust.ui.components.toColor
import com.carxust.ui.theme.CarBackground
import com.carxust.ui.theme.CarBlue
import com.carxust.ui.theme.CarBorder
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarGreen
import com.carxust.ui.theme.CarSurface
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextMuted

@Composable
fun SettingsScreen(
    config: UserConfig,
    onConfig: ((UserConfig) -> UserConfig) -> Unit,
    onRestartTelemetry: () -> Unit,
    onResetTrip: () -> Unit,
    onPreviewCluster: () -> Unit,
    onScreen: (CarScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = config.accent.toColor()
    val context = LocalContext.current
    var displays by remember { mutableStateOf(listOf<DisplayInfo>()) }
    var devices by remember { mutableStateOf(listOf<BluetoothDevice>()) }

    LaunchedEffect(Unit) {
        displays = readDisplays(context)
        devices = readBondedDevices()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CarBackground)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.headlineMedium,
                        color = CarText
                    )
                    Text(
                        text = "Sistema CarXust ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CarTextMuted
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconTextButton(
                        icon = Icons.Rounded.Warning,
                        label = "Diagnóstico",
                        onClick = { onScreen(CarScreen.DIAGNOSTICS) },
                        accent = accent
                    )
                }
            }
        }

        item {
            SettingsCard(
                icon = Icons.Rounded.Monitor,
                title = "Telas e cluster",
                subtitle = "Painel do motorista em uma segunda saída de vídeo"
            ) {
                SettingSwitch(
                    title = "Cluster secundário",
                    subtitle = "Abre o painel em uma segunda tela (HDMI/DSI)",
                    checked = config.clusterEnabled,
                    accent = accent
                ) { checked -> onConfig { it.copy(clusterEnabled = checked) } }
                Spacer(Modifier.height(10.dp))
                SectionLabel("Tela do cluster")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip(
                        text = "Automática",
                        selected = config.clusterDisplayId == -1,
                        accent = accent
                    ) {
                        onConfig { it.copy(clusterDisplayId = -1) }
                    }
                    displays.forEach { display ->
                        SelectChip(
                            text = "${display.name} • ${display.id}",
                            selected = config.clusterDisplayId == display.id,
                            accent = accent
                        ) {
                            onConfig { it.copy(clusterDisplayId = display.id) }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (displays.isEmpty()) {
                        "Nenhuma segunda tela detectada. Conecte o monitor do painel e reconecte."
                    } else {
                        "${displays.size} tela(s) secundária(s): " +
                            displays.joinToString { "${it.name} (${it.width}x${it.height})" }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = CarTextMuted
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconTextButton(
                        icon = Icons.Rounded.Speed,
                        label = "Testar painel",
                        onClick = onPreviewCluster,
                        accent = accent
                    )
                    IconTextButton(
                        icon = Icons.Rounded.Refresh,
                        label = "Atualizar telas",
                        onClick = {
                            displays = readDisplays(context)
                        },
                        accent = accent
                    )
                }
            }
        }

        item {
            SettingsCard(
                icon = Icons.Rounded.DeveloperBoard,
                title = "Fonte de telemetria",
                subtitle = "Escolha como o painel lê os dados do veículo"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceChip("Simulador", VehicleSourceType.MOCK, config, accent, onConfig)
                    SourceChip("OBD-II", VehicleSourceType.OBD, config, accent, onConfig)
                    SourceChip("CAN", VehicleSourceType.CAN, config, accent, onConfig)
                    SourceChip("AAOS", VehicleSourceType.CAR_PROPERTY, config, accent, onConfig)
                }
                Spacer(Modifier.height(12.dp))
                when (config.vehicleSource) {
                    VehicleSourceType.MOCK -> Text(
                        text = "Simulação completa para bancada: velocidade, bateria, ADAS e mídia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CarTextMuted
                    )

                    VehicleSourceType.OBD -> {
                        SectionLabel("Adaptador Bluetooth pareado")
                        Spacer(Modifier.height(6.dp))
                        if (devices.isEmpty()) {
                            Text(
                                text = "Nenhum dispositivo pareado. Pareie o ELM327 nas configurações de Bluetooth do Android.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CarTextMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                devices.forEach { device ->
                                    val label = runCatching {
                                        "${device.name ?: "Sem nome"} • ${device.address}"
                                    }.getOrDefault(device.address)
                                    SelectChip(
                                        text = label,
                                        selected = config.obdMac == device.address,
                                        accent = accent
                                    ) {
                                        onConfig { it.copy(obdMac = device.address) }
                                    }
                                }
                            }
                        }
                    }

                    VehicleSourceType.CAN -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = config.canInterface,
                                onValueChange = { value ->
                                    onConfig { it.copy(canInterface = value.take(16)) }
                                },
                                label = { Text("Interface (can0)") },
                                singleLine = true,
                                modifier = Modifier.width(200.dp),
                                colors = carTextFieldColors(accent)
                            )
                            Column {
                                SectionLabel("Bitrate")
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(250_000, 500_000, 1_000_000).forEach { rate ->
                                        SelectChip(
                                            text = "${rate / 1000}k",
                                            selected = config.canBitrate == rate,
                                            accent = accent
                                        ) {
                                            onConfig { it.copy(canBitrate = rate) }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        SettingSwitch(
                            title = "Mostrar frames brutos",
                            subtitle = "Exibe o último frame CAN no diagnóstico",
                            checked = config.showRawCan,
                            accent = accent
                        ) { checked -> onConfig { it.copy(showRawCan = checked) } }
                    }

                    VehicleSourceType.CAR_PROPERTY -> Text(
                        text = "Use em builds Android Automotive (Raspberry Pi 4B alvo car). " +
                            "O app lê o CarPropertyManager via VHAL.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CarTextMuted
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconTextButton(
                        icon = Icons.Rounded.RestartAlt,
                        label = "Reiniciar telemetria",
                        onClick = onRestartTelemetry,
                        accent = accent
                    )
                    IconTextButton(
                        icon = Icons.Rounded.Refresh,
                        label = "Zerar viagem",
                        onClick = onResetTrip,
                        accent = accent
                    )
                }
            }
        }

        item {
            SettingsCard(
                icon = Icons.Rounded.Tune,
                title = "Unidades e preferências",
                subtitle = "Ajustes do painel e da experiência de condução"
            ) {
                SettingSwitch(
                    title = "Milhas (mph / mi)",
                    subtitle = "Desligado usa km/h, km, °C e bar",
                    checked = config.useMiles,
                    accent = accent
                ) { checked -> onConfig { it.copy(useMiles = checked) } }
                Spacer(Modifier.height(10.dp))
                SettingSwitch(
                    title = "Iniciar no boot",
                    subtitle = "Inicia a telemetria no boot, sem abrir a interface",
                    checked = config.autoStart,
                    accent = accent
                ) { checked -> onConfig { it.copy(autoStart = checked) } }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Limite de velocidade",
                        style = MaterialTheme.typography.titleMedium,
                        color = CarText,
                        modifier = Modifier.width(190.dp)
                    )
                    Slider(
                        value = config.speedLimitKph.toFloat(),
                        onValueChange = { value ->
                            onConfig { it.copy(speedLimitKph = value.toInt()) }
                        },
                        valueRange = 40f..160f,
                        steps = 23,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = CarBorder
                        )
                    )
                    Text(
                        text = "${config.speedLimitKph} km/h",
                        style = MaterialTheme.typography.labelMedium,
                        color = CarTextMuted,
                        modifier = Modifier.width(70.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = config.driverName,
                    onValueChange = { value -> onConfig { it.copy(driverName = value.take(24)) } },
                    label = { Text("Nome do motorista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = carTextFieldColors(accent)
                )
            }
        }

        item {
            SettingsCard(
                icon = Icons.Rounded.Palette,
                title = "Aparência",
                subtitle = "Cor de destaque do painel e da multimídia"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AccentChoice.entries.forEach { choice ->
                        val color = choice.toColor()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onConfig { it.copy(accent = choice) } }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (config.accent == choice) 3.dp else 1.dp,
                                        if (config.accent == choice) Color.White else CarBorder,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (config.accent == choice) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = choice.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (config.accent == choice) CarText else CarTextMuted
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                icon = Icons.Rounded.Info,
                title = "Sobre o CarXust",
                subtitle = "Sistema de painel e multimídia para ARM64"
            ) {
                InfoRow("Versão", BuildConfig.VERSION_NAME)
                InfoRow("Pacote", BuildConfig.APPLICATION_ID)
                InfoRow("Build", "${BuildConfig.BUILD_TYPE} • ${BuildConfig.VERSION_CODE}")
                InfoRow("Plataformas", "Orange Pi Zero 3 • Raspberry Pi 4B • ARM64")
                InfoRow("Min SDK / Target", "29 / 35")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CarChip("Android Automotive", icon = Icons.Rounded.Speed, color = CarCyan)
                    CarChip("Dual display", icon = Icons.Rounded.Monitor, color = CarGreen)
                    CarChip("OBD-II / CAN", icon = Icons.Rounded.Bluetooth, color = CarBlue)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    CarCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(CarSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CarTextMuted,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = CarText
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CarTextMuted
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = CarText
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = CarTextMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = CarTextMuted,
                uncheckedTrackColor = CarSurface
            )
        )
    }
}

@Composable
private fun SelectChip(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.16f) else CarSurface)
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.55f) else CarBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) CarText else CarTextMuted
        )
    }
}

@Composable
private fun SourceChip(
    label: String,
    source: VehicleSourceType,
    config: UserConfig,
    accent: Color,
    onConfig: ((UserConfig) -> UserConfig) -> Unit
) {
    SelectChip(
        text = label,
        selected = config.vehicleSource == source,
        accent = accent
    ) { onConfig { it.copy(vehicleSource = source) } }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = CarTextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = CarText
        )
    }
}

@Composable
private fun carTextFieldColors(accent: Color) = TextFieldDefaults.colors(
    focusedContainerColor = CarSurface,
    unfocusedContainerColor = CarSurface,
    focusedTextColor = CarText,
    unfocusedTextColor = CarText,
    cursorColor = accent,
    focusedIndicatorColor = accent,
    unfocusedIndicatorColor = CarBorder,
    focusedLabelColor = CarTextMuted,
    unfocusedLabelColor = CarTextMuted
)

private data class DisplayInfo(
    val id: Int,
    val name: String,
    val width: Int,
    val height: Int
)

@SuppressLint("MissingPermission")
private fun readDisplays(context: Context): List<DisplayInfo> {
    val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    return manager.displays
        .filter { it.displayId != Display.DEFAULT_DISPLAY && it.state != Display.STATE_OFF }
        .map { display ->
            val size = android.graphics.Point()
            @Suppress("DEPRECATION")
            display.getRealSize(size)
            DisplayInfo(
                id = display.displayId,
                name = display.name ?: "Display ${display.displayId}",
                width = size.x,
                height = size.y
            )
        }
}

@SuppressLint("MissingPermission")
private fun readBondedDevices(): List<BluetoothDevice> {
    return runCatching {
        BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.toList().orEmpty()
    }.getOrDefault(emptyList())
}
