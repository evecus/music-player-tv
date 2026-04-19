package com.tvmusic.app.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvmusic.app.media.metadata.LyricLine
import com.tvmusic.app.media.metadata.MetadataExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val extractor = MetadataExtractor(app)

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics

    private var lastLoadedPath: String? = null

    fun loadLyrics(path: String) {
        if (path == lastLoadedPath) return
        lastLoadedPath = path
        viewModelScope.launch {
            _lyrics.value = extractor.parseLyrics(path)
        }
    }
}
