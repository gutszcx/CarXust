package com.carxust.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carxust.core.Formatters
import com.carxust.media.MediaUiState
import com.carxust.ui.theme.CarBorder
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarRed
import com.carxust.ui.theme.CarSurface
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextMuted
import com.carxust.ui.theme.CarTextDim

@Composable
fun AlbumArt(
    media: MediaUiState,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val colors = when {
        media.isRadio -> listOf(CarRed.copy(alpha = 0.85f), Color(0xFF26101A))
        media.isPlaying -> listOf(CarCyan.copy(alpha = 0.55f), Color(0xFF0B1B2A))
        else -> listOf(Color(0xFF213040), Color(0xFF0C141B))
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.2f))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        if (media.isRadio) {
            Icon(
                imageVector = Icons.Rounded.Radio,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(size * 0.42f)
            )
        } else {
            Text(
                text = media.title.take(1).uppercase(),
                fontSize = (size.value * 0.34f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun MediaMini(
    media: MediaUiState,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = CarRed
) {
    CarCard(modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AlbumArt(media = media, size = 58.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = CarText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = media.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CarTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (media.isBuffering) "Carregando..." else media.album,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (media.isBuffering) accent else CarTextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MediaControl(Icons.Rounded.SkipPrevious, "Anterior", onPrevious, 34.dp)
                    MediaControl(
                        if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (media.isPlaying) "Pausar" else "Tocar",
                        onToggle,
                        42.dp,
                        accent = accent
                    )
                    MediaControl(Icons.Rounded.SkipNext, "Próxima", onNext, 34.dp)
                }
                Text(
                    text = if (media.isRadio) "AO VIVO" else Formatters.duration(media.positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
        }
    }
}

@Composable
fun MediaControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    size: Dp = 42.dp,
    accent: Color = CarSurface,
    tint: Color = CarText
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(accent)
            .border(1.dp, CarBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
fun VolumeControl(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = CarRed
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (volume > 0f) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
            contentDescription = "Volume",
            tint = CarTextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = CarBorder
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${(volume * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = CarTextMuted,
            modifier = Modifier.width(38.dp)
        )
    }
}

@Composable
fun ProgressBar(
    positionMs: Long,
    durationMs: Long,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val ratio = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(CarBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
        Spacer(Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = Formatters.duration(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = CarTextMuted
            )
            Text(
                text = Formatters.duration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = CarTextMuted
            )
        }
    }
}
