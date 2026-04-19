package com.tvmusic.app.media.metadata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class LyricLine(
    val timeMs: Long,
    val text: String
)

class MetadataExtractor(private val context: Context) {

    companion object {
        private const val TAG = "MetadataExtractor"
    }

    /** Extract embedded album art bitmap */
    suspend fun getEmbeddedArt(path: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val bytes = retriever.embeddedPicture
            retriever.release()
            bytes?.let {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeByteArray(it, 0, it.size, options)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No embedded art for $path")
            null
        }
    }

    /** Get album art from MediaStore album art content URI */
    suspend fun getAlbumArtFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (_: Exception) { null }
    }

    /** Get folder cover art (cover.jpg / folder.jpg) */
    suspend fun getFolderArt(songPath: String): Bitmap? = withContext(Dispatchers.IO) {
        val dir = songPath.substringBeforeLast('/')
        val candidates = listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "cover.png")
        for (name in candidates) {
            val file = File("$dir/$name")
            if (file.exists()) {
                try {
                    return@withContext BitmapFactory.decodeFile(file.absolutePath)
                } catch (_: Exception) {}
            }
        }
        null
    }

    /** Parse LRC lyrics file into timed lines */
    suspend fun parseLyrics(songPath: String): List<LyricLine> = withContext(Dispatchers.IO) {
        val lrcPath = songPath.substringBeforeLast('.') + ".lrc"
        val lrcFile = File(lrcPath)
        if (!lrcFile.exists()) return@withContext emptyList()

        val lines = mutableListOf<LyricLine>()
        val timeRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\]""")

        try {
            lrcFile.readLines(Charsets.UTF_8).forEach { raw ->
                val matches = timeRegex.findAll(raw)
                val text = raw.replace(timeRegex, "").trim()
                if (text.isEmpty()) return@forEach

                matches.forEach { match ->
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val ms = match.groupValues[3].padEnd(3, '0').take(3).toLong()
                    val timeMs = min * 60000 + sec * 1000 + ms
                    lines.add(LyricLine(timeMs, text))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "LRC parse error", e)
        }

        lines.sortBy { it.timeMs }
        lines
    }

    /** Get sample rate and extra metadata */
    suspend fun getExtraMetadata(path: String): Map<String, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, String>()
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                ?.let { map["sampleRate"] = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.let { map["bitrate"] = (it.toLongOrNull()?.div(1000))?.toString() ?: it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)
                ?.let { map["numTracks"] = it }
            retriever.release()
        } catch (_: Exception) {}
        map
    }
}
