package com.tvmusic.app.data.repository

import android.content.Context
import com.tvmusic.app.data.db.MusicDatabase
import com.tvmusic.app.data.model.Artist
import com.tvmusic.app.data.model.Favorite
import com.tvmusic.app.data.model.QueueItem
import com.tvmusic.app.data.model.Song
import com.tvmusic.app.data.model.SortOrder
import com.tvmusic.app.media.scanner.MusicScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(context: Context) {

    private val db = MusicDatabase.getInstance(context)
    private val songDao = db.songDao()
    private val favoriteDao = db.favoriteDao()
    private val queueDao = db.queueDao()
    private val scanner = MusicScanner(context)

    companion object {
        @Volatile private var instance: MusicRepository? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: MusicRepository(context.applicationContext).also { instance = it }
        }
    }

    // ── Scan ──────────────────────────────────────────────────────────────────

    suspend fun scanLibrary(): Int = withContext(Dispatchers.IO) {
        val songs = scanner.scanAll()
        if (songs.isNotEmpty()) {
            songDao.insertSongs(songs)
            songDao.removeDeletedSongs(songs.map { it.id })
        }
        songs.size
    }

    // ── Songs ─────────────────────────────────────────────────────────────────

    fun getAllSongsFlow(): Flow<List<Song>> = songDao.getAllSongs()

    suspend fun getSortedSongs(order: SortOrder, query: String = ""): List<Song> =
        withContext(Dispatchers.IO) {
            val base = if (query.isBlank()) {
                songDao.getAllSongs().let { flow ->
                    // Collect synchronously for one-shot sorted fetch
                    var result = listOf<Song>()
                    kotlinx.coroutines.runBlocking {
                        result = db.songDao().searchSongs("%") // all
                    }
                    result
                }
            } else {
                songDao.searchSongs(query)
            }
            sortSongs(base, order)
        }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        songDao.searchSongs(query)
    }

    private fun sortSongs(songs: List<Song>, order: SortOrder): List<Song> = when (order) {
        SortOrder.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
        SortOrder.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
        SortOrder.ARTIST_ASC -> songs.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
        SortOrder.ARTIST_DESC -> songs.sortedWith(compareByDescending<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() })
        SortOrder.ALBUM_ASC -> songs.sortedWith(compareBy({ it.album.lowercase() }, { it.trackNumber }))
        SortOrder.DATE_ADDED_DESC -> songs.sortedByDescending { it.dateAdded }
        SortOrder.DATE_ADDED_ASC -> songs.sortedBy { it.dateAdded }
        SortOrder.DURATION_DESC -> songs.sortedByDescending { it.duration }
        SortOrder.DURATION_ASC -> songs.sortedBy { it.duration }
        SortOrder.FILE_SIZE_DESC -> songs.sortedByDescending { it.size }
    }

    // ── Artists ───────────────────────────────────────────────────────────────

    fun getArtistsFlow(): Flow<List<Artist>> = songDao.getArtistStats().map { rows ->
        rows.map { Artist(it.artist, it.songCount, it.albumCount) }
    }

    suspend fun getSongsByArtist(artist: String): List<Song> = withContext(Dispatchers.IO) {
        songDao.getSongsByArtist(artist)
    }

    // ── Favorites ─────────────────────────────────────────────────────────────

    fun getFavoriteSongsFlow(): Flow<List<Song>> = songDao.getFavoriteSongs()

    suspend fun toggleFavorite(song: Song): Boolean = withContext(Dispatchers.IO) {
        val isFav = favoriteDao.isFavorite(song.id)
        if (isFav) {
            favoriteDao.removeFavorite(song.id)
            songDao.setFavorite(song.id, false)
        } else {
            favoriteDao.addFavorite(Favorite(song.id))
            songDao.setFavorite(song.id, true)
        }
        !isFav
    }

    suspend fun isFavorite(songId: Long): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(songId)
    }

    // ── Queue ─────────────────────────────────────────────────────────────────

    fun getQueueFlow(): Flow<List<Song>> = queueDao.getQueueSongs()

    suspend fun saveQueue(songs: List<Song>) = withContext(Dispatchers.IO) {
        queueDao.clearQueue()
        val items = songs.mapIndexed { i, s -> QueueItem(songId = s.id, position = i) }
        queueDao.insertQueueItems(items)
    }

    suspend fun clearQueue() = withContext(Dispatchers.IO) {
        queueDao.clearQueue()
    }
}
