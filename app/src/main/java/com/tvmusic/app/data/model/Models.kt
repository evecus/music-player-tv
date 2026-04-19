package com.tvmusic.app.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,           // ms
    val path: String,
    val size: Long,               // bytes
    val dateAdded: Long,
    val dateModified: Long,
    val mimeType: String,
    val bitrate: Int = 0,         // kbps
    val sampleRate: Int = 0,      // Hz
    val trackNumber: Int = 0,
    val year: Int = 0,
    val hasCover: Boolean = false,
    val hasLyrics: Boolean = false,
    val isFavorite: Boolean = false
) {
    val uri: Uri get() = android.content.ContentUris.withAppendedId(
        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
    )

    val qualityLabel: String get() = when {
        mimeType.contains("flac", ignoreCase = true) -> when {
            sampleRate >= 88200 -> "Hi-Res"
            else -> "FLAC"
        }
        mimeType.contains("wav", ignoreCase = true) -> "WAV"
        mimeType.contains("ape", ignoreCase = true) -> "APE"
        mimeType.contains("ogg", ignoreCase = true) -> "OGG"
        mimeType.contains("opus", ignoreCase = true) -> "OPUS"
        bitrate >= 320 -> "320K"
        bitrate >= 256 -> "256K"
        bitrate >= 192 -> "192K"
        bitrate >= 128 -> "128K"
        bitrate > 0 -> "${bitrate}K"
        else -> "MP3"
    }

    val qualityColor: Int get() = when {
        mimeType.contains("flac", ignoreCase = true) ||
        mimeType.contains("wav", ignoreCase = true) ||
        mimeType.contains("ape", ignoreCase = true) -> 0xFF4CAF50.toInt() // green = lossless
        bitrate >= 320 -> 0xFF2196F3.toInt()  // blue = high
        bitrate >= 192 -> 0xFFFF9800.toInt()  // orange = medium
        else -> 0xFF9E9E9E.toInt()            // gray = low
    }

    val durationFormatted: String get() {
        val minutes = duration / 1000 / 60
        val seconds = duration / 1000 % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "queue_items")
data class QueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val songId: Long,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int
)

enum class SortOrder {
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
    ARTIST_DESC,
    ALBUM_ASC,
    DATE_ADDED_DESC,
    DATE_ADDED_ASC,
    DURATION_DESC,
    DURATION_ASC,
    FILE_SIZE_DESC
}
