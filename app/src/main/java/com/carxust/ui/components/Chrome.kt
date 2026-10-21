package com.carxust.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carxust.core.Formatters
import com.carxust.ui.CarScreen
import com.carxust.ui.theme.CarBorder
import com.carxust.ui.theme.CarBorderSoft
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarGreen
import com.carxust.ui.theme.CarRed
import com.carxust.ui.theme.CarSurface
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextDim
import com.carxust.ui.theme.CarTextMuted
import com.carxust.vehicle.VehicleState

@Composable
fun StatusTopBar(
    vehicle: VehicleState,
    useMiles: Boolean,
    driverName: String,
    clusterStatus: String,
    onSettings: () -> Unit,
    onDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clock = rememberClock()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = clock,
                style = MaterialTheme.typography.headlineMedium,
                color = CarText
            )
            Text(
                text = Formatters.shortDate(),
                style = MaterialTheme.typography.labelSmall,
                color = CarTextMuted
            )
        }
        Spacer(Modifier.width(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarChip(
                text = "${Formatters.temperature(vehicle.outsideTempC, useMiles)} externo",
                icon = Icons.Rounded.Cloud,
                color = CarTextMuted
            )
            CarChip(
                text = vehicle.sourceLabel,
                icon = Icons.Rounded.Speed,
                color = if (vehicle.sourceConnected) CarGreen else CarRed
            )
        }

        Spacer(Modifier.weight(1f))

        CarChip(
            text = clusterStatus,
            icon = Icons.Rounded.Bluetooth,
            color = if (clusterStatus.startsWith("Cluster ativo")) CarCyan else CarTextMuted
        )
        Spacer(Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = null,
                tint = CarTextMuted,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.Bluetooth,
                contentDescription = null,
                tint = CarCyan,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CarRed.copy(alpha = 0.15f))
                    .border(1.dp, CarRed.copy(alpha = 0.4f), CircleShape)
                    .clickable(onClick = onDiagnostics),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Diagnóstico",
                    tint = CarRed,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CarSurface)
                    .border(1.dp, CarBorder, RoundedCornerShape(20.dp))
                    .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CarRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = driverName.take(1).uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarRed
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = driverName,
                        style = MaterialTheme.typography.labelLarge,
                        color = CarText
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButtonRound(
                icon = Icons.Rounded.Settings,
                contentDescription = "Ajustes",
                onClick = onSettings,
                size = 40.dp
            )
        }
    }
}

private data class NavItem(
    val screen: CarScreen,
    val icon: ImageVector,
    val label: String
)

private val navItems = listOf(
    NavItem(CarScreen.HOME, Icons.Rounded.Home, "Início"),
    NavItem(CarScreen.NAVIGATION, Icons.Rounded.Navigation, "Mapa"),
    NavItem(CarScreen.MEDIA, Icons.Rounded.MusicNote, "Mídia"),
    NavItem(CarScreen.CLIMATE, Icons.Rounded.AcUnit, "Clima"),
    NavItem(CarScreen.PHONE, Icons.Rounded.Phone, "Telefone"),
    NavItem(CarScreen.APPS, Icons.Rounded.Apps, "Apps"),
    NavItem(CarScreen.SENSORS, Icons.Rounded.Sensors, "Sensores"),
    NavItem(CarScreen.SETTINGS, Icons.Rounded.Settings, "Ajustes")
)

@Composable
fun NavRail(
    current: CarScreen,
    onSelect: (CarScreen) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(88.dp)
            .fillMaxHeight()
            .background(Color(0xFF070A0E))
            .border(width = 1.dp, color = CarBorderSoft)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.15f))
                .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "CX",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
        Spacer(Modifier.height(26.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            navItems.forEach { item ->
                val selected = item.screen == current
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) accent.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onSelect(item.screen) }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) accent else CarTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        fontSize = 9.sp,
                        color = if (selected) CarText else CarTextDim
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CarSurface)
                .border(1.dp, CarBorder, CircleShape)
                .clickable { onSelect(CarScreen.DIAGNOSTICS) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = "Diagnóstico",
                tint = if (current == CarScreen.DIAGNOSTICS) accent else CarTextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
