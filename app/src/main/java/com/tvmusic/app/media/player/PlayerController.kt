package com.tvmusic.app.media.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.tvmusic.app.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0
)

class PlayerController private constructor(private val context: Context) {

    companion object {
        @Volatile private var instance: PlayerController? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: PlayerController(context.applicationContext).also { instance = it }
        }
    }

    private var controller: MediaController? = null
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    private var currentQueue: List<Song> = emptyList()

    fun connect() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            setupListener()
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        controller?.release()
        controller = null
    }

    private fun setupListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { updateState() }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { updateState() }
            override fun onPlaybackStateChanged(playbackState: Int) { updateState() }
            override fun onRepeatModeChanged(repeatMode: Int) { updateState() }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { updateState() }
        })
    }

    private fun updateState() {
        val ctrl = controller ?: return
        val idx = ctrl.currentMediaItemIndex
        val song = if (idx in currentQueue.indices) currentQueue[idx] else null
        _state.value = PlaybackState(
            currentSong = song,
            isPlaying = ctrl.isPlaying,
            positionMs = ctrl.currentPosition,
            durationMs = ctrl.duration.coerceAtLeast(0),
            shuffleEnabled = ctrl.shuffleModeEnabled,
            repeatMode = ctrl.repeatMode,
            queue = currentQueue,
            currentIndex = idx
        )
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val ctrl = controller ?: return
        currentQueue = queue
        val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        val items = queue.map { s ->
            MediaItem.Builder()
                .setUri(s.uri)
                .setMediaId(s.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .build()
                )
                .build()
        }
        ctrl.setMediaItems(items, startIndex, 0L)
        ctrl.prepare()
        ctrl.play()
        updateState()
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        playSong(songs[startIndex], songs)
    }

    fun addToQueue(song: Song) {
        val ctrl = controller ?: return
        currentQueue = currentQueue + song
        val item = MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(song.id.toString())
            .build()
        ctrl.addMediaItem(item)
    }

    fun playPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    fun toggleShuffle() {
        val ctrl = controller ?: return
        ctrl.shuffleModeEnabled = !ctrl.shuffleModeEnabled
    }

    fun toggleRepeat() {
        val ctrl = controller ?: return
        ctrl.repeatMode = when (ctrl.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun getCurrentPosition(): Long = controller?.currentPosition ?: 0L

    fun skipToIndex(index: Int) {
        controller?.seekTo(index, 0L)
    }
}
