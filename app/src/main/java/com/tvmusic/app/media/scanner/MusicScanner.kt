package com.tvmusic.app.media.scanner

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.tvmusic.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicScanner(private val context: Context) {

    companion object {
        private const val TAG = "MusicScanner"
        private val SUPPORTED_MIME = setOf(
            "audio/mpeg", "audio/flac", "audio/ogg", "audio/x-wav",
            "audio/aac", "audio/mp4", "audio/x-ms-wma", "audio/x-ape",
            "audio/opus", "audio/3gpp", "audio/amr"
        )
    }

    suspend fun scanAll(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        try {
            songs.addAll(scanMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI))
            songs.addAll(scanMediaStore(MediaStore.Audio.Media.INTERNAL_CONTENT_URI))
        } catch (e: Exception) {
            Log.e(TAG, "Scan failed", e)
        }
        // Deduplicate by id
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

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                contentUri, projection, selection, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val bitrateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idCol)
                        val mime = cursor.getString(mimeCol) ?: continue
                        val path = cursor.getString(pathCol) ?: continue

                        val song = Song(
                            id = id,
                            title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                                ?: path.substringAfterLast('/').substringBeforeLast('.'),
                            artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" }
                                ?: "未知艺术家",
                            album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() }
                                ?: "未知专辑",
                            albumId = cursor.getLong(albumIdCol),
                            duration = cursor.getLong(durationCol),
                            path = path,
                            size = cursor.getLong(sizeCol),
                            dateAdded = cursor.getLong(dateAddedCol),
                            dateModified = cursor.getLong(dateModCol),
                            mimeType = mime,
                            bitrate = cursor.getInt(bitrateCol) / 1000,
                            trackNumber = cursor.getInt(trackCol),
                            year = cursor.getInt(yearCol),
                            hasCover = checkHasCover(id, path),
                            hasLyrics = checkHasLyrics(path)
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

    private fun checkHasCover(songId: Long, path: String): Boolean {
        // Check 1: embedded art via MediaMetadataRetriever
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val art = retriever.embeddedPicture
            retriever.release()
            if (art != null) return true
        } catch (_: Exception) {}

        // Check 2: album art via MediaStore album art
        try {
            val albumArtUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                songId
            )
            context.contentResolver.openInputStream(albumArtUri)?.close()
            return true
        } catch (_: Exception) {}

        // Check 3: folder.jpg / cover.jpg in same directory
        val dir = path.substringBeforeLast('/')
        return listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "cover.png")
            .any { java.io.File("$dir/$it").exists() }
    }

    private fun checkHasLyrics(path: String): Boolean {
        // Check embedded USLT tag
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            // No direct USLT API, so check .lrc sidecar file
            retriever.release()
        } catch (_: Exception) {}

        val lrcPath = path.substringBeforeLast('.') + ".lrc"
        return java.io.File(lrcPath).exists()
    }

    fun getAlbumArtUri(albumId: Long): Uri =
        ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"), albumId
        )
}
