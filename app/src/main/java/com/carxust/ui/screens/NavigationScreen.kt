package com.carxust.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carxust.core.UserConfig
import com.carxust.ui.components.CarCard
import com.carxust.ui.components.MapCanvas
import com.carxust.ui.components.SectionLabel
import com.carxust.ui.components.toColor
import com.carxust.ui.theme.CarBackground
import com.carxust.ui.theme.CarBorder
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarGreen
import com.carxust.ui.theme.CarSurface
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextMuted
import com.carxust.vehicle.VehicleState

@Composable
fun NavigationScreen(
    vehicle: VehicleState,
    config: UserConfig,
    modifier: Modifier = Modifier
) {
    val accent = config.accent.toColor()
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(CarBackground)
            .padding(22.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(0.68f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CarSurface)
                    .border(1.dp, CarBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = CarTextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Para onde vamos?",
                    style = MaterialTheme.typography.titleMedium,
                    color = CarTextMuted
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "VOZ",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
            MapCanvas(
                vehicle = vehicle,
                accent = accent,
                showRoute = true,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }

        Column(
            modifier = Modifier.weight(0.32f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CarCard(modifier = Modifier.fillMaxWidth(), accent = accent) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Vire à direita",
                                style = MaterialTheme.typography.headlineSmall,
                                color = CarText
                            )
                            Text(
                                text = "em 350 m • Av. Paulista",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CarTextMuted
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            SectionLabel("Distância")
                            Text(
                                text = "8,4 km",
                                fontSize = 24.sp,
                                color = CarText
                            )
                        }
                        Column {
                            SectionLabel("Chegada")
                            Text(
                                text = "14:32",
                                fontSize = 24.sp,
                                color = CarText
                            )
                        }
                        Column {
                            SectionLabel("Trânsito")
                            Text(
                                text = "Leve",
                                fontSize = 24.sp,
                                color = CarGreen
                            )
                        }
                    }
                }
            }

            CarCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column {
                    SectionLabel("Rota")
                    Spacer(Modifier.height(10.dp))
                    RouteStep("1", "Siga na Av. Paulista", "2,1 km", accent)
                    RouteStep("2", "Vire à direita na R. Augusta", "3,8 km", accent)
                    RouteStep("3", "Continue na R. da Consolação", "1,9 km", accent)
                    RouteStep("4", "Destino à direita", "350 m", accent)
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CarBackground)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Rodovia ponderada",
                            style = MaterialTheme.typography.labelMedium,
                            color = CarTextMuted
                        )
                        Text(
                            text = "EV • 22% chegada",
                            style = MaterialTheme.typography.labelMedium,
                            color = CarCyan
                        )
                    }
                }
            }

            CarCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SectionLabel("Favoritos")
                    Spacer(Modifier.height(10.dp))
                    FavoriteRow(Icons.Rounded.Home, "Casa", "4,2 km", accent)
                    Spacer(Modifier.height(8.dp))
                    FavoriteRow(Icons.Rounded.Work, "Trabalho", "8,4 km", accent)
                    Spacer(Modifier.height(8.dp))
                    FavoriteRow(Icons.Rounded.Star, "Carregador rápido", "1,8 km", accent)
                }
            }
        }
    }
}

@Composable
private fun RouteStep(number: String, title: String, distance: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = CarText,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = distance,
            style = MaterialTheme.typography.labelSmall,
            color = CarTextMuted
        )
    }
}

@Composable
private fun FavoriteRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    distance: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CarBackground)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = CarText,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = distance,
            style = MaterialTheme.typography.labelSmall,
            color = CarTextMuted
        )
    }
}
