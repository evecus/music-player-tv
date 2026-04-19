package com.tvmusic.app

import android.app.Application
import com.tvmusic.app.data.repository.MusicRepository
import com.tvmusic.app.media.player.PlayerController

class TVMusicApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Pre-initialize singletons
        MusicRepository.getInstance(this)
        PlayerController.getInstance(this).connect()
    }
}
