package com.tvmusic.app.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvmusic.app.data.model.Song
import com.tvmusic.app.data.model.SortOrder
import com.tvmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SongListViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MusicRepository.getInstance(app)

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    init {
        viewModelScope.launch {
            combine(repository.getAllSongsFlow(), _sortOrder, _searchQuery) { all, order, query ->
                val filtered = if (query.isBlank()) all
                else all.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
                }
                sortSongs(filtered, order)
            }.collectLatest { _songs.value = it }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun toggleFavorite(song: Song) = repository.toggleFavorite(song)

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
}
