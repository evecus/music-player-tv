package com.tvmusic.app.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvmusic.app.data.model.Song
import com.tvmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository.getInstance(app)

    private val _results = MutableStateFlow<List<Song>>(emptyList())
    val results: StateFlow<List<Song>> = _results

    fun search(query: String) {
        viewModelScope.launch {
            _results.value = if (query.isBlank()) emptyList()
            else repo.searchSongs(query)
        }
    }
}
