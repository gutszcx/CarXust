package com.carxust.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val isRadio: Boolean = false,
    val durationMs: Long = 0L
)

data class MediaUiState(
    val queue: List<MediaTrack> = emptyList(),
    val index: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val title: String = "Nada reproduzindo",
    val artist: String = "Selecione uma fonte",
    val album: String = "",
    val isRadio: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 0.75f,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
    val error: String? = null
) {
    val hasNext: Boolean
        get() = index < queue.lastIndex

    val hasPrevious: Boolean
        get() = index > 0
}

object RadioCatalog {
    val stations = listOf(
        MediaTrack(
            id = "radio-groove",
            title = "Groove Salad",
            artist = "SomaFM • ambient",
            album = "Internet radio",
            uri = "https://ice1.somafm.com/groovesalad-128-mp3",
            isRadio = true
        ),
        MediaTrack(
            id = "radio-defcon",
            title = "DEF CON Radio",
            artist = "SomaFM • electronic",
            album = "Internet radio",
            uri = "https://ice1.somafm.com/defcon-128-mp3",
            isRadio = true
        ),
        MediaTrack(
            id = "radio-secretagent",
            title = "Secret Agent",
            artist = "SomaFM • lounge",
            album = "Internet radio",
            uri = "https://ice1.somafm.com/secretagent-128-mp3",
            isRadio = true
        ),
        MediaTrack(
            id = "radio-indiepop",
            title = "Indie Pop Rocks",
            artist = "SomaFM • indie",
            album = "Internet radio",
            uri = "https://ice1.somafm.com/indiepop-128-mp3",
            isRadio = true
        )
    )
}

class MediaRepository(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _state = MutableStateFlow(MediaUiState())
    val state: StateFlow<MediaUiState> = _state.asStateFlow()

    private var queue: List<MediaTrack> = emptyList()
    private var ticker: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = publish()
            override fun onPlaybackStateChanged(playbackState: Int) {
                publish()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()
            override fun onVolumeChanged(volume: Float) = publish()
            override fun onPlayerError(error: PlaybackException) {
                _state.value = _state.value.copy(
                    error = error.message ?: error.errorCodeName,
                    isPlaying = false
                )
            }
        })
        player.volume = 0.75f
        startTicker()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                if (player.isPlaying) publish()
                delay(500)
            }
        }
    }

    private fun publish() {
        val index = player.currentMediaItemIndex.coerceAtLeast(0)
        val track = queue.getOrNull(index)
        _state.value = _state.value.copy(
            queue = queue,
            index = index,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            title = track?.title ?: _state.value.title,
            artist = track?.artist ?: _state.value.artist,
            album = track?.album ?: _state.value.album,
            isRadio = track?.isRadio ?: false,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it > 0 } ?: track?.durationMs ?: 0L,
            volume = player.volume,
            shuffle = player.shuffleModeEnabled,
            repeat = player.repeatMode != Player.REPEAT_MODE_OFF,
            error = null
        )
    }

    fun play(track: MediaTrack) {
        playQueue(listOf(track), 0)
    }

    fun playQueue(tracks: List<MediaTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        queue = tracks
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(Uri.parse(track.uri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }
        player.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        player.prepare()
        player.play()
        publish()
    }

    fun playRadio(station: MediaTrack = RadioCatalog.stations.first()) {
        playQueue(RadioCatalog.stations, RadioCatalog.stations.indexOf(station).coerceAtLeast(0))
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
        publish()
    }

    fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()
        }
        publish()
    }

    fun previous() {
        if (player.currentPosition > 4_000) {
            player.seekTo(0)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.play()
        }
        publish()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0))
        publish()
    }

    fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
        publish()
    }

    fun toggleMute() {
        player.volume = if (player.volume > 0f) 0f else 0.75f
        publish()
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        publish()
    }

    fun toggleRepeat() {
        player.repeatMode = if (player.repeatMode == Player.REPEAT_MODE_OFF) {
            Player.REPEAT_MODE_ALL
        } else {
            Player.REPEAT_MODE_OFF
        }
        publish()
    }

    suspend fun loadLocalMusic(): List<MediaTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<MediaTrack>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        runCatching {
            appContext.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                var count = 0
                while (cursor.moveToNext() && count < 500) {
                    val id = cursor.getLong(idColumn)
                    tracks += MediaTrack(
                        id = "local-$id",
                        title = cursor.getString(titleColumn) ?: "Faixa $id",
                        artist = cursor.getString(artistColumn) ?: "Artista desconhecido",
                        album = cursor.getString(albumColumn) ?: "Álbum desconhecido",
                        uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString(),
                        durationMs = cursor.getLong(durationColumn)
                    )
                    count++
                }
            }
        }
        tracks
    }

    fun shutdown() {
        ticker?.cancel()
        player.release()
    }
}
