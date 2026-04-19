package com.tvmusic.app.media.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.tvmusic.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicScanner(private val context: Context) {

    companion object {
        private const val TAG = "MusicScanner"
    }

    suspend fun scanAll(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        try {
            songs.addAll(scanMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI))
            songs.addAll(scanMediaStore(MediaStore.Audio.Media.INTERNAL_CONTENT_URI))
        } catch (e: Exception) {
            Log.e(TAG, "Scan failed", e)
        }
        Log.d(TAG, "Total songs found: ${songs.size}")
        songs.distinctBy { it.id }
    }

    private fun scanMediaStore(contentUri: Uri): List<Song> {
        val songs = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.BITRATE,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.IS_MUSIC
        )

        // Removed DURATION > 10000 filter — some devices report 0 duration in MediaStore
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                contentUri, projection, selection, null, sortOrder
            )
            Log.d(TAG, "Query $contentUri -> ${cursor?.count ?: -1} rows")
            cursor?.use {
                val idCol        = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol     = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol    = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol     = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol   = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol  = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val pathCol      = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol      = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModCol   = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val mimeCol      = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val bitrateCol   = it.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)
                val trackCol     = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol      = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

                while (it.moveToNext()) {
                    try {
                        val id         = it.getLong(idCol)
                        val path       = it.getString(pathCol) ?: ""
                        val mime       = it.getString(mimeCol) ?: "audio/mpeg"
                        val rawBitrate = it.getInt(bitrateCol)

                        val song = Song(
                            id           = id,
                            title        = it.getString(titleCol)?.takeIf { t -> t.isNotBlank() }
                                           ?: path.substringAfterLast('/').substringBeforeLast('.'),
                            artist       = it.getString(artistCol)?.takeIf { a -> a != "<unknown>" && a.isNotBlank() }
                                           ?: "未知艺术家",
                            album        = it.getString(albumCol)?.takeIf { al -> al.isNotBlank() }
                                           ?: "未知专辑",
                            albumId      = it.getLong(albumIdCol),
                            duration     = it.getLong(durationCol),
                            path         = path,
                            size         = it.getLong(sizeCol),
                            dateAdded    = it.getLong(dateAddedCol),
                            dateModified = it.getLong(dateModCol),
                            mimeType     = mime,
                            // BITRATE column unit varies by device: bps or kbps
                            bitrate      = if (rawBitrate > 10000) rawBitrate / 1000 else rawBitrate,
                            trackNumber  = it.getInt(trackCol),
                            year         = it.getInt(yearCol),
                            // Use MediaStore album art URI only — no direct file path access
                            hasCover     = checkHasCoverViaMediaStore(id),
                            hasLyrics    = false
                        )
                        songs.add(song)
                    } catch (e: Exception) {
                        Log.w(TAG, "Skip row: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Query failed for $contentUri", e)
        }

        return songs
    }

    /**
     * Check album art via MediaStore content URI only.
     * Direct file path access fails under Scoped Storage on Android 10+.
     */
    private fun checkHasCoverViaMediaStore(albumId: Long): Boolean {
        return try {
            val uri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"), albumId
            )
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun getAlbumArtUri(albumId: Long): Uri =
        ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"), albumId
        )
}
