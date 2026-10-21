package com.carxust.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carxust.core.Formatters
import com.carxust.core.UserConfig
import com.carxust.media.MediaTrack
import com.carxust.media.MediaUiState
import com.carxust.media.RadioCatalog
import com.carxust.ui.components.AlbumArt
import com.carxust.ui.components.CarCard
import com.carxust.ui.components.IconButtonRound
import com.carxust.ui.components.MediaControl
import com.carxust.ui.components.ProgressBar
import com.carxust.ui.components.SectionLabel
import com.carxust.ui.components.VolumeControl
import com.carxust.ui.components.toColor
import com.carxust.ui.theme.CarBackground
import com.carxust.ui.theme.CarBorder
import com.carxust.ui.theme.CarCyan
import com.carxust.ui.theme.CarRed
import com.carxust.ui.theme.CarSurface
import com.carxust.ui.theme.CarText
import com.carxust.ui.theme.CarTextMuted
import java.util.Locale

private enum class MediaTab { RADIO, LOCAL }

@Composable
fun MediaScreen(
    media: MediaUiState,
    config: UserConfig,
    localTracks: List<MediaTrack>,
    loadingLocal: Boolean,
    onRefreshLocal: () -> Unit,
    onPlayTrack: (List<MediaTrack>, Int) -> Unit,
    onPlayRadio: (MediaTrack) -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = config.accent.toColor()
    var tab by remember { mutableStateOf(MediaTab.RADIO) }
    val queue = media.queue

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(CarBackground)
            .padding(22.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(0.52f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CarCard(modifier = Modifier.fillMaxWidth().weight(1f), accent = accent) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            accent.copy(alpha = 0.55f),
                                            Color(0xFF12202E),
                                            Color(0xFF070B10)
                                        )
                                    )
                                )
                                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (media.isRadio) Icons.Rounded.Radio else Icons.Rounded.Album,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(84.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = CarText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = media.artist,
                        style = MaterialTheme.typography.titleLarge,
                        color = CarTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (media.album.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = media.album,
                            style = MaterialTheme.typography.labelMedium,
                            color = CarTextMuted
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    if (!media.isRadio) {
                        ProgressBar(
                            positionMs = media.positionMs,
                            durationMs = media.durationMs,
                            accent = accent,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                        Spacer(Modifier.height(10.dp))
                    } else {
                        Text(
                            text = "TRANSMISSÃO AO VIVO",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButtonRound(
                            icon = Icons.Rounded.Shuffle,
                            contentDescription = "Aleatório",
                            onClick = onShuffle,
                            tint = if (media.shuffle) accent else CarTextMuted,
                            size = 42.dp
                        )
                        MediaControl(
                            icon = Icons.Rounded.SkipPrevious,
                            description = "Anterior",
                            onClick = onPrevious,
                            size = 48.dp,
                            accent = CarSurface
                        )
                        MediaControl(
                            icon = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            description = "Reproduzir",
                            onClick = onToggle,
                            size = 68.dp,
                            accent = accent,
                            tint = Color.White
                        )
                        MediaControl(
                            icon = Icons.Rounded.SkipNext,
                            description = "Próxima",
                            onClick = onNext,
                            size = 48.dp,
                            accent = CarSurface
                        )
                        IconButtonRound(
                            icon = Icons.Rounded.Repeat,
                            contentDescription = "Repetir",
                            onClick = onRepeat,
                            tint = if (media.repeat) accent else CarTextMuted,
                            size = 42.dp
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            CarCard(modifier = Modifier.fillMaxWidth()) {
                VolumeControl(
                    volume = media.volume,
                    onVolumeChange = onVolume,
                    accent = accent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(0.48f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TabButton(
                    icon = Icons.Rounded.Radio,
                    label = "Rádio",
                    selected = tab == MediaTab.RADIO,
                    accent = accent
                ) { tab = MediaTab.RADIO }
                TabButton(
                    icon = Icons.Rounded.Usb,
                    label = "USB / Local",
                    selected = tab == MediaTab.LOCAL,
                    accent = accent
                ) { tab = MediaTab.LOCAL }
                Spacer(Modifier.weight(1f))
                IconButtonRound(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = "Atualizar",
                    onClick = onRefreshLocal,
                    size = 42.dp,
                    tint = if (loadingLocal) accent else CarTextMuted
                )
            }

            CarCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (tab == MediaTab.RADIO) {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionLabel("Estações de internet")
                            Text(
                                text = "${RadioCatalog.stations.size} canais",
                                style = MaterialTheme.typography.labelSmall,
                                color = CarTextMuted
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(RadioCatalog.stations) { station ->
                                TrackRow(
                                    track = station,
                                    selected = media.title == station.title,
                                    accent = accent,
                                    onClick = { onPlayRadio(station) }
                                )
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionLabel("Biblioteca local")
                            Text(
                                text = "${localTracks.size} faixas",
                                style = MaterialTheme.typography.labelSmall,
                                color = CarTextMuted
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        when {
                            loadingLocal -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Procurando músicas...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CarTextMuted
                                    )
                                }
                            }
                            localTracks.isEmpty() -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = CarTextMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Nenhuma música encontrada",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CarText
                                    )
                                    Text(
                                        text = "Conecte um pendrive com áudio ou copie para o dispositivo.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CarTextMuted
                                    )
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(localTracks) { track ->
                                        val index = localTracks.indexOf(track)
                                        TrackRow(
                                            track = track,
                                            selected = queue.getOrNull(media.index)?.id == track.id,
                                            accent = accent,
                                            onClick = { onPlayTrack(localTracks, index) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CarCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Fila de reprodução",
                                style = MaterialTheme.typography.titleMedium,
                                color = CarText
                            )
                            Text(
                                text = "${queue.size} itens • ${if (media.isPlaying) "reproduzindo" else "pausado"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CarTextMuted
                            )
                        }
                    }
                    Text(
                        text = String.format(Locale.US, "%02d/%02d", media.index + 1, queue.size.coerceAtLeast(1)),
                        style = MaterialTheme.typography.labelMedium,
                        color = CarTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.14f) else CarSurface)
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.5f) else CarBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent else CarTextMuted,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) CarText else CarTextMuted
        )
    }
}

@Composable
private fun TrackRow(
    track: MediaTrack,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.1f) else CarBackground)
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.35f) else CarBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(
            media = MediaUiState(
                title = track.title,
                artist = track.artist,
                album = track.album,
                isRadio = track.isRadio,
                isPlaying = selected
            ),
            size = 44.dp
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) accent else CarText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.labelMedium,
                color = CarTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Icon(
                imageVector = if (selected) Icons.Rounded.PlayArrow else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        } else if (!track.isRadio && track.durationMs > 0) {
            Text(
                text = Formatters.duration(track.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = CarTextMuted
            )
        }
    }
}
