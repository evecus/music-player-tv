package com.tvmusic.app.data.db

import android.content.Context
import androidx.room.*
import com.tvmusic.app.data.model.Favorite
import com.tvmusic.app.data.model.QueueItem
import com.tvmusic.app.data.model.Song
import kotlinx.coroutines.flow.Flow

// ───────────── DAOs ─────────────

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): Song?

    @Query("""
        SELECT * FROM songs WHERE
        lower(title) LIKE '%' || lower(:query) || '%' OR
        lower(artist) LIKE '%' || lower(:query) || '%' OR
        lower(album) LIKE '%' || lower(:query) || '%'
        ORDER BY title ASC
    """)
    suspend fun searchSongs(query: String): List<Song>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album ASC, trackNumber ASC")
    suspend fun getSongsByArtist(artist: String): List<Song>

    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>

    @Query("""
        SELECT artist, COUNT(*) as songCount, COUNT(DISTINCT album) as albumCount
        FROM songs GROUP BY artist ORDER BY artist ASC
    """)
    fun getArtistStats(): Flow<List<ArtistRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Query("DELETE FROM songs WHERE id NOT IN (:activeIds)")
    suspend fun removeDeletedSongs(activeIds: List<Long>)

    @Query("UPDATE songs SET isFavorite = :isFav WHERE id = :songId")
    suspend fun setFavorite(songId: Long, isFav: Boolean)

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
}

data class ArtistRow(
    val artist: String,
    val songCount: Int,
    val albumCount: Int
)

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun isFavorite(songId: Long): Boolean
}

@Dao
interface QueueDao {
    @Query("SELECT songs.* FROM songs INNER JOIN queue_items ON songs.id = queue_items.songId ORDER BY queue_items.position ASC")
    fun getQueueSongs(): Flow<List<Song>>

    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    suspend fun getQueueItems(): List<QueueItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<QueueItem>)

    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()

    @Query("SELECT COUNT(*) FROM queue_items")
    suspend fun getQueueSize(): Int
}

// ───────────── Database ─────────────

@Database(
    entities = [Song::class, Favorite::class, QueueItem::class],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun queueDao(): QueueDao

    companion object {
        @Volatile private var INSTANCE: MusicDatabase? = null

        fun getInstance(context: Context): MusicDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "tvmusic.db"
                ).build().also { INSTANCE = it }
            }
    }
}
