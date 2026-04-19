package com.tvmusic.app.ui.artists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvmusic.app.data.model.Artist
import com.tvmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ArtistsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MusicRepository.getInstance(app)

    val artists: StateFlow<List<Artist>> = repo.getArtistsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
