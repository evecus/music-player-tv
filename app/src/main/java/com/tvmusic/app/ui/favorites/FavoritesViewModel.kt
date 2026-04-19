package com.tvmusic.app.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvmusic.app.data.model.Song
import com.tvmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository.getInstance(app)

    val favorites: StateFlow<List<Song>> = repo.getFavoriteSongsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
